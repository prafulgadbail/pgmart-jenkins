package com.pgmart.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RazorpayTest {

    private final Razorpay razorpay = new Razorpay("rzp_test_key", "test_secret", "hook_secret", "http://localhost:1");

    @Test
    void checkoutSignatureIsHmacOfOrderAndPayment() {
        // HMAC-SHA256("order_ABC|pay_XYZ", "test_secret"), worked out separately
        String expected = "15656b40fea6f2159b578efa459e969de9f5e223fb8a08393e274ac578d9d005";
        assertThat(Razorpay.hmac("test_secret", "order_ABC|pay_XYZ")).isEqualTo(expected);
        assertThat(razorpay.paymentSignatureValid("order_ABC", "pay_XYZ", expected)).isTrue();
        assertThat(razorpay.paymentSignatureValid("order_ABC", "pay_OTHER", expected)).isFalse();
    }

    @Test
    void webhookNeedsTheWebhookSecret() {
        String body = "{\"event\":\"payment.captured\"}";
        assertThat(razorpay.webhookSignatureValid(body, Razorpay.hmac("hook_secret", body))).isTrue();
        assertThat(razorpay.webhookSignatureValid(body, Razorpay.hmac("test_secret", body))).isFalse();
    }

    @Test
    void withoutKeysOnlyCashOnDelivery() {
        assertThat(new Razorpay("", "", "", "http://localhost:1").enabled()).isFalse();
        assertThat(razorpay.enabled()).isTrue();
    }
}
