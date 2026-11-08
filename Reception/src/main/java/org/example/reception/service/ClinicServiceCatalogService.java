package org.example.reception.service;

import org.example.reception.dto.ClinicServiceResponse;
import org.example.reception.dto.CreateClinicServiceRequest;
import org.example.reception.dto.UpdateClinicServiceRequest;

import java.util.List;

public interface ClinicServiceCatalogService {

    ClinicServiceResponse createClinicService(CreateClinicServiceRequest request);

    ClinicServiceResponse updateClinicService(String id, UpdateClinicServiceRequest request);

    ClinicServiceResponse getClinicServiceById(String id);

    List<ClinicServiceResponse> getClinicServices(String specialization, Boolean activeOnly);

    void deleteClinicServiceById(String id);
}
