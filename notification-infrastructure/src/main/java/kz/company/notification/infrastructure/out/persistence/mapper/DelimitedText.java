package kz.company.notification.infrastructure.out.persistence.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Короткие множества в текстовой колонке через запятую. */
final class DelimitedText {

    private DelimitedText() {
    }

    static <T> String join(Collection<T> values, Function<T, String> toText) {
        return values.stream().map(toText).sorted().collect(Collectors.joining(","));
    }

    static <T> Set<T> split(String text, Function<String, T> parser) {
        if (text == null || text.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(text.split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(parser)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
