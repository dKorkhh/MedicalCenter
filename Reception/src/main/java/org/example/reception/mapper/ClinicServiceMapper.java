package org.example.reception.mapper;

import org.example.reception.dto.ClinicServiceResponse;
import org.example.reception.dto.CreateClinicServiceRequest;
import org.example.reception.dto.UpdateClinicServiceRequest;
import org.example.reception.model.ClinicServiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface ClinicServiceMapper extends CatalogEntityMapper<ClinicServiceItem, CreateClinicServiceRequest, UpdateClinicServiceRequest, ClinicServiceResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currency", expression = "java(resolveCurrency(request.currency()))")
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), true))")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    ClinicServiceItem toEntity(CreateClinicServiceRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currency", expression = "java(resolveCurrency(request.currency()))")
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), entity.isActive()))")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void updateEntity(@MappingTarget ClinicServiceItem entity, UpdateClinicServiceRequest request);
}
