package com.pgmart.order;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Razorpay payment gateway, called over its REST API.
 * Test-mode keys (rzp_test_...) run the real checkout with test cards and UPI; no money moves.
 * Switching to live keys needs no code change.
 */
@Component
public class Razorpay {

    private static final Logger log = LoggerFactory.getLogger(Razorpay.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient http;

    public Razorpay(@Value("${app.razorpay.key-id:}") String keyId,
                    @Value("${app.razorpay.key-secret:}") String keySecret,
                    @Value("${app.razorpay.webhook-secret:}") String webhookSecret,
                    @Value("${app.razorpay.api-url:https://api.razorpay.com}") String apiUrl) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        String basic = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        this.http = RestClient.builder().baseUrl(apiUrl).requestFactory(factory)
                .defaultHeader("Authorization", "Basic " + basic).build();
    }

    /** Online payment is offered only when keys are configured; otherwise the store is Cash on Delivery only. */
    public boolean enabled() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    public String keyId() {
        return keyId;
    }

    /** Creates the Razorpay order the checkout popup pays. Returns its id (order_...). */
    public String createOrder(long amountPaise, String receipt, String orderId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = http.post().uri("/v1/orders")
                    .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt,
                            "notes", Map.of("order_id", orderId)))
                    .retrieve().body(Map.class);
            return (String) res.get("id");
        } catch (RestClientException | NullPointerException e) {
            log.error("Razorpay order could not be created", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY",
                    "Online payment is not available right now. Please choose Cash on Delivery.");
        }
    }

    /** Full refund of a captured payment. Returns false if Razorpay could not be reached (retried later). */
    public boolean refund(String paymentId, long amountPaise) {
        try {
            http.post().uri("/v1/payments/{id}/refund", paymentId)
                    .body(Map.of("amount", amountPaise)).retrieve().toBodilessEntity();
            log.info("Refund started for payment {}", paymentId);
            return true;
        } catch (RestClientException e) {
            log.warn("Refund failed for payment {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    /** Checkout success: signature = HMAC-SHA256(order_id + "|" + payment_id, key secret). */
    public boolean paymentSignatureValid(String razorpayOrderId, String paymentId, String signature) {
        return enabled() && signature != null && equal(hmac(keySecret, razorpayOrderId + "|" + paymentId), signature);
    }

    /** Webhook: X-Razorpay-Signature = HMAC-SHA256(raw body, webhook secret). */
    public boolean webhookSignatureValid(String rawBody, String signature) {
        return !webhookSecret.isBlank() && signature != null && equal(hmac(webhookSecret, rawBody), signature);
    }

    static String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean equal(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
