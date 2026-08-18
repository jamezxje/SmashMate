package com.smashmate.repository;

import com.smashmate.entity.SessionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionTaskRepository extends JpaRepository<SessionTask, Long> {
    List<SessionTask> findAllBySessionId(Long sessionId);
}
