package com.smashmate.repository;

import com.smashmate.entity.Session;
import com.smashmate.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findAllByStatus(SessionStatus status);
    List<Session> findAllBySessionDateBetween(LocalDate start, LocalDate end);
}
