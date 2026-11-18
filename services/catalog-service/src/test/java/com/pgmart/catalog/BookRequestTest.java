package com.pgmart.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class BookRequestTest {

    private static AdminBookController.BookRequest request(List<String> highlights, Boolean active) {
        return new AdminBookController.BookRequest("Kubernetes Field Guide", "Run it. Scale it. Fix it.", "Containers",
                "k8s", "#326CE5", "#12206b", 119900, 69900, 10, 480, highlights, "", active);
    }

    @Test
    void emptyHighlightsAreDroppedAndTrimmed() {
        var req = request(Arrays.asList(" Rolling updates ", "", "  "), null);
        assertThat(req.highlightsOrEmpty()).containsExactly("Rolling updates");
    }

    @Test
    void missingValuesGetDefaults() {
        var req = request(null, null);
        assertThat(req.highlightsOrEmpty()).isEmpty();
        assertThat(req.activeOrTrue()).isTrue();
        assertThat(request(null, false).activeOrTrue()).isFalse();
    }
}
