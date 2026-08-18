package com.smashmate.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private Long sessionId;
    private String title;
    private Long assignedToId;
    private String assignedToName;
    private Boolean isDone;
    private LocalDateTime createdAt;
}
