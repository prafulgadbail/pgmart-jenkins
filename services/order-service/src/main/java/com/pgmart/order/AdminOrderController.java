package com.pgmart.order;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin console: all orders, move them through Packed → Out for delivery → Delivered, or cancel. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    public record StatusRequest(@NotNull OrderStatus status) {
    }

    public record Stats(long orders, long revenue, Map<String, Long> byStatus) {
    }

    private final Orders orders;
    private final OrderService service;
    private final Auth auth;

    public AdminOrderController(Orders orders, OrderService service, Auth auth) {
        this.orders = orders;
        this.service = service;
        this.auth = auth;
    }

    @GetMapping
    public List<Orders.Order> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam(required = false) OrderStatus status) {
        auth.admin(authorization);
        return orders.forAdmin(status == null ? null : status.name());
    }

    @GetMapping("/stats")
    public Stats stats(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.admin(authorization);
        Map<String, Long> byStatus = orders.countByStatus();
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum() - byStatus.get("PAYMENT_PENDING");
        return new Stats(total, orders.revenue(), byStatus);
    }

    @PatchMapping("/{id}/status")
    public Orders.Order move(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable String id, @Valid @RequestBody StatusRequest req) {
        return service.adminMove(auth.admin(authorization), id, req.status());
    }
}
