package com.pgmart.order;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for orders, their items, the tracking timeline and notifications. */
@Repository
public class Orders {

    public record Address(String name, String mobile, String line1, String city, String state, String pincode) {
    }

    public record Item(long bookId, String title, int qty, long price, long mrp) {
    }

    public record Event(String status, String note, OffsetDateTime at) {
    }

    public record Order(String id, String number, long userId, String customerName, String customerEmail,
                        OrderStatus status, String paymentMethod, String paymentStatus, long itemsTotal, long mrpTotal,
                        long delivery, long total, Address address, String razorpayOrderId, String razorpayPaymentId,
                        boolean stockReleased, OffsetDateTime createdAt, List<Item> items, List<Event> events) {

        Order with(List<Item> items, List<Event> events) {
            return new Order(id, number, userId, customerName, customerEmail, status, paymentMethod, paymentStatus,
                    itemsTotal, mrpTotal, delivery, total, address, razorpayOrderId, razorpayPaymentId, stockReleased,
                    createdAt, items, events);
        }
    }

    public record Notification(long id, String orderId, String title, String body, boolean read, OffsetDateTime createdAt) {
    }

    public record NewOrder(String id, long userId, String customerName, String customerEmail, OrderStatus status,
                           String paymentMethod, String paymentStatus, Pricing.Totals totals, Address address,
                           String idempotencyKey) {
    }

    private static final String COLUMNS = """
            id, number, user_id, customer_name, customer_email, status, payment_method, payment_status, items_paise,
            mrp_paise, delivery_paise, total_paise, ship_name, ship_mobile, ship_line1, ship_city, ship_state,
            ship_pincode, razorpay_order_id, razorpay_payment_id, stock_released, created_at""";

    private static final RowMapper<Order> ORDER = (rs, i) -> new Order(
            rs.getString("id"), rs.getString("number"), rs.getLong("user_id"), rs.getString("customer_name"),
            rs.getString("customer_email"), OrderStatus.valueOf(rs.getString("status")), rs.getString("payment_method"),
            rs.getString("payment_status"), rs.getLong("items_paise"), rs.getLong("mrp_paise"),
            rs.getLong("delivery_paise"), rs.getLong("total_paise"),
            new Address(rs.getString("ship_name"), rs.getString("ship_mobile"), rs.getString("ship_line1"),
                    rs.getString("ship_city"), rs.getString("ship_state"), rs.getString("ship_pincode")),
            rs.getString("razorpay_order_id"), rs.getString("razorpay_payment_id"), rs.getBoolean("stock_released"),
            rs.getObject("created_at", OffsetDateTime.class), List.of(), List.of());

    private final JdbcClient db;

    public Orders(JdbcClient db) {
        this.db = db;
    }

    // ---------- orders ----------

    public Order insert(NewOrder o, List<CatalogClient.ReservedLine> lines, String note) {
        String number = "PGM-" + db.sql("SELECT nextval('order_number_seq')").query(Long.class).single();
        Pricing.Totals t = o.totals();
        Address a = o.address();
        db.sql("""
                INSERT INTO orders (id, number, user_id, customer_name, customer_email, status, payment_method,
                    payment_status, items_paise, mrp_paise, delivery_paise, total_paise, ship_name, ship_mobile,
                    ship_line1, ship_city, ship_state, ship_pincode, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .params(o.id(), number, o.userId(), o.customerName(), o.customerEmail(), o.status().name(),
                        o.paymentMethod(), o.paymentStatus(), t.items(), t.mrp(), t.delivery(), t.total(), a.name(),
                        a.mobile(), a.line1(), a.city(), a.state(), a.pincode(), o.idempotencyKey())
                .update();
        for (var l : lines) {
            db.sql("INSERT INTO order_items (order_id, book_id, title, qty, price_paise, mrp_paise) VALUES (?, ?, ?, ?, ?, ?)")
                    .params(o.id(), l.bookId(), l.title(), l.qty(), l.price(), l.mrp()).update();
        }
        addEvent(o.id(), o.status(), note);
        return byId(o.id()).orElseThrow();
    }

    public Optional<Order> byId(String id) {
        return db.sql("SELECT " + COLUMNS + " FROM orders WHERE id = ?").param(id).query(ORDER).optional()
                .map(this::withDetails);
    }

    /** Locks the row until the transaction ends, so two status changes cannot race. */
    public Optional<Order> lock(String id) {
        return db.sql("SELECT " + COLUMNS + " FROM orders WHERE id = ? FOR UPDATE").param(id).query(ORDER).optional();
    }

    public Optional<Order> byIdempotencyKey(long userId, String key) {
        return db.sql("SELECT " + COLUMNS + " FROM orders WHERE user_id = ? AND idempotency_key = ?")
                .params(userId, key).query(ORDER).optional().map(this::withDetails);
    }

    public Optional<Order> byRazorpayOrderId(String razorpayOrderId) {
        return db.sql("SELECT " + COLUMNS + " FROM orders WHERE razorpay_order_id = ? FOR UPDATE")
                .param(razorpayOrderId).query(ORDER).optional();
    }

    public List<Order> forUser(long userId) {
        return details(db.sql("SELECT " + COLUMNS + " FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 50")
                .param(userId).query(ORDER).list());
    }

    public List<Order> forAdmin(String status) {
        return details(db.sql("SELECT " + COLUMNS + " FROM orders "
                        + "WHERE (CAST(:status AS TEXT) IS NULL OR status = :status) ORDER BY created_at DESC LIMIT 200")
                .param("status", status).query(ORDER).list());
    }

    public void setStatus(String id, OrderStatus status, String paymentStatus, String note) {
        db.sql("UPDATE orders SET status = ?, payment_status = ?, updated_at = now() WHERE id = ?")
                .params(status.name(), paymentStatus, id).update();
        addEvent(id, status, note);
    }

    public void setRazorpayOrder(String id, String razorpayOrderId) {
        db.sql("UPDATE orders SET razorpay_order_id = ?, updated_at = now() WHERE id = ?")
                .params(razorpayOrderId, id).update();
    }

    public void setPaid(String id, String paymentId, String note) {
        db.sql("UPDATE orders SET status = 'PLACED', payment_status = 'PAID', razorpay_payment_id = ?, updated_at = now() WHERE id = ?")
                .params(paymentId, id).update();
        addEvent(id, OrderStatus.PLACED, note);
    }

    public void setPaymentStatus(String id, String paymentStatus) {
        db.sql("UPDATE orders SET payment_status = ?, updated_at = now() WHERE id = ?").params(paymentStatus, id).update();
    }

    public void setPaymentId(String id, String paymentId) {
        db.sql("UPDATE orders SET razorpay_payment_id = ?, updated_at = now() WHERE id = ?").params(paymentId, id).update();
    }

    public void markStockReleased(String id) {
        db.sql("UPDATE orders SET stock_released = TRUE, updated_at = now() WHERE id = ?").param(id).update();
    }

    // ---------- background clean-up ----------

    public List<String> unpaidOlderThan(int minutes) {
        return db.sql("SELECT id FROM orders WHERE status = 'PAYMENT_PENDING' AND created_at < now() - make_interval(mins => ?)")
                .param(minutes).query(String.class).list();
    }

    public List<String> cancelledWithStockHeld() {
        return db.sql("SELECT id FROM orders WHERE status = 'CANCELLED' AND NOT stock_released").query(String.class).list();
    }

    public List<Order> refundsPending() {
        return db.sql("SELECT " + COLUMNS + " FROM orders WHERE payment_status = 'REFUND_PENDING'").query(ORDER).list();
    }

    // ---------- admin numbers ----------

    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), 0L);
        }
        db.sql("SELECT status, count(*) AS n FROM orders GROUP BY status")
                .query((rs, i) -> counts.put(rs.getString("status"), rs.getLong("n"))).list();
        return counts;
    }

    public long revenue() {
        return db.sql("SELECT coalesce(sum(total_paise), 0) FROM orders WHERE status NOT IN ('CANCELLED', 'PAYMENT_PENDING')")
                .query(Long.class).single();
    }

    // ---------- notifications ----------

    public void notify(long userId, String orderId, String title, String body) {
        db.sql("INSERT INTO notifications (user_id, order_id, title, body) VALUES (?, ?, ?, ?)")
                .params(userId, orderId, title, body).update();
    }

    public List<Notification> notifications(long userId) {
        return db.sql("SELECT id, order_id, title, body, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT 30")
                .param(userId)
                .query((rs, i) -> new Notification(rs.getLong("id"), rs.getString("order_id"), rs.getString("title"),
                        rs.getString("body"), rs.getBoolean("is_read"), rs.getObject("created_at", OffsetDateTime.class)))
                .list();
    }

    public void markAllRead(long userId) {
        db.sql("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND NOT is_read").param(userId).update();
    }

    public void clearNotifications(long userId) {
        db.sql("DELETE FROM notifications WHERE user_id = ?").param(userId).update();
    }

    // ---------- helpers ----------

    private void addEvent(String orderId, OrderStatus status, String note) {
        db.sql("INSERT INTO order_events (order_id, status, note) VALUES (?, ?, ?)")
                .params(orderId, status.name(), note).update();
    }

    private Order withDetails(Order o) {
        return details(List.of(o)).get(0);
    }

    /** Items and events for many orders with two queries, not two per order. */
    private List<Order> details(List<Order> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        List<String> ids = orders.stream().map(Order::id).toList();
        Map<String, List<Item>> items = new LinkedHashMap<>();
        db.sql("SELECT order_id, book_id, title, qty, price_paise, mrp_paise FROM order_items WHERE order_id IN (:ids) ORDER BY book_id")
                .param("ids", ids)
                .query((rs, i) -> items.computeIfAbsent(rs.getString("order_id"), k -> new ArrayList<>())
                        .add(new Item(rs.getLong("book_id"), rs.getString("title"), rs.getInt("qty"),
                                rs.getLong("price_paise"), rs.getLong("mrp_paise"))))
                .list();
        Map<String, List<Event>> events = new LinkedHashMap<>();
        db.sql("SELECT order_id, status, note, created_at FROM order_events WHERE order_id IN (:ids) ORDER BY id")
                .param("ids", ids)
                .query((rs, i) -> events.computeIfAbsent(rs.getString("order_id"), k -> new ArrayList<>())
                        .add(new Event(rs.getString("status"), rs.getString("note"),
                                rs.getObject("created_at", OffsetDateTime.class))))
                .list();
        return orders.stream()
                .map(o -> o.with(items.getOrDefault(o.id(), List.of()), events.getOrDefault(o.id(), List.of())))
                .toList();
    }
}
