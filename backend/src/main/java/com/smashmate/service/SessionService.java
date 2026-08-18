package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.CreateTaskRequest;
import com.smashmate.dto.request.UpdateRsvpRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.request.UpdateTaskRequest;
import com.smashmate.dto.response.AttendeeResponse;
import com.smashmate.dto.response.SessionResponse;
import com.smashmate.dto.response.TaskResponse;
import com.smashmate.entity.enums.RsvpStatus;
import com.smashmate.entity.enums.SessionStatus;

import java.util.List;

public interface SessionService {
    List<SessionResponse> getAllSessions(SessionStatus status, Integer month, Integer year);
    SessionResponse getSessionById(Long id);
    SessionResponse createSession(String adminEmail, CreateSessionRequest req);
    SessionResponse updateSession(Long id, UpdateSessionRequest req);
    SessionResponse updateStatus(Long id, SessionStatus status);
    void deleteSession(Long id);

    List<AttendeeResponse> getAttendees(Long sessionId);
    AttendeeResponse updateRsvp(Long sessionId, String userEmail, RsvpStatus rsvpStatus);
    AttendeeResponse checkInMember(Long sessionId, Long memberId, boolean checkedIn);
    AttendeeResponse addGuestAttendee(Long sessionId, String fullName);

    List<TaskResponse> getTasks(Long sessionId);
    TaskResponse createTask(Long sessionId, CreateTaskRequest req);
    TaskResponse updateTask(Long taskId, UpdateTaskRequest req);
    void deleteTask(Long taskId);
}
