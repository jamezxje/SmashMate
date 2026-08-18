package com.smashmate.mapper;

import com.smashmate.dto.response.AttendeeResponse;
import com.smashmate.entity.SessionAttendee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendeeMapper {
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "memberName", source = "member.fullName")
    @Mapping(target = "memberPhone", source = "member.phone")
    @Mapping(target = "memberRole", source = "member.role")
    @Mapping(target = "memberStatus", source = "member.status")
    AttendeeResponse toResponse(SessionAttendee attendee);
}
