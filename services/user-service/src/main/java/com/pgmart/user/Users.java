package com.pgmart.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** All SQL for users, one-time codes and addresses. */
@Repository
public class Users {

    public record User(long id, String email, String name, String phone, String passwordHash, String role,
                       boolean emailVerified) {
    }

    public record OtpRow(String codeHash, OffsetDateTime expiresAt, int attempts, OffsetDateTime sentAt) {
    }

    public record Address(long id, String fullName, String mobile, String line1, String city, String state,
                          String pincode, boolean isDefault) {
    }

    private static final String USER_COLUMNS =
            "id, email, name, phone, password_hash, role, email_verified";

    private final JdbcClient db;

    public Users(JdbcClient db) {
        this.db = db;
    }

    // ---------- users ----------

    public Optional<User> byEmail(String email) {
        return db.sql("SELECT " + USER_COLUMNS + " FROM users WHERE email = ?")
                .param(email).query(User.class).optional();
    }

    public Optional<User> byId(long id) {
        return db.sql("SELECT " + USER_COLUMNS + " FROM users WHERE id = ?")
                .param(id).query(User.class).optional();
    }

    public User create(String email, String name, String passwordHash, String role, boolean verified) {
        return db.sql("INSERT INTO users (email, name, password_hash, role, email_verified) VALUES (?, ?, ?, ?, ?) "
                        + "RETURNING " + USER_COLUMNS)
                .params(email, name, passwordHash, role, verified).query(User.class).single();
    }

    /** A sign-up that was never verified can be started again with a new name and password. */
    public void restartSignup(long id, String name, String passwordHash) {
        db.sql("UPDATE users SET name = ?, password_hash = ?, updated_at = now() WHERE id = ?")
                .params(name, passwordHash, id).update();
    }

    public void markVerified(long id) {
        db.sql("UPDATE users SET email_verified = TRUE, updated_at = now() WHERE id = ?").param(id).update();
    }

    public void updatePassword(long id, String passwordHash) {
        db.sql("UPDATE users SET password_hash = ?, updated_at = now() WHERE id = ?")
                .params(passwordHash, id).update();
    }

    public void updateProfile(long id, String name, String phone) {
        db.sql("UPDATE users SET name = ?, phone = ?, updated_at = now() WHERE id = ?")
                .params(name, phone, id).update();
    }

    public void makeAdmin(long id, String passwordHash) {
        db.sql("UPDATE users SET role = 'ADMIN', email_verified = TRUE, password_hash = ?, updated_at = now() WHERE id = ?")
                .params(passwordHash, id).update();
    }

    // ---------- one-time codes ----------

    public Optional<OtpRow> otp(String email, String purpose) {
        return db.sql("SELECT code_hash, expires_at, attempts, sent_at FROM otp_codes WHERE email = ? AND purpose = ?")
                .params(email, purpose).query(OtpRow.class).optional();
    }

    public void saveOtp(String email, String purpose, String codeHash, OffsetDateTime expiresAt) {
        db.sql("""
                INSERT INTO otp_codes (email, purpose, code_hash, expires_at, attempts, sent_at)
                VALUES (?, ?, ?, ?, 0, now())
                ON CONFLICT (email, purpose)
                DO UPDATE SET code_hash = EXCLUDED.code_hash, expires_at = EXCLUDED.expires_at,
                              attempts = 0, sent_at = now()
                """)
                .params(email, purpose, codeHash, expiresAt).update();
    }

    public void countAttempt(String email, String purpose) {
        db.sql("UPDATE otp_codes SET attempts = attempts + 1 WHERE email = ? AND purpose = ?")
                .params(email, purpose).update();
    }

    public void deleteOtp(String email, String purpose) {
        db.sql("DELETE FROM otp_codes WHERE email = ? AND purpose = ?").params(email, purpose).update();
    }

    // ---------- addresses ----------

    public List<Address> addresses(long userId) {
        return db.sql("""
                SELECT id, full_name, mobile, line1, city, state, pincode, is_default
                FROM addresses WHERE user_id = ? ORDER BY is_default DESC, id DESC
                """).param(userId).query(Address.class).list();
    }

    public Address addAddress(long userId, ProfileController.AddressRequest a) {
        boolean first = db.sql("SELECT count(*) FROM addresses WHERE user_id = ?")
                .param(userId).query(Long.class).single() == 0;
        if (a.isDefault() || first) {
            db.sql("UPDATE addresses SET is_default = FALSE WHERE user_id = ?").param(userId).update();
        }
        return db.sql("""
                INSERT INTO addresses (user_id, full_name, mobile, line1, city, state, pincode, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, full_name, mobile, line1, city, state, pincode, is_default
                """)
                .params(userId, a.fullName().trim(), a.mobile(), a.line1().trim(), a.city().trim(),
                        a.state().trim(), a.pincode(), a.isDefault() || first)
                .query(Address.class).single();
    }

    public boolean deleteAddress(long userId, long id) {
        return db.sql("DELETE FROM addresses WHERE id = ? AND user_id = ?").params(id, userId).update() == 1;
    }

    public boolean setDefaultAddress(long userId, long id) {
        int found = db.sql("SELECT count(*) FROM addresses WHERE id = ? AND user_id = ?")
                .params(id, userId).query(Integer.class).single();
        if (found == 0) {
            return false;
        }
        db.sql("UPDATE addresses SET is_default = (id = ?) WHERE user_id = ?").params(id, userId).update();
        return true;
    }
}
