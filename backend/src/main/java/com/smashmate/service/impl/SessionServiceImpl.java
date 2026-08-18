package com.smashmate.service.impl;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.response.SessionResponse;
import com.smashmate.entity.Session;
import com.smashmate.entity.enums.SessionStatus;
import com.smashmate.exception.ResourceNotFoundException;
import com.smashmate.mapper.SessionMapper;
import com.smashmate.repository.MemberRepository;
import com.smashmate.repository.RecurringScheduleRepository;
import com.smashmate.repository.SessionRepository;
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
    private final SessionMapper sessionMapper;

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

    private Session findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
    }
}
