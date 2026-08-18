package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateSessionRequest {
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
    private String notes;
}
