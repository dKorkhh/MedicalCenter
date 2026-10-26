package org.example.reception.client;

import org.example.reception.client.dto.MedicalRecordDto;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/api/medical-records")
public interface MedicalRecordsClient {

    @GetExchange
    List<MedicalRecordDto> getMedicalRecords(@RequestParam(value = "patientId", required = false) String patientId);
}
