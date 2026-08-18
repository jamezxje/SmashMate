package com.smashmate.service.impl;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.CreateTaskRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.request.UpdateTaskRequest;
import com.smashmate.dto.response.AttendeeResponse;
import com.smashmate.dto.response.SessionResponse;
import com.smashmate.dto.response.TaskResponse;
import com.smashmate.entity.Member;
import com.smashmate.entity.Session;
import com.smashmate.entity.SessionAttendee;
import com.smashmate.entity.SessionTask;
import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import com.smashmate.entity.enums.RsvpStatus;
import com.smashmate.entity.enums.SessionStatus;
import com.smashmate.exception.ResourceNotFoundException;
import com.smashmate.mapper.AttendeeMapper;
import com.smashmate.mapper.SessionMapper;
import com.smashmate.mapper.TaskMapper;
import com.smashmate.repository.MemberRepository;
import com.smashmate.repository.RecurringScheduleRepository;
import com.smashmate.repository.SessionAttendeeRepository;
import com.smashmate.repository.SessionRepository;
import com.smashmate.repository.SessionTaskRepository;
import com.smashmate.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final RecurringScheduleRepository scheduleRepository;
    private final SessionAttendeeRepository attendeeRepository;
    private final SessionTaskRepository taskRepository;
    private final SessionMapper sessionMapper;
    private final AttendeeMapper attendeeMapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getAllSessions(SessionStatus status, Integer month, Integer year) {
        List<Session> sessions;
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            sessions = sessionRepository.findAllBySessionDateBetween(ym.atDay(1), ym.atEndOfMonth());
        } else if (status != null) {
            sessions = sessionRepository.findAllByStatus(status);
        } else {
            sessions = sessionRepository.findAll();
        }
        return sessions.stream().map(sessionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long id) {
        return sessionMapper.toResponse(findById(id));
    }

    @Override
    public SessionResponse createSession(String adminEmail, CreateSessionRequest req) {
        var admin = memberRepository.findByEmailAndDeletedAtIsNull(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + adminEmail));
        var schedule = req.getScheduleId() != null
                ? scheduleRepository.findById(req.getScheduleId()).orElse(null)
                : null;

        var session = Session.builder()
                .schedule(schedule)
                .sessionDate(req.getSessionDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .venueName(req.getVenueName())
                .status(SessionStatus.UPCOMING)
                .notes(req.getNotes())
                .createdBy(admin)
                .build();

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public SessionResponse updateSession(Long id, UpdateSessionRequest req) {
        var session = findById(id);
        if (req.getSessionDate() != null) session.setSessionDate(req.getSessionDate());
        if (req.getStartTime() != null) session.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) session.setEndTime(req.getEndTime());
        if (req.getVenueName() != null) session.setVenueName(req.getVenueName());
        if (req.getNotes() != null) session.setNotes(req.getNotes());
        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public SessionResponse updateStatus(Long id, SessionStatus status) {
        var session = findById(id);
        session.setStatus(status);
        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public void deleteSession(Long id) {
        var session = findById(id);
        sessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendeeResponse> getAttendees(Long sessionId) {
        return attendeeRepository.findAllBySessionId(sessionId).stream()
                .map(attendeeMapper::toResponse)
                .toList();
    }

    @Override
    public AttendeeResponse updateRsvp(Long sessionId, String userEmail, RsvpStatus rsvpStatus) {
        var session = findById(sessionId);
        var member = memberRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + userEmail));

        var attendee = attendeeRepository.findBySessionIdAndMemberId(sessionId, member.getId())
                .orElseGet(() -> SessionAttendee.builder()
                        .session(session)
                        .member(member)
                        .build());

        attendee.setRsvpStatus(rsvpStatus);
        return attendeeMapper.toResponse(attendeeRepository.save(attendee));
    }

    @Override
    public AttendeeResponse checkInMember(Long sessionId, Long memberId, boolean checkedIn) {
        var session = findById(sessionId);
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        var attendee = attendeeRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseGet(() -> SessionAttendee.builder()
                        .session(session)
                        .member(member)
                        .rsvpStatus(RsvpStatus.ATTENDING)
                        .build());

        attendee.setCheckedIn(checkedIn);
        return attendeeMapper.toResponse(attendeeRepository.save(attendee));
    }

    @Override
    public AttendeeResponse addGuestAttendee(Long sessionId, String fullName) {
        var session = findById(sessionId);
        var guest = Member.builder()
                .fullName(fullName)
                .role(Role.GUEST)
                .status(MemberStatus.ACTIVE)
                .email(null)
                .password(null)
                .balance(java.math.BigDecimal.ZERO)
                .joinedDate(LocalDate.now())
                .build();
        var savedGuest = memberRepository.save(guest);

        var attendee = SessionAttendee.builder()
                .session(session)
                .member(savedGuest)
                .rsvpStatus(RsvpStatus.ATTENDING)
                .checkedIn(true)
                .build();
        return attendeeMapper.toResponse(attendeeRepository.save(attendee));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long sessionId) {
        return taskRepository.findAllBySessionId(sessionId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Override
    public TaskResponse createTask(Long sessionId, CreateTaskRequest req) {
        var session = findById(sessionId);
        var assignedMember = req.getAssignedToId() != null
                ? memberRepository.findById(req.getAssignedToId()).orElse(null)
                : null;

        var task = SessionTask.builder()
                .session(session)
                .title(req.getTitle())
                .assignedTo(assignedMember)
                .isDone(false)
                .build();

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest req) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionTask", taskId));

        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getIsDone() != null) task.setIsDone(req.getIsDone());
        if (req.getAssignedToId() != null) {
            var member = memberRepository.findById(req.getAssignedToId()).orElse(null);
            task.setAssignedTo(member);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public void deleteTask(Long taskId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionTask", taskId));
        taskRepository.delete(task);
    }

    private Session findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
    }
}
