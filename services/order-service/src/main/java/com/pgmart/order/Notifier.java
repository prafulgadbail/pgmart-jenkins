package com.pgmart.order;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Tells the customer about their order: a row for the bell in the store, and an email.
 * The email is sent in the background, so a slow mail server never slows down checkout.
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public record Message(String title, String body) {
    }

    private final Orders orders;
    private final JavaMailSender mail;
    private final String from;
    private final String storeUrl;

    public Notifier(Orders orders, JavaMailSender mail, @Value("${app.mail-from}") String from,
                    @Value("${app.store-url}") String storeUrl) {
        this.orders = orders;
        this.mail = mail;
        this.from = from;
        this.storeUrl = storeUrl;
    }

    public static Message messageFor(Orders.Order o, OrderStatus status) {
        String n = o.number();
        String amount = Pricing.rupees(o.total());
        return switch (status) {
            case PLACED -> "COD".equals(o.paymentMethod())
                    ? new Message("Order " + n + " placed", "Thank you! Please keep " + amount + " ready to pay in cash when it arrives.")
                    : new Message("Order " + n + " placed", "Payment of " + amount + " received. We are packing your books.");
            case PACKED -> new Message("Order " + n + " is packed", "Your books are packed and will leave the store soon.");
            case OUT_FOR_DELIVERY -> new Message("Order " + n + " is out for delivery",
                    "COD".equals(o.paymentMethod()) ? "The rider is on the way. Please keep " + amount + " ready."
                            : "The rider is on the way.");
            case DELIVERED -> new Message("Order " + n + " delivered", "Enjoy your books. Thank you for shopping with PGMart!");
            case CANCELLED -> new Message("Order " + n + " cancelled", "PAID".equals(o.paymentStatus())
                    || "REFUND_PENDING".equals(o.paymentStatus()) || "REFUNDED".equals(o.paymentStatus())
                    ? "Your refund of " + amount + " has been started. It reaches your account in 5-7 working days."
                    : "Your order has been cancelled.");
            case PAYMENT_PENDING -> new Message("Order " + n + " waiting for payment", "Complete the payment to place the order.");
        };
    }

    /** Call inside the transaction that changed the order, so the bell and the order never disagree. */
    public Message record(Orders.Order o, OrderStatus status) {
        Message m = messageFor(o, status);
        orders.notify(o.userId(), o.id(), m.title(), m.body());
        return m;
    }

    @Async
    public void email(Orders.Order o, Message m) {
        String items = o.items().stream()
                .map(i -> "  - " + i.title() + " x " + i.qty() + "  " + Pricing.rupees(i.price() * i.qty()))
                .collect(Collectors.joining("\n"));
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(o.customerEmail());
        msg.setSubject("PGMart: " + m.title());
        msg.setText("""
                Hi %s,

                %s

                Order %s
                %s
                Delivery: %s
                Total: %s (%s)

                Deliver to: %s, %s, %s, %s - %s

                Track your order: %s/shop/orders

                PGMart · Cloud & DevOps Book Store
                """.formatted(o.customerName(), m.body(), o.number(), items, Pricing.rupees(o.delivery()),
                Pricing.rupees(o.total()), "COD".equals(o.paymentMethod()) ? "Cash on Delivery" : "Paid online",
                o.address().name(), o.address().line1(), o.address().city(), o.address().state(), o.address().pincode(),
                storeUrl));
        try {
            mail.send(msg);
            log.info("Order email sent order={} subject=\"{}\"", o.number(), m.title());
        } catch (MailException e) {
            // The order is fine; only the email failed. The bell in the store still shows the update.
            log.error("Order email failed order={}", o.number(), e);
        }
    }
}
