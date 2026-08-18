package com.smashmate.service;

import com.smashmate.dto.request.ChangePasswordRequest;
import com.smashmate.entity.Member;
import com.smashmate.exception.BusinessException;
import com.smashmate.exception.ResourceNotFoundException;
import com.smashmate.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(String email, ChangePasswordRequest req) {
        Member member = memberRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + email));
        if (!passwordEncoder.matches(req.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException("WRONG_CURRENT_PASSWORD", "Current password is incorrect");
        }
        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        memberRepository.save(member);
    }
}
