package com.smashmate.dto.response;

import com.smashmate.entity.enums.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class SessionResponse {
    private Long id;
    private Long scheduleId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
    private SessionStatus status;
    private String notes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
