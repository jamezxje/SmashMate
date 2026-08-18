package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskRequest {
    private String title;
    private Long assignedToId;
    private Boolean isDone;
}
