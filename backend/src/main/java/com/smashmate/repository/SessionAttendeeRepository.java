package com.smashmate.repository;

import com.smashmate.entity.SessionAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionAttendeeRepository extends JpaRepository<SessionAttendee, Long> {
    List<SessionAttendee> findAllBySessionId(Long sessionId);
    Optional<SessionAttendee> findBySessionIdAndMemberId(Long sessionId, Long memberId);
}
