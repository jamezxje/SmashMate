package com.smashmate.controller;

import com.smashmate.common.ApiResponse;
import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.CreateTaskRequest;
import com.smashmate.dto.request.UpdateRsvpRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.request.UpdateTaskRequest;
import com.smashmate.dto.response.AttendeeResponse;
import com.smashmate.dto.response.SessionResponse;
import com.smashmate.dto.response.TaskResponse;
import com.smashmate.entity.enums.SessionStatus;
import com.smashmate.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getAll(
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getAllSessions(status, month, year)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSessionById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SessionResponse>> create(
            @Valid @RequestBody CreateSessionRequest req, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.createSession(principal.getName(), req), "Session created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SessionResponse>> update(
            @PathVariable Long id, @RequestBody UpdateSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.updateSession(id, req), "Session updated"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SessionResponse>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        SessionStatus status = SessionStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success(sessionService.updateStatus(id, status), "Session status updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Session deleted"));
    }

    @GetMapping("/{id}/attendees")
    public ResponseEntity<ApiResponse<List<AttendeeResponse>>> getAttendees(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getAttendees(id)));
    }

    @PatchMapping("/{id}/rsvp")
    public ResponseEntity<ApiResponse<AttendeeResponse>> updateRsvp(
            @PathVariable Long id, @Valid @RequestBody UpdateRsvpRequest req, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.updateRsvp(id, principal.getName(), req.getRsvpStatus()), "RSVP status updated"));
    }

    @PatchMapping("/{id}/attendees/{memberId}/checkin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendeeResponse>> checkInMember(
            @PathVariable Long id, @PathVariable Long memberId, @RequestBody Map<String, Boolean> body) {
        boolean checkedIn = body.getOrDefault("checkedIn", true);
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.checkInMember(id, memberId, checkedIn), "Check-in updated"));
    }

    @PostMapping("/{id}/attendees/guest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendeeResponse>> addGuestAttendee(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                sessionService.addGuestAttendee(id, fullName), "Guest added to session"));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getTasks(id)));
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable Long id, @Valid @RequestBody CreateTaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.createTask(id, req), "Task created"));
    }

    @PatchMapping("/{id}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id, @PathVariable Long taskId, @RequestBody UpdateTaskRequest req) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.updateTask(taskId, req), "Task updated"));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id, @PathVariable Long taskId) {
        sessionService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task deleted"));
    }
}
