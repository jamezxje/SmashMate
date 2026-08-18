package com.smashmate.mapper;

import com.smashmate.dto.response.SessionResponse;
import com.smashmate.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    @Mapping(target = "scheduleId", source = "schedule.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    SessionResponse toResponse(Session session);
}
