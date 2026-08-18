package com.smashmate.dto.response;

import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import com.smashmate.entity.enums.RsvpStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendeeResponse {
    private Long id;
    private Long sessionId;
    private Long memberId;
    private String memberName;
    private String memberPhone;
    private Role memberRole;
    private MemberStatus memberStatus;
    private RsvpStatus rsvpStatus;
    private Boolean checkedIn;
}
