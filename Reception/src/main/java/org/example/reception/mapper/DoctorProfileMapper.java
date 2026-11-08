package org.example.reception.mapper;

import org.example.reception.dto.CreateDoctorProfileRequest;
import org.example.reception.dto.DoctorProfileResponse;
import org.example.reception.dto.UpdateDoctorProfileRequest;
import org.example.reception.model.DoctorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface DoctorProfileMapper extends CatalogEntityMapper<DoctorProfile, CreateDoctorProfileRequest, UpdateDoctorProfileRequest, DoctorProfileResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currency", expression = "java(resolveCurrency(request.currency()))")
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), true))")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    DoctorProfile toEntity(CreateDoctorProfileRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currency", expression = "java(resolveCurrency(request.currency()))")
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), entity.isActive()))")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void updateEntity(@MappingTarget DoctorProfile entity, UpdateDoctorProfileRequest request);
}
