package org.example.reception.repository;

import org.example.reception.model.DoctorProfile;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorProfileRepository extends SpecializableCatalogRepository<DoctorProfile> {
}
