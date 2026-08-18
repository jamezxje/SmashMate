package com.smashmate.repository;

import com.smashmate.entity.Member;
import com.smashmate.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    List<Member> findAllByDeletedAtIsNull();
    List<Member> findAllByRoleAndDeletedAtIsNull(Role role);
}
