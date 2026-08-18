package com.smashmate.service;

import com.smashmate.dto.request.AssignAccountRequest;
import com.smashmate.dto.request.ChangePasswordRequest;
import com.smashmate.dto.request.CreateMemberRequest;
import com.smashmate.dto.request.UpdateMemberRequest;
import com.smashmate.dto.response.MemberResponse;
import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;

import java.util.List;

public interface MemberService {
    List<MemberResponse> getAllMembers(Role roleFilter);
    MemberResponse getMemberById(Long id);
    MemberResponse createMember(CreateMemberRequest req);
    MemberResponse createGuest(String fullName);
    MemberResponse updateMember(Long id, UpdateMemberRequest req);
    MemberResponse assignAccount(Long id, AssignAccountRequest req);
    MemberResponse updateStatus(Long id, MemberStatus newStatus);
    void softDelete(Long id);
    void changePassword(String email, ChangePasswordRequest req);
}
