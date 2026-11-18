package com.pgmart.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class NotifierTest {

    private static Orders.Order order(String method, String paymentStatus) {
        return new Orders.Order("id", "PGM-100001", 1, "Asha", "a@b.com", OrderStatus.PLACED, method, paymentStatus,
                799_00, 1099_00, 0, 799_00, new Orders.Address("Asha", "9876543210", "1 MG Road", "Pune", "MH", "411001"),
                null, null, false, null, List.of(), List.of());
    }

    @Test
    void codMessageAsksForCash() {
        assertThat(Notifier.messageFor(order("COD", "COD_DUE"), OrderStatus.PLACED).body()).contains("₹799").contains("cash");
    }

    @Test
    void cancelledPaidOrderMentionsRefund() {
        assertThat(Notifier.messageFor(order("RAZORPAY", "REFUND_PENDING"), OrderStatus.CANCELLED).body()).contains("refund");
        assertThat(Notifier.messageFor(order("COD", "NOT_PAID"), OrderStatus.CANCELLED).body()).doesNotContain("refund");
    }
}
