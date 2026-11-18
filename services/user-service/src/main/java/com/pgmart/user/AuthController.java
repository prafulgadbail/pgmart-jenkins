package com.pgmart.user;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign up with an email OTP, login, and password reset.
 * A new account can log in only after the 6-digit code from the email is entered.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Duration OTP_VALID = Duration.ofMinutes(10);
    private static final Duration OTP_RESEND_AFTER = Duration.ofSeconds(60);
    private static final int OTP_MAX_ATTEMPTS = 5;

    public record RegisterRequest(@NotBlank @Size(max = 60) String name,
                                  @NotBlank @Email @Size(max = 120) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record EmailRequest(@NotBlank @Email String email) {
    }

    public record VerifyRequest(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
    }

    public record ResetRequest(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{6}") String code,
                               @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record UserView(long id, String name, String email, String phone, String role) {
        static UserView of(Users.User u) {
            return new UserView(u.id(), u.name(), u.email(), u.phone(), u.role());
        }
    }

    public record Session(String token, UserView user) {
    }

    private final Users users;
    private final Tokens tokens;
    private final Mailer mailer;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AuthController(Users users, Tokens tokens, Mailer mailer) {
        this.users = users;
        this.tokens = tokens;
        this.mailer = mailer;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        String email = normalize(req.email());
        String name = req.name().trim();
        String hash = passwords.encode(req.password());

        var existing = users.byEmail(email);
        if (existing.isPresent() && existing.get().emailVerified()) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists. Please log in.");
        }
        if (existing.isPresent()) {
            users.restartSignup(existing.get().id(), name, hash);
        } else {
            users.create(email, name, hash, "CUSTOMER", false);
        }
        sendCode(email, name, "VERIFY");
        log.info("Sign-up started");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("email", email, "message", "We sent a 6-digit code to " + email));
    }

    @PostMapping("/verify")
    @Transactional(noRollbackFor = ApiException.class)
    public Session verify(@Valid @RequestBody VerifyRequest req) {
        String email = normalize(req.email());
        Users.User user = users.byEmail(email).orElseThrow(() -> ApiException.badRequest("No sign-up found for this email"));
        checkCode(email, req.code(), "VERIFY");
        users.markVerified(user.id());
        log.info("Email verified for user {}", user.id());
        Users.User verified = users.byId(user.id()).orElseThrow();
        return new Session(tokens.issue(verified), UserView.of(verified));
    }

    @PostMapping("/resend-code")
    @Transactional
    public ResponseEntity<Map<String, String>> resend(@Valid @RequestBody EmailRequest req) {
        String email = normalize(req.email());
        users.byEmail(email)
                .filter(u -> !u.emailVerified())
                .ifPresent(u -> sendCode(email, u.name(), "VERIFY"));
        return ResponseEntity.accepted().body(Map.of("message", "If the email is waiting for verification, a new code is on its way"));
    }

    @PostMapping("/login")
    @Transactional(noRollbackFor = ApiException.class)
    public Session login(@Valid @RequestBody LoginRequest req) {
        String email = normalize(req.email());
        Users.User user = users.byEmail(email)
                .filter(u -> passwords.matches(req.password(), u.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Wrong email or password"));
        if (!user.emailVerified()) {
            try {
                sendCode(email, user.name(), "VERIFY");
            } catch (ApiException e) {
                // a code was sent less than a minute ago: the user can still enter it
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please verify your email first. We sent a code to " + email);
        }
        log.info("User {} logged in", user.id());
        return new Session(tokens.issue(user), UserView.of(user));
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody EmailRequest req) {
        String email = normalize(req.email());
        users.byEmail(email)
                .filter(Users.User::emailVerified)
                .ifPresent(u -> sendCode(email, u.name(), "RESET"));
        // Same answer whether the account exists or not, so emails cannot be guessed
        return ResponseEntity.accepted().body(Map.of("message", "If an account exists for " + email + ", a reset code is on its way"));
    }

    @PostMapping("/reset-password")
    @Transactional(noRollbackFor = ApiException.class)
    public Session reset(@Valid @RequestBody ResetRequest req) {
        String email = normalize(req.email());
        Users.User user = users.byEmail(email).orElseThrow(() -> ApiException.badRequest("Wrong or expired code"));
        checkCode(email, req.code(), "RESET");
        users.updatePassword(user.id(), passwords.encode(req.newPassword()));
        log.info("Password reset for user {}", user.id());
        return new Session(tokens.issue(user), UserView.of(user));
    }

    // ---------- helpers ----------

    private void sendCode(String email, String name, String purpose) {
        users.otp(email, purpose).ifPresent(o -> {
            long wait = OTP_RESEND_AFTER.getSeconds()
                    - Duration.between(o.sentAt(), OffsetDateTime.now()).getSeconds();
            if (wait > 0) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "WAIT",
                        "Please wait " + wait + " seconds before asking for a new code");
            }
        });
        String code = Otp.newCode();
        users.saveOtp(email, purpose, Otp.hash(email, code), OffsetDateTime.now().plus(OTP_VALID));
        mailer.sendOtp(email, name, code, purpose);
    }

    private void checkCode(String email, String code, String purpose) {
        Users.OtpRow otp = users.otp(email, purpose)
                .orElseThrow(() -> ApiException.badRequest("No code found. Please ask for a new one."));
        if (otp.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED", "This code has expired. Please ask for a new one.");
        }
        if (otp.attempts() >= OTP_MAX_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", "Too many wrong codes. Please ask for a new one.");
        }
        if (!Otp.matches(email, code, otp.codeHash())) {
            users.countAttempt(email, purpose);
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_WRONG", "This code is not correct");
        }
        users.deleteOtp(email, purpose);
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
