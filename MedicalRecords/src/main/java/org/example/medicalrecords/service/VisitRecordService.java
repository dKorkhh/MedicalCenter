package org.example.medicalrecords.service;

import org.example.medicalrecords.dto.VisitRecordResponse;
import org.example.medicalrecords.event.AppointmentCompletedEvent;
import org.example.medicalrecords.model.VisitRecord;

import java.util.List;

public interface VisitRecordService {

    VisitRecord processAppointmentCompleted(AppointmentCompletedEvent event);

    VisitRecordResponse getVisitRecordByAppointmentId(String appointmentId);

    List<VisitRecordResponse> getVisitRecordsByPatientId(String patientId);

    List<VisitRecordResponse> getVisitRecordsByDoctorId(String doctorId);
}
