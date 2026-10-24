package com.kren.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Stable pagination envelope, so the API does not leak Spring Data's {@code PageImpl} JSON. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
				page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

}
