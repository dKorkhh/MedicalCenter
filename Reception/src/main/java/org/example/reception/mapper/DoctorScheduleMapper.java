package org.example.reception.mapper;

import org.example.reception.dto.DoctorScheduleResponse;
import org.example.reception.dto.UpsertDoctorScheduleRequest;
import org.example.reception.model.DoctorSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface DoctorScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), true))")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    DoctorSchedule toNewEntity(UpsertDoctorScheduleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", expression = "java(resolveActive(request.active(), entity.isActive()))")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void updateEntity(@MappingTarget DoctorSchedule entity, UpsertDoctorScheduleRequest request);

    DoctorScheduleResponse toResponse(DoctorSchedule entity);

    List<DoctorScheduleResponse> toResponseList(List<DoctorSchedule> entities);

    default boolean resolveActive(Boolean requestedActive, boolean fallback) {
        return requestedActive != null ? requestedActive : fallback;
    }
}
