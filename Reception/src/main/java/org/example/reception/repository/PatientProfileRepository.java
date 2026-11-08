package org.example.reception.repository;

import org.example.reception.model.PatientProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientProfileRepository extends MongoRepository<PatientProfile, String> {
}
