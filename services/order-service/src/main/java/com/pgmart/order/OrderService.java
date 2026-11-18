package com.pgmart.order;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checkout and everything that happens to an order afterwards.
 *
 * Calls to other systems (catalog-service, Razorpay) are made outside database transactions.
 * If one fails half way, the order keeps a flag (stock_released, REFUND_PENDING) and
 * {@link Housekeeping} finishes the job later.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public record Checkout(List<CatalogClient.Line> items, Orders.Address address, String paymentMethod) {
    }

    /** What the frontend needs to open the Razorpay checkout popup. */
    public record PaymentStart(String keyId, String razorpayOrderId, long amount) {
    }

    public record Placed(Orders.Order order, PaymentStart payment) {
    }

    private final Orders orders;
    private final CatalogClient catalog;
    private final Razorpay razorpay;
    private final Notifier notifier;
    private final TransactionTemplate tx;

    public OrderService(Orders orders, CatalogClient catalog, Razorpay razorpay, Notifier notifier, TransactionTemplate tx) {
        this.orders = orders;
        this.catalog = catalog;
        this.razorpay = razorpay;
        this.notifier = notifier;
        this.tx = tx;
    }

    // ---------- checkout ----------

    public Placed place(AuthUser user, Checkout req, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = orders.byIdempotencyKey(user.id(), idempotencyKey);
            if (existing.isPresent()) {
                return new Placed(existing.get(), paymentStart(existing.get()));
            }
        }
        boolean online = "RAZORPAY".equals(req.paymentMethod());
        if (online && !razorpay.enabled()) {
            throw ApiException.badRequest("Online payment is not set up. Please choose Cash on Delivery.");
        }

        // 1. Take the stock. Prices come from catalog-service, never from the browser.
        String orderId = UUID.randomUUID().toString();
        List<CatalogClient.ReservedLine> lines = catalog.reserve(orderId, req.items());
        Pricing.Totals totals = Pricing.of(lines);

        // 2. Save the order. If that fails, give the stock back.
        OrderStatus status = online ? OrderStatus.PAYMENT_PENDING : OrderStatus.PLACED;
        var newOrder = new Orders.NewOrder(orderId, user.id(), user.name(), user.email(), status,
                online ? "RAZORPAY" : "COD", online ? "PENDING" : "COD_DUE", totals, req.address(), idempotencyKey);
        Orders.Order order;
        Notifier.Message message;
        try {
            Object[] result = tx.execute(s -> {
                Orders.Order o = orders.insert(newOrder, lines, online ? "Waiting for online payment" : "Order placed, Cash on Delivery");
                return new Object[] {o, online ? null : notifier.record(o, OrderStatus.PLACED)};
            });
            order = (Orders.Order) result[0];
            message = (Notifier.Message) result[1];
        } catch (RuntimeException e) {
            catalog.release(orderId);
            throw e;
        }
        log.info("Order {} created payment={} total={}", order.number(), order.paymentMethod(), order.total());

        if (!online) {
            notifier.email(order, message);
            return new Placed(order, null);
        }

        // 3. Online: create the Razorpay order the popup will pay
        try {
            String rzpOrder = razorpay.createOrder(order.total(), order.number(), order.id());
            orders.setRazorpayOrder(order.id(), rzpOrder);
            Orders.Order withRzp = orders.byId(order.id()).orElseThrow();
            return new Placed(withRzp, paymentStart(withRzp));
        } catch (ApiException e) {
            cancel(order.id(), "Payment could not be started", false);
            throw e;
        }
    }

    /** Checkout popup succeeded: check Razorpay's signature, then the order is placed. */
    public Orders.Order confirmPayment(AuthUser user, String orderId, String razorpayOrderId, String paymentId, String signature) {
        Orders.Order current = ownOrder(user, orderId);
        if (current.razorpayOrderId() == null || !current.razorpayOrderId().equals(razorpayOrderId)
                || !razorpay.paymentSignatureValid(razorpayOrderId, paymentId, signature)) {
            log.warn("Payment signature check failed for order {}", current.number());
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID", "We could not confirm this payment");
        }
        markPaid(razorpayOrderId, paymentId, "checkout");
        return orders.byId(orderId).orElseThrow();
    }

    /** Used by the checkout callback and by the webhook; whichever arrives first wins, the second does nothing. */
    public void markPaid(String razorpayOrderId, String paymentId, String source) {
        Object[] result = tx.execute(s -> {
            Orders.Order o = orders.byRazorpayOrderId(razorpayOrderId).orElse(null);
            if (o == null) {
                log.warn("Payment {} for unknown Razorpay order {}", paymentId, razorpayOrderId);
                return null;
            }
            if (o.status() == OrderStatus.PAYMENT_PENDING) {
                orders.setPaid(o.id(), paymentId, "Payment received online");
                Orders.Order paid = orders.byId(o.id()).orElseThrow();
                return new Object[] {paid, notifier.record(paid, OrderStatus.PLACED)};
            }
            if (o.status() == OrderStatus.CANCELLED && !"REFUNDED".equals(o.paymentStatus())
                    && !"REFUND_PENDING".equals(o.paymentStatus())) {
                // Paid after the order had already expired: give the money back
                orders.setPaymentId(o.id(), paymentId);
                orders.setPaymentStatus(o.id(), "REFUND_PENDING");
                log.warn("Payment {} arrived for cancelled order {}: refund queued", paymentId, o.number());
            }
            return null;
        });
        if (result != null) {
            Orders.Order paid = (Orders.Order) result[0];
            log.info("Order {} paid via {}", paid.number(), source);
            notifier.email(paid, (Notifier.Message) result[1]);
        }
    }

    // ---------- after checkout ----------

    public Orders.Order customerCancel(AuthUser user, String orderId) {
        Orders.Order o = ownOrder(user, orderId);
        if (!o.status().customerCanCancel()) {
            throw ApiException.conflict("TOO_LATE", "This order is already on its way and cannot be cancelled");
        }
        return cancel(orderId, "Cancelled by you", true);
    }

    public Orders.Order adminMove(AuthUser admin, String orderId, OrderStatus next) {
        if (next == OrderStatus.CANCELLED) {
            Orders.Order o = orders.byId(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
            if (!o.status().adminCanMoveTo(OrderStatus.CANCELLED)) {
                throw ApiException.conflict("BAD_STATUS", "An order that is " + o.status() + " cannot be cancelled");
            }
            log.info("Admin {} cancels order {}", admin.id(), o.number());
            return cancel(orderId, "Cancelled by the store", true);
        }
        Object[] result = tx.execute(s -> {
            Orders.Order o = orders.lock(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
            if (!o.status().adminCanMoveTo(next)) {
                throw ApiException.conflict("BAD_STATUS", "Cannot move an order from " + o.status() + " to " + next);
            }
            // Cash on Delivery is paid when the parcel is handed over
            String payment = next == OrderStatus.DELIVERED && "COD".equals(o.paymentMethod()) ? "PAID" : o.paymentStatus();
            orders.setStatus(orderId, next, payment, switch (next) {
                case PACKED -> "Packed at the store";
                case OUT_FOR_DELIVERY -> "Handed to the delivery rider";
                case DELIVERED -> "COD".equals(o.paymentMethod()) ? "Delivered, cash collected" : "Delivered";
                default -> next.name();
            });
            Orders.Order moved = orders.byId(orderId).orElseThrow();
            return new Object[] {moved, notifier.record(moved, next)};
        });
        Orders.Order moved = (Orders.Order) result[0];
        log.info("Admin {} moved order {} to {}", admin.id(), moved.number(), next);
        notifier.email(moved, (Notifier.Message) result[1]);
        return moved;
    }

    /**
     * Cancels an order, gives the stock back and refunds an online payment.
     * The status change is committed first; stock and refund are retried by Housekeeping if they fail now.
     */
    Orders.Order cancel(String orderId, String note, boolean tellCustomer) {
        Object[] result = tx.execute(s -> {
            Orders.Order o = orders.lock(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
            if (o.status() == OrderStatus.CANCELLED) {
                return new Object[] {o, null};
            }
            String payment = switch (o.paymentStatus()) {
                case "PAID" -> "REFUND_PENDING";
                case "PENDING", "COD_DUE" -> "NOT_PAID";
                default -> o.paymentStatus();
            };
            orders.setStatus(orderId, OrderStatus.CANCELLED, payment, note);
            Orders.Order cancelled = orders.byId(orderId).orElseThrow();
            return new Object[] {cancelled, tellCustomer ? notifier.record(cancelled, OrderStatus.CANCELLED) : null};
        });
        Orders.Order cancelled = (Orders.Order) result[0];
        releaseStock(cancelled);
        if ("REFUND_PENDING".equals(cancelled.paymentStatus())) {
            refund(cancelled);
        }
        if (result[1] != null) {
            log.info("Order {} cancelled: {}", cancelled.number(), note);
            notifier.email(cancelled, (Notifier.Message) result[1]);
        }
        return orders.byId(orderId).orElseThrow();
    }

    void releaseStock(Orders.Order o) {
        if (!o.stockReleased() && catalog.release(o.id())) {
            orders.markStockReleased(o.id());
        }
    }

    void refund(Orders.Order o) {
        if (o.razorpayPaymentId() != null && razorpay.refund(o.razorpayPaymentId(), o.total())) {
            orders.setPaymentStatus(o.id(), "REFUNDED");
        }
    }

    Orders.Order ownOrder(AuthUser user, String orderId) {
        return orders.byId(orderId)
                .filter(o -> o.userId() == user.id())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    private PaymentStart paymentStart(Orders.Order o) {
        return o.status() == OrderStatus.PAYMENT_PENDING && o.razorpayOrderId() != null
                ? new PaymentStart(razorpay.keyId(), o.razorpayOrderId(), o.total())
                : null;
    }

    PaymentStart paymentFor(AuthUser user, String orderId) {
        Orders.Order o = ownOrder(user, orderId);
        PaymentStart start = paymentStart(o);
        if (start == null) {
            throw ApiException.conflict("NOT_PENDING", "This order is not waiting for payment");
        }
        return start;
    }
}
