package com.smashmate.dto.response;

import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private Role role;
    private MemberStatus status;
    private BigDecimal balance;
    private LocalDate joinedDate;
    private LocalDateTime createdAt;
    private boolean hasAccount;
}
