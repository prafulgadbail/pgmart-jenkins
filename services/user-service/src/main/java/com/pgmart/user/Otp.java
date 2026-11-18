package com.pgmart.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** One-time codes for email verification and password reset. Only a hash is stored. */
public final class Otp {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Otp() {
    }

    public static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String hash(String email, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((email.toLowerCase() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String email, String code, String storedHash) {
        return MessageDigest.isEqual(hash(email, code).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
