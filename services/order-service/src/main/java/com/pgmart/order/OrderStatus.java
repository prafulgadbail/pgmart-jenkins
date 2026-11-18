package com.pgmart.order;

import java.util.Map;
import java.util.Set;

/** Order life cycle and who may move it where. */
public enum OrderStatus {
    PAYMENT_PENDING, PLACED, PACKED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_NEXT = Map.of(
            PLACED, Set.of(PACKED, CANCELLED),
            PACKED, Set.of(OUT_FOR_DELIVERY, CANCELLED),
            OUT_FOR_DELIVERY, Set.of(DELIVERED));

    /** The store admin moves an order one step forward, or cancels it before it leaves the store. */
    public boolean adminCanMoveTo(OrderStatus next) {
        return ADMIN_NEXT.getOrDefault(this, Set.of()).contains(next);
    }

    /** The customer can cancel until the parcel is handed to the rider. */
    public boolean customerCanCancel() {
        return this == PAYMENT_PENDING || this == PLACED || this == PACKED;
    }
}
