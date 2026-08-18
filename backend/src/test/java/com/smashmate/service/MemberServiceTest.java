package com.smashmate.service;

import com.smashmate.dto.request.AssignAccountRequest;
import com.smashmate.dto.request.CreateMemberRequest;
import com.smashmate.entity.enums.Role;
import com.smashmate.exception.BusinessException;
import com.smashmate.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void createMember_offline_noEmailPassword() {
        var req = new CreateMemberRequest();
        req.setFullName("Nguyen Van A");
        var result = memberService.createMember(req);
        assertThat(result.isHasAccount()).isFalse();
        assertThat(result.getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    void createGuest_roleIsGuest() {
        var result = memberService.createGuest("Khach 1");
        assertThat(result.getRole()).isEqualTo(Role.GUEST);
        assertThat(result.isHasAccount()).isFalse();
    }

    @Test
    void assignAccount_toGuest_throws() {
        var guest = memberService.createGuest("Khach 2");
        var req = new AssignAccountRequest();
        req.setEmail("khach@test.com");
        req.setPassword("pass12345");
        assertThatThrownBy(() -> memberService.assignAccount(guest.getId(), req))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void softDelete_memberNotFoundAfterDelete() {
        var req = new CreateMemberRequest();
        req.setFullName("To Delete");
        var created = memberService.createMember(req);
        memberService.softDelete(created.getId());
        assertThatThrownBy(() -> memberService.getMemberById(created.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createMember_onlyEmail_withoutPassword_throws() {
        var req = new CreateMemberRequest();
        req.setFullName("Email Only");
        req.setEmail("test@test.com");
        assertThatThrownBy(() -> memberService.createMember(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("both");
    }
}
