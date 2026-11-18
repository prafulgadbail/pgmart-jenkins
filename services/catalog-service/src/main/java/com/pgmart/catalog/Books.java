package com.pgmart.catalog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for topics, books and the wishlist. Prices are in paise (₹1 = 100 paise). */
@Repository
public class Books {

    public record Topic(String key, String name, String description, int books) {
    }

    public record Book(long id, String title, String subtitle, String topic, String logo, String color1, String color2,
                       long mrp, long price, int stock, int pages, List<String> highlights, String imageUrl,
                       boolean active) {
    }

    private static final String COLUMNS = """
            id, title, subtitle, topic_key, logo_key, color1, color2, mrp_paise, price_paise, stock, pages,
            highlights, image_url, active""";

    private static final String B_COLUMNS = """
            b.id, b.title, b.subtitle, b.topic_key, b.logo_key, b.color1, b.color2, b.mrp_paise, b.price_paise, b.stock,
            b.pages, b.highlights, b.image_url, b.active""";

    static final RowMapper<Book> BOOK = Books::map;

    private final JdbcClient db;

    public Books(JdbcClient db) {
        this.db = db;
    }

    private static Book map(ResultSet rs, int row) throws SQLException {
        String highlights = rs.getString("highlights");
        return new Book(rs.getLong("id"), rs.getString("title"), rs.getString("subtitle"), rs.getString("topic_key"),
                rs.getString("logo_key"), rs.getString("color1"), rs.getString("color2"), rs.getLong("mrp_paise"),
                rs.getLong("price_paise"), rs.getInt("stock"), rs.getInt("pages"),
                highlights == null || highlights.isBlank() ? List.of() : Arrays.asList(highlights.split("\n")),
                rs.getString("image_url"), rs.getBoolean("active"));
    }

    public List<Topic> topics() {
        return db.sql("""
                SELECT t.key, t.name, t.description, count(b.id) AS books
                FROM topics t LEFT JOIN books b ON b.topic_key = t.key AND b.active
                GROUP BY t.key, t.name, t.description, t.sort_order
                ORDER BY t.sort_order
                """).query((rs, i) -> new Topic(rs.getString("key"), rs.getString("name"),
                rs.getString("description"), rs.getInt("books"))).list();
    }

    public boolean topicExists(String key) {
        return db.sql("SELECT count(*) FROM topics WHERE key = ?").param(key).query(Integer.class).single() > 0;
    }

    /** Store list: active books, optional search text and topic. */
    public List<Book> search(String q, String topic) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return db.sql("SELECT " + B_COLUMNS + """

                FROM books b JOIN topics t ON t.key = b.topic_key
                WHERE b.active
                  AND (CAST(:topic AS TEXT) IS NULL OR b.topic_key = :topic)
                  AND (CAST(:like AS TEXT) IS NULL OR lower(b.title || ' ' || b.subtitle || ' ' || b.topic_key || ' ' || b.logo_key) LIKE :like)
                ORDER BY t.sort_order, b.id
                """)
                .param("topic", topic == null || topic.isBlank() ? null : topic)
                .param("like", like)
                .query(BOOK).list();
    }

    public List<Book> all() {
        return db.sql("SELECT " + COLUMNS + " FROM books ORDER BY id").query(BOOK).list();
    }

    public Optional<Book> byId(long id) {
        return db.sql("SELECT " + COLUMNS + " FROM books WHERE id = ?").param(id).query(BOOK).optional();
    }

    public Book create(AdminBookController.BookRequest r) {
        return db.sql("""
                INSERT INTO books (title, subtitle, topic_key, logo_key, color1, color2, mrp_paise, price_paise, stock,
                                   pages, highlights, image_url, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING
                """ + COLUMNS)
                .params(r.title().trim(), r.subtitle().trim(), r.topic(), r.logo(), r.color1(), r.color2(), r.mrp(),
                        r.price(), r.stock(), r.pages(), String.join("\n", r.highlightsOrEmpty()), blankToNull(r.imageUrl()),
                        r.activeOrTrue())
                .query(BOOK).single();
    }

    public Optional<Book> update(long id, AdminBookController.BookRequest r) {
        return db.sql("""
                UPDATE books SET title = ?, subtitle = ?, topic_key = ?, logo_key = ?, color1 = ?, color2 = ?,
                       mrp_paise = ?, price_paise = ?, stock = ?, pages = ?, highlights = ?, image_url = ?, active = ?,
                       updated_at = now()
                WHERE id = ?
                RETURNING
                """ + COLUMNS)
                .params(r.title().trim(), r.subtitle().trim(), r.topic(), r.logo(), r.color1(), r.color2(), r.mrp(),
                        r.price(), r.stock(), r.pages(), String.join("\n", r.highlightsOrEmpty()), blankToNull(r.imageUrl()),
                        r.activeOrTrue(), id)
                .query(BOOK).optional();
    }

    public Optional<Book> setStock(long id, int stock) {
        return db.sql("UPDATE books SET stock = ?, updated_at = now() WHERE id = ? RETURNING " + COLUMNS)
                .params(stock, id).query(BOOK).optional();
    }

    public Optional<Book> setActive(long id, boolean active) {
        return db.sql("UPDATE books SET active = ?, updated_at = now() WHERE id = ? RETURNING " + COLUMNS)
                .params(active, id).query(BOOK).optional();
    }

    public int lowStock(int limit) {
        return db.sql("SELECT count(*) FROM books WHERE active AND stock <= ?").param(limit).query(Integer.class).single();
    }

    // ---------- wishlist ----------

    public List<Long> wishlist(long userId) {
        return db.sql("""
                SELECT w.book_id FROM wishlist w JOIN books b ON b.id = w.book_id
                WHERE w.user_id = ? AND b.active ORDER BY w.created_at DESC
                """).param(userId).query(Long.class).list();
    }

    public void addToWishlist(long userId, long bookId) {
        db.sql("INSERT INTO wishlist (user_id, book_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params(userId, bookId).update();
    }

    public void removeFromWishlist(long userId, long bookId) {
        db.sql("DELETE FROM wishlist WHERE user_id = ? AND book_id = ?").params(userId, bookId).update();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
