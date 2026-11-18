package com.pgmart.catalog;

/** The logged-in user, read from the JWT. */
public record AuthUser(long id, String email, String name, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
