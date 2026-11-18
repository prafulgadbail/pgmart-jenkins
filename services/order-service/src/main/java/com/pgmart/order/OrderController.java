package com.pgmart.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checkout, "Your orders", cancel, and paying for an online order. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    public record ItemRequest(@Min(1) long bookId, @Min(1) @Max(10) int qty) {
    }

    public record AddressRequest(@NotBlank @Size(max = 60) String name,
                                 @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String mobile,
                                 @NotBlank @Size(max = 160) String line1,
                                 @NotBlank @Size(max = 60) String city,
                                 @NotBlank @Size(max = 60) String state,
                                 @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String pincode) {
    }

    public record CheckoutRequest(@NotEmpty @Size(max = 30) List<@Valid ItemRequest> items,
                                  @NotNull @Valid AddressRequest address,
                                  @NotBlank @Pattern(regexp = "COD|RAZORPAY") String paymentMethod) {
    }

    public record PaymentConfirmRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
                                        @NotBlank String razorpaySignature) {
    }

    private final OrderService service;
    private final Orders orders;
    private final Auth auth;

    public OrderController(OrderService service, Orders orders, Auth auth) {
        this.service = service;
        this.orders = orders;
        this.auth = auth;
    }

    /** Idempotency-Key: the browser sends one per checkout attempt, so a double click places one order. */
    @PostMapping
    public ResponseEntity<OrderService.Placed> place(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                     @Valid @RequestBody CheckoutRequest req) {
        AuthUser user = auth.customer(authorization);
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > 64)) {
            throw ApiException.badRequest("Idempotency-Key must be 1 to 64 characters");
        }
        var a = req.address();
        var checkout = new OrderService.Checkout(
                req.items().stream().map(i -> new CatalogClient.Line(i.bookId(), i.qty())).toList(),
                new Orders.Address(a.name().trim(), a.mobile(), a.line1().trim(), a.city().trim(), a.state().trim(), a.pincode()),
                req.paymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.place(user, checkout, idempotencyKey));
    }

    @GetMapping
    public List<Orders.Order> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return orders.forUser(auth.customer(authorization).id());
    }

    @GetMapping("/{id}")
    public Orders.Order one(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @PathVariable String id) {
        return service.ownOrder(auth.customer(authorization), id);
    }

    @PostMapping("/{id}/cancel")
    public Orders.Order cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String id) {
        return service.customerCancel(auth.customer(authorization), id);
    }

    /** Opens the Razorpay popup again for an order that is still waiting for payment. */
    @GetMapping("/{id}/payment")
    public OrderService.PaymentStart payment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable String id) {
        return service.paymentFor(auth.customer(authorization), id);
    }

    @PostMapping("/{id}/payment")
    public Orders.Order confirmPayment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String id, @Valid @RequestBody PaymentConfirmRequest req) {
        return service.confirmPayment(auth.customer(authorization), id, req.razorpayOrderId(), req.razorpayPaymentId(),
                req.razorpaySignature());
    }
}
