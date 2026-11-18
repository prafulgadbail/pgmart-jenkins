package com.pgmart.user;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's profile and saved delivery addresses. */
@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    public record ProfileRequest(@NotBlank @Size(max = 60) String name,
                                 @Pattern(regexp = "^$|\\d{10}", message = "must be 10 digits") String phone) {
    }

    public record AddressRequest(@NotBlank @Size(max = 60) String fullName,
                                 @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String mobile,
                                 @NotBlank @Size(max = 160) String line1,
                                 @NotBlank @Size(max = 60) String city,
                                 @NotBlank @Size(max = 60) String state,
                                 @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String pincode,
                                 boolean isDefault) {
    }

    private final Users users;
    private final Tokens tokens;

    public ProfileController(Users users, Tokens tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @GetMapping
    public AuthController.UserView me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return AuthController.UserView.of(current(authorization));
    }

    @PutMapping
    public AuthController.UserView update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @Valid @RequestBody ProfileRequest req) {
        Users.User user = current(authorization);
        users.updateProfile(user.id(), req.name().trim(), req.phone() == null || req.phone().isBlank() ? null : req.phone());
        return AuthController.UserView.of(users.byId(user.id()).orElseThrow());
    }

    @GetMapping("/addresses")
    public List<Users.Address> addresses(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return users.addresses(current(authorization).id());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public Users.Address addAddress(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @Valid @RequestBody AddressRequest req) {
        long userId = current(authorization).id();
        if (users.addresses(userId).size() >= 10) {
            throw ApiException.badRequest("You can save up to 10 addresses");
        }
        return users.addAddress(userId, req);
    }

    @PutMapping("/addresses/{id}/default")
    public List<Users.Address> makeDefault(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable long id) {
        long userId = current(authorization).id();
        if (!users.setDefaultAddress(userId, id)) {
            throw ApiException.notFound("Address not found");
        }
        return users.addresses(userId);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable long id) {
        if (!users.deleteAddress(current(authorization).id(), id)) {
            throw ApiException.notFound("Address not found");
        }
    }

    private Users.User current(String authorization) {
        return users.byId(tokens.read(authorization).id()).orElseThrow(ApiException::unauthorized);
    }
}
