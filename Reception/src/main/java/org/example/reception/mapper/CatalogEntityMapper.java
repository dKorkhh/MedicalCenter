package org.example.reception.mapper;

import org.mapstruct.MappingTarget;

import java.util.List;

public interface CatalogEntityMapper<E, C, U, R> {

    String DEFAULT_CURRENCY = "UAH";

    E toEntity(C request);

    void updateEntity(@MappingTarget E entity, U request);

    R toResponse(E entity);

    List<R> toResponseList(List<E> entities);

    default String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency.toUpperCase();
    }

    default boolean resolveActive(Boolean requestedActive, boolean fallback) {
        return requestedActive != null ? requestedActive : fallback;
    }
}
