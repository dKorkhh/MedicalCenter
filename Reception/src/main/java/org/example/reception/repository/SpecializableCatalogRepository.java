package org.example.reception.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface SpecializableCatalogRepository<T> extends MongoRepository<T, String> {

    List<T> findBySpecializationIgnoreCase(String specialization);

    List<T> findByActiveTrue();

    List<T> findBySpecializationIgnoreCaseAndActiveTrue(String specialization);
}
