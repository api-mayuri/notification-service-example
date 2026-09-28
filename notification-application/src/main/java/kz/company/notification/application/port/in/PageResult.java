package kz.company.notification.application.port.in;

import java.util.List;

/** Страница результатов. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return size == 0 ? 0 : (totalItems + size - 1) / size;
    }
}
