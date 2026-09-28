package kz.company.notification.infrastructure.in.rest.dto;

import java.util.List;

/** Страница результатов. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, long totalPages) {
}
