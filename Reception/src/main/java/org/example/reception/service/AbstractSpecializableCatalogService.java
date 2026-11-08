package org.example.reception.service;

import org.example.reception.mapper.CatalogEntityMapper;
import org.example.reception.repository.SpecializableCatalogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public abstract class AbstractSpecializableCatalogService<E, C, U, R> {

    private final SpecializableCatalogRepository<E> repository;
    private final CatalogEntityMapper<E, C, U, R> mapper;
    private final String resourceDisplayName;

    protected AbstractSpecializableCatalogService(
            SpecializableCatalogRepository<E> repository,
            CatalogEntityMapper<E, C, U, R> mapper,
            String resourceDisplayName) {
        this.repository = repository;
        this.mapper = mapper;
        this.resourceDisplayName = resourceDisplayName;
    }

    protected R createCatalogItem(C request) {
        E entity = mapper.toEntity(request);
        E savedEntity = repository.save(entity);
        return mapper.toResponse(savedEntity);
    }

    protected R updateCatalogItem(String id, U request) {
        E existingEntity = findExistingCatalogItem(id);
        mapper.updateEntity(existingEntity, request);
        E savedEntity = repository.save(existingEntity);
        return mapper.toResponse(savedEntity);
    }

    protected R getCatalogItemById(String id) {
        return mapper.toResponse(findExistingCatalogItem(id));
    }

    protected List<R> getCatalogItems(String specialization, Boolean activeOnly) {
        return mapper.toResponseList(queryCatalogItems(specialization, Boolean.TRUE.equals(activeOnly)));
    }

    protected void deleteCatalogItemById(String id) {
        E existingEntity = findExistingCatalogItem(id);
        repository.delete(existingEntity);
    }

    private List<E> queryCatalogItems(String specialization, boolean activeOnly) {
        boolean hasSpecialization = specialization != null && !specialization.isBlank();
        if (hasSpecialization && activeOnly) {
            return repository.findBySpecializationIgnoreCaseAndActiveTrue(specialization.trim());
        }
        if (hasSpecialization) {
            return repository.findBySpecializationIgnoreCase(specialization.trim());
        }
        if (activeOnly) {
            return repository.findByActiveTrue();
        }
        return repository.findAll();
    }

    private E findExistingCatalogItem(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, resourceDisplayName + " not found with id: " + id));
    }
}
