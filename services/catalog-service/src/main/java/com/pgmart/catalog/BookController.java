package com.pgmart.catalog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** What every shopper sees: topics, the book list, one book, and their own wishlist. */
@RestController
public class BookController {

    private final Books books;
    private final Auth auth;

    public BookController(Books books, Auth auth) {
        this.books = books;
        this.auth = auth;
    }

    @GetMapping("/api/topics")
    public List<Books.Topic> topics() {
        return books.topics();
    }

    @GetMapping("/api/books")
    public List<Books.Book> list(@RequestParam(required = false) String q, @RequestParam(required = false) String topic) {
        if (q != null && q.length() > 100) {
            throw ApiException.badRequest("Search text is too long");
        }
        return books.search(q, topic);
    }

    @GetMapping("/api/books/{id}")
    public Books.Book one(@PathVariable long id) {
        return books.byId(id).filter(Books.Book::active)
                .orElseThrow(() -> ApiException.notFound("This book is not available"));
    }

    @GetMapping("/api/wishlist")
    public List<Long> wishlist(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return books.wishlist(auth.customer(authorization).id());
    }

    @PutMapping("/api/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addToWishlist(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable long bookId) {
        long userId = auth.customer(authorization).id();
        books.byId(bookId).filter(Books.Book::active).orElseThrow(() -> ApiException.notFound("This book is not available"));
        books.addToWishlist(userId, bookId);
    }

    @DeleteMapping("/api/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable long bookId) {
        books.removeFromWishlist(auth.customer(authorization).id(), bookId);
    }
}
