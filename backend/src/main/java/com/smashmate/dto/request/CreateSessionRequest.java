package com.smashmate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateSessionRequest {
    private Long scheduleId;

    @NotNull
    private LocalDate sessionDate;

    @NotNull
    private LocalTime startTime;

    private LocalTime endTime;
    private String venueName;
    private String notes;
}
