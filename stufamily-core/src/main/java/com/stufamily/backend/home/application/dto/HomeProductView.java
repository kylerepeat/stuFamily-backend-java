package com.stufamily.backend.home.application.dto;

public record HomeProductView(
    Long id,
    String type,
    String title,
    long priceCents,
    boolean top,
    String publishStatus
) {
}
