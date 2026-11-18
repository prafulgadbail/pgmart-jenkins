package com.pgmart.catalog;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin console: add and edit books, change stock, hide or show a book. */
@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {

    private static final Logger log = LoggerFactory.getLogger(AdminBookController.class);
    static final int LOW_STOCK = 5;

    public record BookRequest(@NotBlank @Size(max = 80) String title,
                              @NotBlank @Size(max = 120) String subtitle,
                              @NotBlank String topic,
                              @NotBlank @Size(max = 40) String logo,
                              @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String color1,
                              @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String color2,
                              @Min(100) @Max(10_000_000) long mrp,
                              @Min(100) @Max(10_000_000) long price,
                              @Min(0) @Max(100_000) int stock,
                              @Min(0) @Max(5_000) int pages,
                              List<@Size(max = 120) String> highlights,
                              @Pattern(regexp = "^$|https://.+", message = "must start with https://") String imageUrl,
                              Boolean active) {

        List<String> highlightsOrEmpty() {
            return highlights == null ? List.of()
                    : highlights.stream().map(String::trim).filter(h -> !h.isEmpty()).limit(6).toList();
        }

        boolean activeOrTrue() {
            return active == null || active;
        }
    }

    public record StockRequest(@Min(0) @Max(100_000) int stock) {
    }

    public record Stats(int books, int lowStock) {
    }

    private final Books books;
    private final Auth auth;

    public AdminBookController(Books books, Auth auth) {
        this.books = books;
        this.auth = auth;
    }

    @GetMapping
    public List<Books.Book> all(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.admin(authorization);
        return books.all();
    }

    @GetMapping("/stats")
    public Stats stats(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.admin(authorization);
        return new Stats((int) books.all().stream().filter(Books.Book::active).count(), books.lowStock(LOW_STOCK));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Books.Book create(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @Valid @RequestBody BookRequest req) {
        AuthUser admin = auth.admin(authorization);
        check(req);
        Books.Book book = books.create(req);
        log.info("Admin {} added book {}", admin.id(), book.id());
        return book;
    }

    @PutMapping("/{id}")
    public Books.Book update(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable long id, @Valid @RequestBody BookRequest req) {
        AuthUser admin = auth.admin(authorization);
        check(req);
        Books.Book book = books.update(id, req).orElseThrow(() -> ApiException.notFound("Book not found"));
        log.info("Admin {} updated book {}", admin.id(), id);
        return book;
    }

    @PatchMapping("/{id}/stock")
    public Books.Book stock(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @PathVariable long id, @Valid @RequestBody StockRequest req) {
        AuthUser admin = auth.admin(authorization);
        Books.Book book = books.setStock(id, req.stock()).orElseThrow(() -> ApiException.notFound("Book not found"));
        log.info("Admin {} set stock of book {} to {}", admin.id(), id, req.stock());
        return book;
    }

    /** Books are hidden, not deleted: old orders still point to them. */
    @PatchMapping("/{id}/active")
    public Books.Book active(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable long id, @RequestBody Map<String, Boolean> body) {
        AuthUser admin = auth.admin(authorization);
        boolean active = Boolean.TRUE.equals(body.get("active"));
        Books.Book book = books.setActive(id, active).orElseThrow(() -> ApiException.notFound("Book not found"));
        log.info("Admin {} {} book {}", admin.id(), active ? "showed" : "hid", id);
        return book;
    }

    private void check(BookRequest req) {
        if (req.price() > req.mrp()) {
            throw ApiException.badRequest("Selling price cannot be above the MRP");
        }
        if (!books.topicExists(req.topic())) {
            throw ApiException.badRequest("Unknown topic: " + req.topic());
        }
    }
}
