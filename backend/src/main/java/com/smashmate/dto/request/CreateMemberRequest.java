package com.smashmate.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateMemberRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    private String phone;
    @Email(message = "Invalid email format")
    private String email;
    private String password;
    private LocalDate joinedDate;
}
