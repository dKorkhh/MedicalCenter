package org.example.reception.mapper;

import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.model.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TimeSlotMapper {

    TimeSlotResponse toResponse(TimeSlot entity);

    List<TimeSlotResponse> toResponseList(List<TimeSlot> entities);
}
