package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.response.SessionResponse;
import com.smashmate.entity.enums.SessionStatus;

import java.util.List;

public interface SessionService {
    List<SessionResponse> getAllSessions(SessionStatus status, Integer month, Integer year);
    SessionResponse getSessionById(Long id);
    SessionResponse createSession(String adminEmail, CreateSessionRequest req);
    SessionResponse updateSession(Long id, UpdateSessionRequest req);
    SessionResponse updateStatus(Long id, SessionStatus status);
    void deleteSession(Long id);
}
