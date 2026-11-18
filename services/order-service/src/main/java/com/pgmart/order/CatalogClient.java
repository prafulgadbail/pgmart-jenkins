package com.pgmart.order;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls catalog-service to take stock for an order and to give it back. */
@Component
public class CatalogClient {

    public record Line(long bookId, int qty) {
    }

    public record ReservedLine(long bookId, String title, int qty, long price, long mrp) {
    }

    private final RestClient http;

    public CatalogClient(@Value("${app.catalog-url}") String baseUrl, @Value("${app.internal-token}") String token) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Token", token)
                .build();
    }

    public List<ReservedLine> reserve(String orderId, List<Line> items) {
        try {
            ReservedLine[] lines = http.post().uri("/internal/stock/reserve")
                    .body(Map.of("orderId", orderId, "items", items))
                    .retrieve().body(ReservedLine[].class);
            return lines == null ? List.of() : List.of(lines);
        } catch (HttpClientErrorException e) {
            // 409 out of stock / book hidden, 400 bad quantity: pass the message to the shopper
            @SuppressWarnings("unchecked")
            Map<String, String> body = e.getResponseBodyAs(Map.class);
            String code = body == null ? "STOCK" : body.getOrDefault("code", "STOCK");
            String message = body == null ? "Could not reserve stock" : body.getOrDefault("message", "Could not reserve stock");
            throw new ApiException(HttpStatus.valueOf(e.getStatusCode().value()), code, message);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_DOWN",
                    "We could not check stock right now. Please try again in a moment.");
        }
    }

    /** Safe to call more than once. Returns false if catalog-service could not be reached. */
    public boolean release(String orderId) {
        try {
            http.post().uri("/internal/stock/release").body(Map.of("orderId", orderId)).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }
}
