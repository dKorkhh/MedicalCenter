package org.example.reception.repository;

import org.example.reception.model.ClinicServiceItem;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicServiceRepository extends SpecializableCatalogRepository<ClinicServiceItem> {
}
