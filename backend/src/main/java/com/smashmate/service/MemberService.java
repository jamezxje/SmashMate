package com.smashmate.service;

import com.smashmate.dto.request.AssignAccountRequest;
import com.smashmate.dto.request.ChangePasswordRequest;
import com.smashmate.dto.request.CreateMemberRequest;
import com.smashmate.dto.request.UpdateMemberRequest;
import com.smashmate.dto.response.MemberResponse;
import com.smashmate.entity.Member;
import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import com.smashmate.exception.BusinessException;
import com.smashmate.exception.ResourceNotFoundException;
import com.smashmate.mapper.MemberMapper;
import com.smashmate.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers(Role roleFilter) {
        var members = roleFilter != null
            ? memberRepository.findAllByRoleAndDeletedAtIsNull(roleFilter)
            : memberRepository.findAllByDeletedAtIsNull();
        return members.stream().map(memberMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberById(Long id) {
        return memberMapper.toResponse(findActiveById(id));
    }

    public MemberResponse createMember(CreateMemberRequest req) {
        if ((req.getEmail() != null && req.getPassword() == null)
            || (req.getEmail() == null && req.getPassword() != null)) {
            throw new BusinessException("INVALID_ACCOUNT",
                "Email and password must both be provided or both be absent");
        }
        if (req.getEmail() != null && memberRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already in use");
        }
        var member = Member.builder()
            .fullName(req.getFullName())
            .phone(req.getPhone())
            .email(req.getEmail())
            .password(req.getPassword() != null ? passwordEncoder.encode(req.getPassword()) : null)
            .role(Role.MEMBER)
            .status(MemberStatus.ACTIVE)
            .balance(BigDecimal.ZERO)
            .joinedDate(req.getJoinedDate() != null ? req.getJoinedDate() : LocalDate.now())
            .build();
        return memberMapper.toResponse(memberRepository.save(member));
    }

    public MemberResponse createGuest(String fullName) {
        var guest = Member.builder()
            .fullName(fullName)
            .role(Role.GUEST)
            .status(MemberStatus.ACTIVE)
            .balance(BigDecimal.ZERO)
            .joinedDate(LocalDate.now())
            .build();
        return memberMapper.toResponse(memberRepository.save(guest));
    }

    public MemberResponse updateMember(Long id, UpdateMemberRequest req) {
        var member = findActiveById(id);
        if (req.getFullName() != null) member.setFullName(req.getFullName());
        if (req.getPhone() != null) member.setPhone(req.getPhone());
        return memberMapper.toResponse(memberRepository.save(member));
    }

    public MemberResponse assignAccount(Long id, AssignAccountRequest req) {
        var member = findActiveById(id);
        if (member.getRole() == Role.GUEST) {
            throw new BusinessException("CANNOT_ASSIGN_ACCOUNT_TO_GUEST",
                "GUEST members cannot have an account");
        }
        if (memberRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already in use");
        }
        member.setEmail(req.getEmail());
        member.setPassword(passwordEncoder.encode(req.getPassword()));
        return memberMapper.toResponse(memberRepository.save(member));
    }

    public MemberResponse updateStatus(Long id, MemberStatus newStatus) {
        var member = findActiveById(id);
        member.setStatus(newStatus);
        return memberMapper.toResponse(memberRepository.save(member));
    }

    public void softDelete(Long id) {
        var member = findActiveById(id);
        member.setDeletedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    public void changePassword(String email, ChangePasswordRequest req) {
        var member = memberRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + email));
        if (!passwordEncoder.matches(req.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException("WRONG_CURRENT_PASSWORD", "Current password is incorrect");
        }
        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        memberRepository.save(member);
    }

    private Member findActiveById(Long id) {
        return memberRepository.findById(id)
            .filter(m -> m.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }
}
