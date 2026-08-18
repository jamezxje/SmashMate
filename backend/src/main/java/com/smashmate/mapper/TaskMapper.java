package com.smashmate.mapper;

import com.smashmate.dto.response.TaskResponse;
import com.smashmate.entity.SessionTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    @Mapping(target = "assignedToName", source = "assignedTo.fullName")
    TaskResponse toResponse(SessionTask task);
}
