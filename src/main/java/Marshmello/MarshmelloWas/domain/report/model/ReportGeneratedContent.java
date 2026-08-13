package Marshmello.MarshmelloWas.domain.report.model;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.util.Objects;

public record ReportGeneratedContent(String content) {

    private static final int MAX_CONTENT_LENGTH = 8_000;

    public ReportGeneratedContent {
        content = Objects.requireNonNull(content, "content").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must not exceed 8000 characters");
        }
    }
}
