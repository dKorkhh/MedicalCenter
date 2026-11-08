package org.example.reception.repository;

import org.example.reception.model.TimeSlot;
import org.example.reception.model.TimeSlotStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends MongoRepository<TimeSlot, String> {

    Optional<TimeSlot> findByDoctorIdAndStartTime(String doctorId, LocalDateTime startTime);

    List<TimeSlot> findByDoctorIdAndStartTimeBetweenOrderByStartTimeAsc(
            String doctorId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<TimeSlot> findByDoctorIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
            String doctorId, TimeSlotStatus status, LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<TimeSlot> findByDoctorIdOrderByStartTimeAsc(String doctorId);

    List<TimeSlot> findByDoctorIdAndStatusOrderByStartTimeAsc(String doctorId, TimeSlotStatus status);
}
