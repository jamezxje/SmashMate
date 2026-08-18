package com.smashmate.mapper;

import com.smashmate.dto.response.ScheduleResponse;
import com.smashmate.entity.RecurringSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    ScheduleResponse toResponse(RecurringSchedule schedule);
}
