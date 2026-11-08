package org.example.reception.repository;

import org.example.reception.model.DoctorSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends MongoRepository<DoctorSchedule, String> {

    Optional<DoctorSchedule> findByDoctorId(String doctorId);

    List<DoctorSchedule> findByActiveTrue();
}
