package org.example.reception.mapper;

import org.example.reception.dto.PatientProfileResponse;
import org.example.reception.dto.UpsertPatientProfileRequest;
import org.example.reception.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface PatientProfileMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    PatientProfile toNewEntity(String id, UpsertPatientProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void updateEntity(@MappingTarget PatientProfile entity, UpsertPatientProfileRequest request);

    PatientProfileResponse toResponse(PatientProfile entity);

    List<PatientProfileResponse> toResponseList(List<PatientProfile> entities);
}
