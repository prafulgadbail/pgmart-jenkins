package com.pgmart.order;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Payment options for the checkout page, and the Razorpay webhook. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final Razorpay razorpay;
    private final OrderService service;
    private final ObjectMapper json;

    public PaymentController(Razorpay razorpay, OrderService service, ObjectMapper json) {
        this.razorpay = razorpay;
        this.service = service;
        this.json = json;
    }

    @GetMapping("/options")
    public Map<String, Object> options() {
        return Map.of("cod", true, "online", razorpay.enabled());
    }

    /**
     * Razorpay calls this when a payment is captured, even if the customer closed the browser
     * before the checkout callback. The raw body is needed to check the signature.
     */
    @PostMapping("/razorpay/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                        @RequestBody String body) throws Exception {
        if (!razorpay.webhookSignatureValid(body, signature)) {
            log.warn("Razorpay webhook with a bad signature");
            return ResponseEntity.status(400).build();
        }
        JsonNode event = json.readTree(body);
        String type = event.path("event").asText();
        JsonNode payment = event.path("payload").path("payment").path("entity");
        log.info("Razorpay webhook {}", type);
        if ("payment.captured".equals(type) || "order.paid".equals(type)) {
            service.markPaid(payment.path("order_id").asText(), payment.path("id").asText(), "webhook");
        }
        // payment.failed: nothing to do, the customer can retry from "Your orders" until the order expires
        return ResponseEntity.ok().build();
    }
}
