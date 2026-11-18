package com.pgmart.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes stock for an order and gives it back if the order is cancelled.
 * Both are safe to call twice with the same order id.
 */
@Service
public class Stock {

    private static final Logger log = LoggerFactory.getLogger(Stock.class);

    public record Line(long bookId, int qty) {
    }

    /** What order-service gets back: the price and title at the time of the order. */
    public record ReservedLine(long bookId, String title, int qty, long price, long mrp) {
    }

    private final JdbcClient db;

    public Stock(JdbcClient db) {
        this.db = db;
    }

    @Transactional
    public List<ReservedLine> reserve(String orderId, List<Line> lines) {
        Optional<String> existing = db.sql("SELECT status FROM stock_reservations WHERE order_id = ? FOR UPDATE")
                .param(orderId).query(String.class).optional();
        if (existing.isPresent()) {
            if ("RELEASED".equals(existing.get())) {
                throw ApiException.conflict("RELEASED", "This order was already cancelled");
            }
            return reservedLines(orderId);
        }

        // Same book twice is added up; books are locked in id order so two orders never wait on each other
        Map<Long, Integer> wanted = new TreeMap<>();
        for (Line l : lines) {
            if (l.qty() < 1 || l.qty() > 10) {
                throw ApiException.badRequest("Quantity must be between 1 and 10");
            }
            wanted.merge(l.bookId(), l.qty(), Integer::sum);
        }
        if (wanted.isEmpty() || wanted.size() > 30) {
            throw ApiException.badRequest("An order needs between 1 and 30 different books");
        }

        db.sql("INSERT INTO stock_reservations (order_id, status) VALUES (?, 'RESERVED')").param(orderId).update();
        for (var e : wanted.entrySet()) {
            Books.Book book = db.sql("SELECT id, title, subtitle, topic_key, logo_key, color1, color2, mrp_paise, price_paise, "
                            + "stock, pages, highlights, image_url, active FROM books WHERE id = ? FOR UPDATE")
                    .param(e.getKey()).query(Books.BOOK).optional()
                    .filter(Books.Book::active)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "BOOK_UNAVAILABLE",
                            "A book in your cart is no longer available"));
            if (book.stock() < e.getValue()) {
                throw new ApiException(HttpStatus.CONFLICT, "OUT_OF_STOCK", book.stock() == 0
                        ? book.title() + " just went out of stock"
                        : "Only " + book.stock() + " left of " + book.title());
            }
            db.sql("UPDATE books SET stock = stock - ?, updated_at = now() WHERE id = ?")
                    .params(e.getValue(), book.id()).update();
            db.sql("INSERT INTO stock_reservation_items (order_id, book_id, qty, title, price_paise, mrp_paise) "
                            + "VALUES (?, ?, ?, ?, ?, ?)")
                    .params(orderId, book.id(), e.getValue(), book.title(), book.price(), book.mrp()).update();
        }
        log.info("Stock reserved for order {} ({} books)", orderId, wanted.size());
        return reservedLines(orderId);
    }

    @Transactional
    public void release(String orderId) {
        Optional<String> status = db.sql("SELECT status FROM stock_reservations WHERE order_id = ? FOR UPDATE")
                .param(orderId).query(String.class).optional();
        if (status.isEmpty() || "RELEASED".equals(status.get())) {
            return;
        }
        db.sql("""
                UPDATE books b SET stock = b.stock + i.qty, updated_at = now()
                FROM stock_reservation_items i
                WHERE i.order_id = ? AND i.book_id = b.id
                """).param(orderId).update();
        db.sql("UPDATE stock_reservations SET status = 'RELEASED', updated_at = now() WHERE order_id = ?")
                .param(orderId).update();
        log.info("Stock released for order {}", orderId);
    }

    private List<ReservedLine> reservedLines(String orderId) {
        return db.sql("SELECT book_id, title, qty, price_paise, mrp_paise FROM stock_reservation_items WHERE order_id = ? ORDER BY book_id")
                .param(orderId)
                .query((rs, i) -> new ReservedLine(rs.getLong("book_id"), rs.getString("title"), rs.getInt("qty"),
                        rs.getLong("price_paise"), rs.getLong("mrp_paise")))
                .list();
    }
}
