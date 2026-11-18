package com.pgmart.user;

/** The logged-in user, read from the JWT. */
public record AuthUser(long id, String email, String name, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
