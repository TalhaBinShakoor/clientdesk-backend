package com.clientdesk.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record ApiPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <S, T> ApiPage<T> from(Page<S> source, Function<S, T> mapper) {
        Page<T> mapped = source.map(mapper);
        return new ApiPage<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isFirst(),
                mapped.isLast()
        );
    }
}
