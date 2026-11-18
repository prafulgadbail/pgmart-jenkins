package com.pgmart.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OtpTest {

    @Test
    void codeIsSixDigits() {
        for (int i = 0; i < 100; i++) {
            assertThat(Otp.newCode()).matches("\\d{6}");
        }
    }

    @Test
    void hashMatchesOnlyTheSameEmailAndCode() {
        String hash = Otp.hash("a@b.com", "123456");
        assertThat(Otp.matches("A@B.com", "123456", hash)).isTrue();
        assertThat(Otp.matches("a@b.com", "123457", hash)).isFalse();
        assertThat(Otp.matches("c@d.com", "123456", hash)).isFalse();
    }
}
