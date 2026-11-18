package com.pgmart.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void adminMovesOneStepAtATime() {
        assertThat(OrderStatus.PLACED.adminCanMoveTo(OrderStatus.PACKED)).isTrue();
        assertThat(OrderStatus.PACKED.adminCanMoveTo(OrderStatus.OUT_FOR_DELIVERY)).isTrue();
        assertThat(OrderStatus.OUT_FOR_DELIVERY.adminCanMoveTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.PLACED.adminCanMoveTo(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.DELIVERED.adminCanMoveTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.PAYMENT_PENDING.adminCanMoveTo(OrderStatus.PACKED)).isFalse();
    }

    @Test
    void customerCanCancelUntilTheRiderHasIt() {
        assertThat(OrderStatus.PLACED.customerCanCancel()).isTrue();
        assertThat(OrderStatus.PACKED.customerCanCancel()).isTrue();
        assertThat(OrderStatus.OUT_FOR_DELIVERY.customerCanCancel()).isFalse();
        assertThat(OrderStatus.DELIVERED.customerCanCancel()).isFalse();
    }
}
