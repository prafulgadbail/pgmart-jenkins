package com.pgmart.catalog;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Called only by order-service, inside the cluster. /internal is never routed from the internet. */
@RestController
@RequestMapping("/internal/stock")
public class InternalStockController {

    public record ReserveRequest(@NotBlank String orderId, @NotEmpty List<Stock.Line> items) {
    }

    public record ReleaseRequest(@NotBlank String orderId) {
    }

    private final Stock stock;
    private final Auth auth;

    public InternalStockController(Stock stock, Auth auth) {
        this.stock = stock;
        this.auth = auth;
    }

    @PostMapping("/reserve")
    public List<Stock.ReservedLine> reserve(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                            @Valid @RequestBody ReserveRequest req) {
        auth.internal(token);
        return stock.reserve(req.orderId(), req.items());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                        @Valid @RequestBody ReleaseRequest req) {
        auth.internal(token);
        stock.release(req.orderId());
    }
}
