package com.pgmart.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every minute:
 * - cancels online orders not paid within 30 minutes, so their stock goes back on sale
 * - retries stock releases and refunds that failed earlier (e.g. catalog-service or Razorpay was down)
 */
@Component
public class Housekeeping {

    private static final Logger log = LoggerFactory.getLogger(Housekeeping.class);
    static final int UNPAID_MINUTES = 30;

    private final Orders orders;
    private final OrderService service;

    public Housekeeping(Orders orders, OrderService service) {
        this.orders = orders;
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        for (String id : orders.unpaidOlderThan(UNPAID_MINUTES)) {
            log.info("Order {} not paid in {} minutes: cancelling", id, UNPAID_MINUTES);
            service.cancel(id, "Payment not completed in " + UNPAID_MINUTES + " minutes", true);
        }
        for (String id : orders.cancelledWithStockHeld()) {
            orders.byId(id).ifPresent(service::releaseStock);
        }
        for (Orders.Order o : orders.refundsPending()) {
            service.refund(o);
        }
    }
}
