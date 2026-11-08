package org.example.reception.service;

import org.example.reception.dto.ClinicServiceResponse;
import org.example.reception.dto.CreateClinicServiceRequest;
import org.example.reception.dto.UpdateClinicServiceRequest;
import org.example.reception.mapper.ClinicServiceMapper;
import org.example.reception.model.ClinicServiceItem;
import org.example.reception.repository.ClinicServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicServiceCatalogServiceImpl
        extends AbstractSpecializableCatalogService<ClinicServiceItem, CreateClinicServiceRequest, UpdateClinicServiceRequest, ClinicServiceResponse>
        implements ClinicServiceCatalogService {

    public ClinicServiceCatalogServiceImpl(ClinicServiceRepository clinicServiceRepository, ClinicServiceMapper clinicServiceMapper) {
        super(clinicServiceRepository, clinicServiceMapper, "Clinic service");
    }

    @Override
    public ClinicServiceResponse createClinicService(CreateClinicServiceRequest request) {
        return createCatalogItem(request);
    }

    @Override
    public ClinicServiceResponse updateClinicService(String id, UpdateClinicServiceRequest request) {
        return updateCatalogItem(id, request);
    }

    @Override
    public ClinicServiceResponse getClinicServiceById(String id) {
        return getCatalogItemById(id);
    }

    @Override
    public List<ClinicServiceResponse> getClinicServices(String specialization, Boolean activeOnly) {
        return getCatalogItems(specialization, activeOnly);
    }

    @Override
    public void deleteClinicServiceById(String id) {
        deleteCatalogItemById(id);
    }
}
