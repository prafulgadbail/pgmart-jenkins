package com.pgmart.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The bell in the store header. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final Orders orders;
    private final Auth auth;

    public NotificationController(Orders orders, Auth auth) {
        this.orders = orders;
        this.auth = auth;
    }

    @GetMapping
    public List<Orders.Notification> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return orders.notifications(auth.user(authorization).id());
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@RequestHeader(value = "Authorization", required = false) String authorization) {
        orders.markAllRead(auth.user(authorization).id());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@RequestHeader(value = "Authorization", required = false) String authorization) {
        orders.clearNotifications(auth.user(authorization).id());
    }
}
