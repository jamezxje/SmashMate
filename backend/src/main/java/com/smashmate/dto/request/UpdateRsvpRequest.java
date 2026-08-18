package com.smashmate.dto.request;

import com.smashmate.entity.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRsvpRequest {
    @NotNull
    private RsvpStatus rsvpStatus;
}
