package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRequest {
    private String fullName;
    private String phone;
}
