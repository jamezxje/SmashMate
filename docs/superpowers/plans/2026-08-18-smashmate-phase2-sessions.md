# Phase 2: Sessions & Scheduling Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Session & Scheduling subsystem (Recurring Schedules, Session Creation, Attendance & RSVP, Session Task Checklist) for both Backend (Spring Boot 3.3.2) and Frontend (React 18 + Tailwind CSS).

**Architecture:** Extend Spring Boot backend with JPA entities, repositories, service interfaces (`ScheduleService`, `SessionService`), implementations in `service.impl`, MapStruct mappers, and REST controllers. On frontend, build `schedules.ts` & `sessions.ts` API clients, Zustand state, and React pages (`SchedulesPage`, `SessionsPage`, `SessionDetailPage`).

**Tech Stack:** Java 21, Spring Boot 3.3.2, Spring Security (JWT, `@PreAuthorize`), JPA/Hibernate, Flyway, MapStruct, Lombok, React 18, TypeScript, Vite, Tailwind CSS, Lucide React, Axios, Zustand.

**Spec:** [docs/superpowers/specs/2026-08-18-smashmate-design.md](file:///g:/DuyTX/SmashMate/docs/superpowers/specs/2026-08-18-smashmate-design.md) (Module 2)

## Global Constraints

- Backend package structure: `com.smashmate.{config, controller, dto, entity, exception, mapper, repository, security, service, service.impl}`
- Service layer convention: Interface in `com.smashmate.service`, implementation in `com.smashmate.service.impl`
- REST URL prefix: `/api/v1`
- Money fields: `BigDecimal` in Java, `DECIMAL(12,2)` in MySQL, formatted with `formatVND` on frontend
- Timestamps: `LocalDateTime` (UTC) and `LocalDate`
- Response envelope: `ApiResponse<T>` with `{ success, data, message, error, timestamp }`
- Authorization: `@PreAuthorize("hasRole('ADMIN')")` on Admin-only endpoints
- Commit convention: Conventional Commits in English (`feat(session): ...`, `test(session): ...`)

---

## Component Decomposition & Task Plan

| Task | Subsystem / Component | Key Deliverables |
|---|---|---|
| **Task 1** | Backend Recurring Schedules | `RecurringSchedule` entity, `ScheduleRepository`, `ScheduleService` & `Impl`, `ScheduleController`, `ScheduleServiceTest` |
| **Task 2** | Backend Core Sessions | `Session` entity, `SessionStatus` enum, `SessionRepository`, `SessionService` & `Impl`, `SessionController`, `SessionServiceTest` |
| **Task 3** | Backend Attendance & RSVP | `SessionAttendee` entity, `RsvpStatus` enum, `SessionAttendeeRepository`, RSVP/Check-in service methods, endpoints, integration tests |
| **Task 4** | Backend Session Task Checklist | `SessionTask` entity, `SessionTaskRepository`, Task service methods, task endpoints, integration tests |
| **Task 5** | Frontend Schedules Management UI | `schedules.ts` API, `SchedulesPage`, `ScheduleFormModal`, Routing |
| **Task 6** | Frontend Sessions List UI | `sessions.ts` API, `SessionsPage`, `SessionFormModal`, Status badges, Filtering |
| **Task 7** | Frontend Session Detail & RSVP UI | `SessionDetailPage`, RSVP status control, Check-in toggle list, Task checklist component |
| **Task 8** | Phase 2 E2E Final Verification | Maven clean test, Frontend build, End-to-end integration demo |

---

### Task 1: Backend Recurring Schedules Module

**Files:**
- Create: `backend/src/main/java/com/smashmate/entity/RecurringSchedule.java`
- Create: `backend/src/main/java/com/smashmate/repository/RecurringScheduleRepository.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/CreateScheduleRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/UpdateScheduleRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/ScheduleResponse.java`
- Create: `backend/src/main/java/com/smashmate/mapper/ScheduleMapper.java`
- Create: `backend/src/main/java/com/smashmate/service/ScheduleService.java`
- Create: `backend/src/main/java/com/smashmate/service/impl/ScheduleServiceImpl.java`
- Create: `backend/src/main/java/com/smashmate/controller/ScheduleController.java`
- Create: `backend/src/test/java/com/smashmate/service/ScheduleServiceTest.java`

**Interfaces:**
- Consumes: `ApiResponse<T>`, `SecurityConfig`
- Produces:
  - `ScheduleService.getAllSchedules() -> List<ScheduleResponse>`
  - `ScheduleService.createSchedule(CreateScheduleRequest) -> ScheduleResponse`
  - `ScheduleService.updateSchedule(Long, UpdateScheduleRequest) -> ScheduleResponse`
  - `ScheduleService.toggleActive(Long) -> ScheduleResponse`
  - `ScheduleService.deleteSchedule(Long)`
  - Endpoints under `/api/v1/schedules`

- [x] **Step 1: Create `RecurringSchedule.java` entity**

```java
package com.smashmate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "recurring_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false, columnDefinition = "TINYINT")
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

- [x] **Step 2: Create `RecurringScheduleRepository.java`**

```java
package com.smashmate.repository;

import com.smashmate.entity.RecurringSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, Long> {
    List<RecurringSchedule> findAllByIsActiveTrue();
}
```

- [x] **Step 3: Create DTOs & `ScheduleMapper.java`**

`CreateScheduleRequest.java`:
```java
package com.smashmate.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class CreateScheduleRequest {
    @NotNull @Min(1) @Max(7)
    private Integer dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String venueName;
}
```

`UpdateScheduleRequest.java`:
```java
package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateScheduleRequest {
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
}
```

`ScheduleResponse.java`:
```java
package com.smashmate.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class ScheduleResponse {
    private Long id;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
```

`ScheduleMapper.java`:
```java
package com.smashmate.mapper;

import com.smashmate.dto.response.ScheduleResponse;
import com.smashmate.entity.RecurringSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    ScheduleResponse toResponse(RecurringSchedule schedule);
}
```

- [x] **Step 4: Create `ScheduleService.java` & `ScheduleServiceImpl.java`**

`ScheduleService.java`:
```java
package com.smashmate.service;

import com.smashmate.dto.request.CreateScheduleRequest;
import com.smashmate.dto.request.UpdateScheduleRequest;
import com.smashmate.dto.response.ScheduleResponse;

import java.util.List;

public interface ScheduleService {
    List<ScheduleResponse> getAllSchedules();
    ScheduleResponse createSchedule(CreateScheduleRequest req);
    ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest req);
    ScheduleResponse toggleActive(Long id);
    void deleteSchedule(Long id);
}
```

`ScheduleServiceImpl.java`:
```java
package com.smashmate.service.impl;

import com.smashmate.dto.request.CreateScheduleRequest;
import com.smashmate.dto.request.UpdateScheduleRequest;
import com.smashmate.dto.response.ScheduleResponse;
import com.smashmate.entity.RecurringSchedule;
import com.smashmate.exception.ResourceNotFoundException;
import com.smashmate.mapper.ScheduleMapper;
import com.smashmate.repository.RecurringScheduleRepository;
import com.smashmate.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final RecurringScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream()
            .map(scheduleMapper::toResponse)
            .toList();
    }

    @Override
    public ScheduleResponse createSchedule(CreateScheduleRequest req) {
        var schedule = RecurringSchedule.builder()
            .dayOfWeek(req.getDayOfWeek())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .venueName(req.getVenueName())
            .isActive(true)
            .build();
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest req) {
        var schedule = findById(id);
        if (req.getDayOfWeek() != null) schedule.setDayOfWeek(req.getDayOfWeek());
        if (req.getStartTime() != null) schedule.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) schedule.setEndTime(req.getEndTime());
        if (req.getVenueName() != null) schedule.setVenueName(req.getVenueName());
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    public ScheduleResponse toggleActive(Long id) {
        var schedule = findById(id);
        schedule.setIsActive(!schedule.getIsActive());
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    public void deleteSchedule(Long id) {
        var schedule = findById(id);
        scheduleRepository.delete(schedule);
    }

    private RecurringSchedule findById(Long id) {
        return scheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RecurringSchedule", id));
    }
}
```

- [x] **Step 5: Create `ScheduleController.java`**

```java
package com.smashmate.controller;

import com.smashmate.common.ApiResponse;
import com.smashmate.dto.request.CreateScheduleRequest;
import com.smashmate.dto.request.UpdateScheduleRequest;
import com.smashmate.dto.response.ScheduleResponse;
import com.smashmate.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getAllSchedules()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(@Valid @RequestBody CreateScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(scheduleService.createSchedule(req), "Schedule created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> update(
            @PathVariable Long id, @RequestBody UpdateScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.updateSchedule(id, req), "Schedule updated"));
    }

    @PATCHMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.toggleActive(id), "Schedule status toggled"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Schedule deleted"));
    }
}
```

- [x] **Step 6: Write unit tests for ScheduleService**

`backend/src/test/java/com/smashmate/service/ScheduleServiceTest.java`:
```java
package com.smashmate.service;

import com.smashmate.dto.request.CreateScheduleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Test
    void createSchedule_validData_createsSuccessfully() {
        var req = new CreateScheduleRequest();
        req.setDayOfWeek(2); // Tuesday
        req.setStartTime(LocalTime.of(18, 0));
        req.setEndTime(LocalTime.of(20, 0));
        req.setVenueName("San Cau Long ABC");

        var created = scheduleService.createSchedule(req);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getDayOfWeek()).isEqualTo(2);
        assertThat(created.getIsActive()).isTrue();
    }
}
```

- [x] **Step 7: Run backend test suite**

Run: `mvn test -Dtest=ScheduleServiceTest`
Expected: PASS

- [x] **Step 8: Commit Task 1**

```bash
git add backend/
git commit -m "feat(schedule): implement RecurringSchedule entity, service, controller, and tests"
```

---

### Task 2: Backend Core Sessions Module

**Files:**
- Create: `backend/src/main/java/com/smashmate/entity/enums/SessionStatus.java`
- Create: `backend/src/main/java/com/smashmate/entity/Session.java`
- Create: `backend/src/main/java/com/smashmate/repository/SessionRepository.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/CreateSessionRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/UpdateSessionRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/SessionResponse.java`
- Create: `backend/src/main/java/com/smashmate/mapper/SessionMapper.java`
- Create: `backend/src/main/java/com/smashmate/service/SessionService.java`
- Create: `backend/src/main/java/com/smashmate/service/impl/SessionServiceImpl.java`
- Create: `backend/src/main/java/com/smashmate/controller/SessionController.java`
- Create: `backend/src/test/java/com/smashmate/service/SessionServiceTest.java`

**Interfaces:**
- Consumes: `MemberRepository`, `RecurringScheduleRepository`
- Produces:
  - `SessionService.getAllSessions(SessionStatus status, Integer month, Integer year) -> List<SessionResponse>`
  - `SessionService.getSessionById(Long) -> SessionResponse`
  - `SessionService.createSession(String adminEmail, CreateSessionRequest) -> SessionResponse`
  - `SessionService.updateSession(Long, UpdateSessionRequest) -> SessionResponse`
  - `SessionService.updateStatus(Long, SessionStatus) -> SessionResponse`
  - Endpoints under `/api/v1/sessions`

- [x] **Step 1: Create `SessionStatus.java` & `Session.java`**

`SessionStatus.java`:
```java
package com.smashmate.entity.enums;

public enum SessionStatus {
    UPCOMING,
    IN_PROGRESS,
    CLOSED,
    CANCELLED
}
```

`Session.java`:
```java
package com.smashmate.entity;

import com.smashmate.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private RecurringSchedule schedule;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.UPCOMING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

- [x] **Step 2: Create `SessionRepository.java`**

```java
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
```

- [x] **Step 3: Create DTOs & `SessionMapper.java`**

`CreateSessionRequest.java`:
```java
package com.smashmate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateSessionRequest {
    private Long scheduleId;

    @NotNull
    private LocalDate sessionDate;

    @NotNull
    private LocalTime startTime;

    private LocalTime endTime;
    private String venueName;
    private String notes;
}
```

`UpdateSessionRequest.java`:
```java
package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateSessionRequest {
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
    private String notes;
}
```

`SessionResponse.java`:
```java
package com.smashmate.dto.response;

import com.smashmate.entity.enums.SessionStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class SessionResponse {
    private Long id;
    private Long scheduleId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venueName;
    private SessionStatus status;
    private String notes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
```

`SessionMapper.java`:
```java
package com.smashmate.mapper;

import com.smashmate.dto.response.SessionResponse;
import com.smashmate.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    @Mapping(target = "scheduleId", source = "schedule.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    SessionResponse toResponse(Session session);
}
```

- [x] **Step 4: Create `SessionService.java` & `SessionServiceImpl.java`**

`SessionService.java`:
```java
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
```

`SessionServiceImpl.java`:
```java
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
```

- [x] **Step 5: Create `SessionController.java`**

```java
package com.smashmate.controller;

import com.smashmate.common.ApiResponse;
import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
import com.smashmate.dto.response.SessionResponse;
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
}
```

- [x] **Step 6: Write unit tests for SessionService**

`backend/src/test/java/com/smashmate/service/SessionServiceTest.java`:
```java
package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.entity.enums.SessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void createSession_asAdmin_createsUpcomingSession() {
        var req = new CreateSessionRequest();
        req.setSessionDate(LocalDate.now().plusDays(1));
        req.setStartTime(LocalTime.of(18, 0));
        req.setVenueName("San Cau Long Demo");

        var created = sessionService.createSession("admin@smashmate.local", req);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(SessionStatus.UPCOMING);
        assertThat(created.getCreatedByName()).isEqualTo("Admin");
    }
}
```

- [x] **Step 7: Run backend test suite**

Run: `mvn test -Dtest=SessionServiceTest`
Expected: PASS

- [x] **Step 8: Commit Task 2**

```bash
git add backend/
git commit -m "feat(session): implement Session entity, status lifecycle, service, controller, and tests"
```

---

### Task 3: Backend Attendance & RSVP Management

**Files:**
- Create: `backend/src/main/java/com/smashmate/entity/enums/RsvpStatus.java`
- Create: `backend/src/main/java/com/smashmate/entity/SessionAttendee.java`
- Create: `backend/src/main/java/com/smashmate/repository/SessionAttendeeRepository.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/UpdateRsvpRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/AttendeeResponse.java`
- Create: `backend/src/main/java/com/smashmate/mapper/AttendeeMapper.java`
- Modify: `backend/src/main/java/com/smashmate/service/SessionService.java`
- Modify: `backend/src/main/java/com/smashmate/service/impl/SessionServiceImpl.java`
- Modify: `backend/src/main/java/com/smashmate/controller/SessionController.java`
- Test: `backend/src/test/java/com/smashmate/service/SessionAttendeeTest.java`

**Interfaces:**
- Consumes: `SessionRepository`, `MemberRepository`
- Produces:
  - `SessionService.getAttendees(Long sessionId) -> List<AttendeeResponse>`
  - `SessionService.updateRsvp(Long sessionId, String userEmail, RsvpStatus rsvpStatus) -> AttendeeResponse`
  - `SessionService.checkInMember(Long sessionId, Long memberId, boolean checkedIn) -> AttendeeResponse`
  - Endpoints under `/api/v1/sessions/{id}/attendees` and `/api/v1/sessions/{id}/rsvp`

- [x] **Step 1: Create `RsvpStatus.java` & `SessionAttendee.java`**

`RsvpStatus.java`:
```java
package com.smashmate.entity.enums;

public enum RsvpStatus {
    ATTENDING,
    ABSENT,
    PENDING
}
```

`SessionAttendee.java`:
```java
package com.smashmate.entity;

import com.smashmate.entity.enums.RsvpStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_attendees", uniqueConstraints = {
    @UniqueConstraint(name = "uq_session_attendees", columnNames = {"session_id", "member_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false, length = 20)
    @Builder.Default
    private RsvpStatus rsvpStatus = RsvpStatus.PENDING;

    @Column(name = "checked_in", nullable = false)
    @Builder.Default
    private Boolean checkedIn = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

- [x] **Step 2: Create `SessionAttendeeRepository.java`**

```java
package com.smashmate.repository;

import com.smashmate.entity.SessionAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionAttendeeRepository extends JpaRepository<SessionAttendee, Long> {
    List<SessionAttendee> findAllBySessionId(Long sessionId);
    Optional<SessionAttendee> findBySessionIdAndMemberId(Long sessionId, Long memberId);
}
```

- [x] **Step 3: Create DTOs & `AttendeeMapper.java`**

`UpdateRsvpRequest.java`:
```java
package com.smashmate.dto.request;

import com.smashmate.entity.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRsvpRequest {
    @NotNull
    private RsvpStatus rsvpStatus;
}
```

`AttendeeResponse.java`:
```java
package com.smashmate.dto.response;

import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import com.smashmate.entity.enums.RsvpStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendeeResponse {
    private Long id;
    private Long sessionId;
    private Long memberId;
    private String memberName;
    private String memberPhone;
    private Role memberRole;
    private MemberStatus memberStatus;
    private RsvpStatus rsvpStatus;
    private Boolean checkedIn;
}
```

`AttendeeMapper.java`:
```java
package com.smashmate.mapper;

import com.smashmate.dto.response.AttendeeResponse;
import com.smashmate.entity.SessionAttendee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendeeMapper {
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "memberName", source = "member.fullName")
    @Mapping(target = "memberPhone", source = "member.phone")
    @Mapping(target = "memberRole", source = "member.role")
    @Mapping(target = "memberStatus", source = "member.status")
    AttendeeResponse toResponse(SessionAttendee attendee);
}
```

- [x] **Step 4: Update `SessionService.java` & `SessionServiceImpl.java`**

Add to `SessionService.java`:
```java
List<AttendeeResponse> getAttendees(Long sessionId);
AttendeeResponse updateRsvp(Long sessionId, String userEmail, RsvpStatus rsvpStatus);
AttendeeResponse checkInMember(Long sessionId, Long memberId, boolean checkedIn);
AttendeeResponse addGuestAttendee(Long sessionId, String fullName);
```

Implement in `SessionServiceImpl.java`:
```java
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
```

- [x] **Step 5: Add endpoints to `SessionController.java`**

```java
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
```

- [x] **Step 6: Write unit tests for Attendance & RSVP**

`backend/src/test/java/com/smashmate/service/SessionAttendeeTest.java`:
```java
package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.entity.enums.RsvpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SessionAttendeeTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void rsvpAndCheckIn_updatesAttendeeState() {
        var req = new CreateSessionRequest();
        req.setSessionDate(LocalDate.now().plusDays(2));
        req.setStartTime(LocalTime.of(19, 0));
        var session = sessionService.createSession("admin@smashmate.local", req);

        var rsvpResult = sessionService.updateRsvp(session.getId(), "admin@smashmate.local", RsvpStatus.ATTENDING);
        assertThat(rsvpResult.getRsvpStatus()).isEqualTo(RsvpStatus.ATTENDING);

        var checkInResult = sessionService.checkInMember(session.getId(), rsvpResult.getMemberId(), true);
        assertThat(checkInResult.getCheckedIn()).isTrue();
    }
}
```

- [x] **Step 7: Run backend test suite**

Run: `mvn test -Dtest=SessionAttendeeTest`
Expected: PASS

- [x] **Step 8: Commit Task 3**

```bash
git add backend/
git commit -m "feat(session): implement SessionAttendee entity, RSVP, and check-in endpoints"
```

---

### Task 4: Backend Session Task Checklist

**Files:**
- Create: `backend/src/main/java/com/smashmate/entity/SessionTask.java`
- Create: `backend/src/main/java/com/smashmate/repository/SessionTaskRepository.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/CreateTaskRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/UpdateTaskRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/TaskResponse.java`
- Create: `backend/src/main/java/com/smashmate/mapper/TaskMapper.java`
- Modify: `backend/src/main/java/com/smashmate/service/SessionService.java`
- Modify: `backend/src/main/java/com/smashmate/service/impl/SessionServiceImpl.java`
- Modify: `backend/src/main/java/com/smashmate/controller/SessionController.java`
- Test: `backend/src/test/java/com/smashmate/service/SessionTaskTest.java`

**Interfaces:**
- Consumes: `SessionRepository`, `MemberRepository`
- Produces:
  - `SessionService.getTasks(Long sessionId) -> List<TaskResponse>`
  - `SessionService.createTask(Long sessionId, CreateTaskRequest) -> TaskResponse`
  - `SessionService.updateTask(Long taskId, UpdateTaskRequest) -> TaskResponse`
  - `SessionService.deleteTask(Long taskId)`
  - Endpoints under `/api/v1/sessions/{id}/tasks`

- [x] **Step 1: Create `SessionTask.java` entity**

```java
package com.smashmate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Member assignedTo;

    @Column(name = "is_done", nullable = false)
    @Builder.Default
    private Boolean isDone = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

- [x] **Step 2: Create `SessionTaskRepository.java`**

```java
package com.smashmate.repository;

import com.smashmate.entity.SessionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionTaskRepository extends JpaRepository<SessionTask, Long> {
    List<SessionTask> findAllBySessionId(Long sessionId);
}
```

- [x] **Step 3: Create DTOs & `TaskMapper.java`**

`CreateTaskRequest.java`:
```java
package com.smashmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {
    @NotBlank
    private String title;
    private Long assignedToId;
}
```

`UpdateTaskRequest.java`:
```java
package com.smashmate.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskRequest {
    private String title;
    private Long assignedToId;
    private Boolean isDone;
}
```

`TaskResponse.java`:
```java
package com.smashmate.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private Long sessionId;
    private String title;
    private Long assignedToId;
    private String assignedToName;
    private Boolean isDone;
    private LocalDateTime createdAt;
}
```

`TaskMapper.java`:
```java
package com.smashmate.mapper;

import com.smashmate.dto.response.TaskResponse;
import com.smashmate.entity.SessionTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    @Mapping(target = "assignedToName", source = "assignedTo.fullName")
    TaskResponse toResponse(SessionTask task);
}
```

- [x] **Step 4: Update `SessionService.java` & `SessionServiceImpl.java`**

Add to `SessionService.java`:
```java
List<TaskResponse> getTasks(Long sessionId);
TaskResponse createTask(Long sessionId, CreateTaskRequest req);
TaskResponse updateTask(Long taskId, UpdateTaskRequest req);
void deleteTask(Long taskId);
```

Implement in `SessionServiceImpl.java`:
```java
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
```

- [x] **Step 5: Add endpoints to `SessionController.java`**

```java
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
```

- [x] **Step 6: Write unit tests for Session Tasks**

`backend/src/test/java/com/smashmate/service/SessionTaskTest.java`:
```java
package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.CreateTaskRequest;
import com.smashmate.dto.request.UpdateTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SessionTaskTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void createAndUpdateTask_togglesDoneStatus() {
        var sReq = new CreateSessionRequest();
        sReq.setSessionDate(LocalDate.now().plusDays(3));
        sReq.setStartTime(LocalTime.of(17, 30));
        var session = sessionService.createSession("admin@smashmate.local", sReq);

        var tReq = new CreateTaskRequest();
        tReq.setTitle("Dat san tap 1 va 2");
        var task = sessionService.createTask(session.getId(), tReq);
        assertThat(task.getIsDone()).isFalse();

        var uReq = new UpdateTaskRequest();
        uReq.setIsDone(true);
        var updated = sessionService.updateTask(task.getId(), uReq);
        assertThat(updated.getIsDone()).isTrue();
    }
}
```

- [x] **Step 7: Run backend test suite**

Run: `mvn test`
Expected: BUILD SUCCESS (All 15+ tests pass)

- [x] **Step 8: Commit Task 4**

```bash
git add backend/
git commit -m "feat(session): implement SessionTask entity, checklist CRUD, and tests"
```

---

### Task 5: Frontend Schedules Management UI

**Files:**
- Create: `frontend/src/api/schedules.ts`
- Create: `frontend/src/pages/SchedulesPage.tsx`
- Create: `frontend/src/components/schedules/ScheduleFormModal.tsx`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `api`, `useAuthStore`
- Produces: `/schedules` route for managing recurring badminton schedules

- [x] **Step 1: Update `frontend/src/types/index.ts`**

Add:
```typescript
export interface ScheduleResponse {
  id: number;
  dayOfWeek: number; // 1=Monday ... 7=Sunday
  startTime: string;
  endTime: string;
  venueName?: string;
  isActive: boolean;
  createdAt: string;
}
```

- [x] **Step 2: Create `frontend/src/api/schedules.ts`**

```typescript
import { api } from './axios';
import type { ApiResponse, ScheduleResponse } from '../types';

export const scheduleApi = {
  getAll: () =>
    api.get<ApiResponse<ScheduleResponse[]>>('/schedules'),
  create: (data: { dayOfWeek: number; startTime: string; endTime: string; venueName?: string }) =>
    api.post<ApiResponse<ScheduleResponse>>('/schedules', data),
  update: (id: number, data: { dayOfWeek?: number; startTime?: string; endTime?: string; venueName?: string }) =>
    api.put<ApiResponse<ScheduleResponse>>(`/schedules/${id}`, data),
  toggleActive: (id: number) =>
    api.patch<ApiResponse<ScheduleResponse>>(`/schedules/${id}/toggle`),
  delete: (id: number) =>
    api.delete<ApiResponse<void>>(`/schedules/${id}`),
};
```

- [x] **Step 3: Create `ScheduleFormModal.tsx`**

`frontend/src/components/schedules/ScheduleFormModal.tsx`:
```tsx
import React, { useState } from 'react';
import { scheduleApi } from '../../api/schedules';
import type { ScheduleResponse } from '../../types';
import { X, Calendar, Clock, MapPin } from 'lucide-react';

interface Props {
  schedule: ScheduleResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

const dayNames = [
  'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'
];

export function ScheduleFormModal({ schedule, onClose, onSaved }: Props) {
  const [dayOfWeek, setDayOfWeek] = useState(schedule?.dayOfWeek ?? 2);
  const [startTime, setStartTime] = useState(schedule?.startTime ?? '18:00');
  const [endTime, setEndTime] = useState(schedule?.endTime ?? '20:00');
  const [venueName, setVenueName] = useState(schedule?.venueName ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (schedule) {
        await scheduleApi.update(schedule.id, { dayOfWeek, startTime, endTime, venueName });
      } else {
        await scheduleApi.create({ dayOfWeek, startTime, endTime, venueName: venueName || undefined });
      }
      onSaved();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể lưu lịch sinh hoạt');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h2 className="text-lg font-bold text-white">
            {schedule ? 'Sửa lịch định kỳ' : 'Tạo lịch sinh hoạt định kỳ'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
              Thứ trong tuần *
            </label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
              >
                {dayNames.map((name, idx) => (
                  <option key={idx + 1} value={idx + 1}>{name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
                Giờ bắt đầu *
              </label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="time"
                  required
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
                Giờ kết thúc *
              </label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="time"
                  required
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
              Sân tập / Địa điểm
            </label>
            <div className="relative">
              <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={venueName}
                onChange={(e) => setVenueName(e.target.value)}
                placeholder="Sân Cầu Lông ABC - Sân số 2"
                className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          {error && <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs">{error}</div>}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 bg-slate-700 text-slate-200 py-2.5 rounded-xl text-sm">Hủy</button>
            <button type="submit" disabled={loading} className="flex-1 bg-emerald-500 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50">
              {loading ? 'Đang lưu...' : 'Lưu lịch tập'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [x] **Step 4: Create `SchedulesPage.tsx`**

`frontend/src/pages/SchedulesPage.tsx`:
```tsx
import { useEffect, useState } from 'react';
import { scheduleApi } from '../api/schedules';
import type { ScheduleResponse } from '../types';
import { ScheduleFormModal } from '../components/schedules/ScheduleFormModal';
import { useAuthStore } from '../stores/useAuthStore';
import { Calendar, Plus, ToggleLeft, ToggleRight, Trash2, Edit, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const dayNames = ['', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];

export function SchedulesPage() {
  const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<ScheduleResponse | null>(null);

  const { role } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      const { data } = await scheduleApi.getAll();
      setSchedules(data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleToggle(id: number) {
    await scheduleApi.toggleActive(id);
    load();
  }

  async function handleDelete(id: number) {
    if (!confirm('Bạn có muốn xóa lịch định kỳ này?')) return;
    await scheduleApi.delete(id);
    load();
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/sessions')} className="p-2 bg-slate-800 rounded-xl hover:bg-slate-700 text-slate-300">
              <ArrowLeft className="w-5 h-5" />
            </button>
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              <Calendar className="w-6 h-6 text-emerald-400" />
              Lịch tập định kỳ hàng tuần
            </h1>
          </div>

          {isAdmin && (
            <button
              onClick={() => { setEditTarget(null); setShowModal(true); }}
              className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs"
            >
              <Plus className="w-4 h-4" /> + Thêm lịch tập
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {loading ? (
            <p className="text-slate-400 col-span-2 text-center py-8">Đang tải lịch sinh hoạt...</p>
          ) : schedules.length === 0 ? (
            <p className="text-slate-400 col-span-2 text-center py-8">Chưa có lịch sinh hoạt định kỳ nào.</p>
          ) : (
            schedules.map((s) => (
              <div key={s.id} className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-lg flex justify-between items-center">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-extrabold text-emerald-400 text-lg">{dayNames[s.dayOfWeek]}</span>
                    <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${s.isActive ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-700 text-slate-400'}`}>
                      {s.isActive ? 'Bật' : 'Tắt'}
                    </span>
                  </div>
                  <p className="text-sm font-semibold text-white mt-1">{s.startTime} - {s.endTime}</p>
                  <p className="text-xs text-slate-400 mt-0.5">{s.venueName || 'Chưa xếp địa điểm'}</p>
                </div>

                {isAdmin && (
                  <div className="flex items-center gap-2">
                    <button title="Bật/Tắt lịch" onClick={() => handleToggle(s.id)} className="p-2 text-slate-400 hover:text-emerald-400">
                      {s.isActive ? <ToggleRight className="w-6 h-6 text-emerald-400" /> : <ToggleLeft className="w-6 h-6 text-slate-500" />}
                    </button>
                    <button title="Sửa" onClick={() => { setEditTarget(s); setShowModal(true); }} className="p-2 text-blue-400 hover:bg-blue-400/10 rounded-lg">
                      <Edit className="w-4 h-4" />
                    </button>
                    <button title="Xóa" onClick={() => handleDelete(s.id)} className="p-2 text-rose-400 hover:bg-rose-400/10 rounded-lg">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {showModal && (
        <ScheduleFormModal
          schedule={editTarget}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
```

- [x] **Step 5: Add `/schedules` route in `App.tsx`**

```tsx
import { SchedulesPage } from './pages/SchedulesPage';
// Add to Routes:
<Route path="/schedules" element={<ProtectedRoute><SchedulesPage /></ProtectedRoute>} />
```

- [x] **Step 6: Build frontend to verify compilation**

Run: `cd frontend && npm run build`
Expected: PASS (0 TypeScript errors)

- [x] **Step 7: Commit Task 5**

```bash
git add frontend/
git commit -m "feat(schedule): implement SchedulesPage, ScheduleFormModal, and scheduleApi"
```

---

### Task 6: Frontend Sessions List UI

**Files:**
- Create: `frontend/src/api/sessions.ts`
- Create: `frontend/src/pages/SessionsPage.tsx`
- Create: `frontend/src/components/sessions/SessionFormModal.tsx`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `api`, `useAuthStore`, `ScheduleResponse`
- Produces: `/sessions` list route with status badges and creation modal

- [x] **Step 1: Update `frontend/src/types/index.ts`**

Add:
```typescript
export type SessionStatus = 'UPCOMING' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED';
export type RsvpStatus = 'ATTENDING' | 'ABSENT' | 'PENDING';

export interface SessionResponse {
  id: number;
  scheduleId?: number;
  sessionDate: string;
  startTime: string;
  endTime?: string;
  venueName?: string;
  status: SessionStatus;
  notes?: string;
  createdById: number;
  createdByName: string;
  createdAt: string;
}
```

- [x] **Step 2: Create `frontend/src/api/sessions.ts`**

```typescript
import { api } from './axios';
import type { ApiResponse, SessionResponse, SessionStatus } from '../types';

export const sessionApi = {
  getAll: (status?: SessionStatus, month?: number, year?: number) =>
    api.get<ApiResponse<SessionResponse[]>>('/sessions', {
      params: { status, month, year }
    }),
  getById: (id: number) =>
    api.get<ApiResponse<SessionResponse>>(`/sessions/${id}`),
  create: (data: { scheduleId?: number; sessionDate: string; startTime: string; endTime?: string; venueName?: string; notes?: string }) =>
    api.post<ApiResponse<SessionResponse>>('/sessions', data),
  update: (id: number, data: { sessionDate?: string; startTime?: string; endTime?: string; venueName?: string; notes?: string }) =>
    api.put<ApiResponse<SessionResponse>>(`/sessions/${id}`, data),
  updateStatus: (id: number, status: SessionStatus) =>
    api.patch<ApiResponse<SessionResponse>>(`/sessions/${id}/status`, { status }),
  delete: (id: number) =>
    api.delete<ApiResponse<void>>(`/sessions/${id}`),
};
```

- [x] **Step 3: Create `SessionFormModal.tsx`**

`frontend/src/components/sessions/SessionFormModal.tsx`:
```tsx
import React, { useState } from 'react';
import { sessionApi } from '../../api/sessions';
import type { SessionResponse } from '../../types';
import { X, Calendar, Clock, MapPin, FileText } from 'lucide-react';

interface Props {
  session: SessionResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

export function SessionFormModal({ session, onClose, onSaved }: Props) {
  const [sessionDate, setSessionDate] = useState(session?.sessionDate ?? new Date().toISOString().split('T')[0]);
  const [startTime, setStartTime] = useState(session?.startTime ?? '18:00');
  const [endTime, setEndTime] = useState(session?.endTime ?? '20:00');
  const [venueName, setVenueName] = useState(session?.venueName ?? '');
  const [notes, setNotes] = useState(session?.notes ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (session) {
        await sessionApi.update(session.id, { sessionDate, startTime, endTime, venueName, notes });
      } else {
        await sessionApi.create({ sessionDate, startTime, endTime, venueName: venueName || undefined, notes: notes || undefined });
      }
      onSaved();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể lưu buổi sinh hoạt');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h2 className="text-lg font-bold text-white">{session ? 'Sửa buổi sinh hoạt' : 'Tạo buổi tập mới'}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Ngày sinh hoạt *</label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input type="date" required value={sessionDate} onChange={(e) => setSessionDate(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Giờ bắt đầu *</label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="time" required value={startTime} onChange={(e) => setStartTime(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Giờ kết thúc</label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Địa điểm / Sân tập</label>
            <div className="relative">
              <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input type="text" value={venueName} onChange={(e) => setVenueName(e.target.value)} placeholder="Sân số 3 - Cầu lông Thể Thao" className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Ghi chú</label>
            <div className="relative">
              <FileText className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} placeholder="Ghi chú buổi tập..." className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          {error && <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs">{error}</div>}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 bg-slate-700 text-slate-200 py-2.5 rounded-xl text-sm">Hủy</button>
            <button type="submit" disabled={loading} className="flex-1 bg-emerald-500 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50">
              {loading ? 'Đang lưu...' : 'Lưu buổi tập'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [x] **Step 4: Create `SessionsPage.tsx`**

`frontend/src/pages/SessionsPage.tsx`:
```tsx
import { useEffect, useState } from 'react';
import { sessionApi } from '../api/sessions';
import type { SessionResponse, SessionStatus } from '../types';
import { SessionFormModal } from '../components/sessions/SessionFormModal';
import { useAuthStore } from '../stores/useAuthStore';
import { Calendar, Plus, MapPin, Clock, ArrowRight, Shield, Users, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const statusBadges: Record<SessionStatus, string> = {
  UPCOMING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  IN_PROGRESS: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 animate-pulse',
  CLOSED: 'bg-slate-700 text-slate-400 border-slate-600',
  CANCELLED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
};

export function SessionsPage() {
  const [sessions, setSessions] = useState<SessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<SessionResponse | null>(null);

  const { role, logout } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      const { data } = await sessionApi.getAll();
      setSessions(data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
      <header className="bg-slate-800/80 border-b border-slate-700/80 sticky top-0 z-40 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center justify-center">
              <Shield className="w-5 h-5 text-emerald-400" />
            </div>
            <h1 className="font-extrabold text-lg text-white">SmashMate</h1>
          </div>

          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/members')} className="flex items-center gap-1 text-xs text-slate-300 hover:text-white px-3 py-1.5 rounded-lg bg-slate-700">
              <Users className="w-4 h-4" /> Thành viên
            </button>
            <button onClick={() => navigate('/schedules')} className="flex items-center gap-1 text-xs text-slate-300 hover:text-white px-3 py-1.5 rounded-lg bg-slate-700">
              <Calendar className="w-4 h-4" /> Lịch tập
            </button>
            <button onClick={() => { logout(); navigate('/login'); }} className="text-xs text-slate-400 hover:text-rose-400 p-1.5">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Calendar className="w-6 h-6 text-emerald-400" /> Danh sách buổi sinh hoạt
          </h1>
          {isAdmin && (
            <button onClick={() => { setEditTarget(null); setShowModal(true); }} className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs">
              <Plus className="w-4 h-4" /> + Tạo buổi tập
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {loading ? (
            <p className="text-slate-400 col-span-3 text-center py-12">Đang tải danh sách buổi tập...</p>
          ) : sessions.length === 0 ? (
            <p className="text-slate-400 col-span-3 text-center py-12">Chưa có buổi sinh hoạt nào được tạo.</p>
          ) : (
            sessions.map((s) => (
              <div key={s.id} onClick={() => navigate(`/sessions/${s.id}`)} className="bg-slate-800 border border-slate-700/80 hover:border-emerald-500/50 rounded-2xl p-5 shadow-xl transition-all cursor-pointer flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-extrabold text-white">{s.sessionDate}</span>
                    <span className={`px-2.5 py-0.5 rounded-full border text-[10px] font-bold ${statusBadges[s.status]}`}>
                      {s.status}
                    </span>
                  </div>

                  <div className="space-y-1.5 text-xs text-slate-300">
                    <div className="flex items-center gap-2">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{s.startTime} - {s.endTime || 'Chưa định'}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span>{s.venueName || 'Chưa chọn sân'}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-700/60 flex items-center justify-between text-xs text-emerald-400 font-semibold">
                  <span>Chi tiết & RSVP</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </div>
            ))
          )}
        </div>
      </main>

      {showModal && (
        <SessionFormModal
          session={editTarget}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
```

- [x] **Step 5: Add `/sessions` route to `App.tsx`**

```tsx
import { SessionsPage } from './pages/SessionsPage';
// Add to Routes:
<Route path="/sessions" element={<ProtectedRoute><SessionsPage /></ProtectedRoute>} />
```

- [x] **Step 6: Build frontend to verify compilation**

Run: `cd frontend && npm run build`
Expected: PASS (0 TypeScript errors)

- [x] **Step 7: Commit Task 6**

```bash
git add frontend/
git commit -m "feat(session): implement SessionsPage, SessionFormModal, and sessionApi"
```

---

### Task 7: Frontend Session Detail, RSVP & Checklist UI

**Files:**
- Create: `frontend/src/pages/SessionDetailPage.tsx`
- Modify: `frontend/src/api/sessions.ts`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `sessionApi`, `AttendeeResponse`, `TaskResponse`
- Produces: `/sessions/:id` detail route with RSVP buttons, Check-in toggle, and Session Task checklist

- [ ] **Step 1: Update `frontend/src/types/index.ts`**

Add:
```typescript
export interface SessionAttendeeResponse {
  id: number;
  sessionId: number;
  memberId: number;
  memberName: string;
  memberPhone?: string;
  memberRole: Role;
  memberStatus: MemberStatus;
  rsvpStatus: RsvpStatus;
  checkedIn: boolean;
}

export interface SessionTaskResponse {
  id: number;
  sessionId: number;
  title: string;
  assignedToId?: number;
  assignedToName?: string;
  isDone: boolean;
  createdAt: string;
}
```

- [ ] **Step 2: Add Attendee & Task API methods to `frontend/src/api/sessions.ts`**

Add:
```typescript
getAttendees: (sessionId: number) =>
  api.get<ApiResponse<SessionAttendeeResponse[]>>(`/sessions/${sessionId}/attendees`),
updateRsvp: (sessionId: number, rsvpStatus: RsvpStatus) =>
  api.patch<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/rsvp`, { rsvpStatus }),
checkInMember: (sessionId: number, memberId: number, checkedIn: boolean) =>
  api.patch<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/attendees/${memberId}/checkin`, { checkedIn }),
addGuestAttendee: (sessionId: number, fullName: string) =>
  api.post<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/attendees/guest`, { fullName }),

getTasks: (sessionId: number) =>
  api.get<ApiResponse<SessionTaskResponse[]>>(`/sessions/${sessionId}/tasks`),
createTask: (sessionId: number, title: string, assignedToId?: number) =>
  api.post<ApiResponse<SessionTaskResponse>>(`/sessions/${sessionId}/tasks`, { title, assignedToId }),
updateTask: (sessionId: number, taskId: number, isDone: boolean) =>
  api.patch<ApiResponse<SessionTaskResponse>>(`/sessions/${sessionId}/tasks/${taskId}`, { isDone }),
deleteTask: (sessionId: number, taskId: number) =>
  api.delete<ApiResponse<void>>(`/sessions/${sessionId}/tasks/${taskId}`),
```

- [ ] **Step 3: Create `SessionDetailPage.tsx`**

`frontend/src/pages/SessionDetailPage.tsx`:
```tsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { sessionApi } from '../api/sessions';
import type { SessionResponse, SessionAttendeeResponse, SessionTaskResponse, RsvpStatus } from '../types';
import { useAuthStore } from '../stores/useAuthStore';
import { ArrowLeft, Calendar, Clock, MapPin, CheckCircle, XCircle, HelpCircle, CheckSquare, Plus, Trash2, UserPlus } from 'lucide-react';

export function SessionDetailPage() {
  const { id } = useParams();
  const sessionId = Number(id);
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [attendees, setAttendees] = useState<SessionAttendeeResponse[]>([]);
  const [tasks, setTasks] = useState<SessionTaskResponse[]>([]);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [loading, setLoading] = useState(true);

  const { role } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function loadData() {
    setLoading(true);
    try {
      const [sRes, aRes, tRes] = await Promise.all([
        sessionApi.getById(sessionId),
        sessionApi.getAttendees(sessionId),
        sessionApi.getTasks(sessionId)
      ]);
      setSession(sRes.data.data);
      setAttendees(aRes.data.data);
      setTasks(tRes.data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (sessionId) loadData();
  }, [sessionId]);

  async function handleRsvp(status: RsvpStatus) {
    await sessionApi.updateRsvp(sessionId, status);
    loadData();
  }

  async function handleCheckIn(memberId: number, current: boolean) {
    await sessionApi.checkInMember(sessionId, memberId, !current);
    loadData();
  }

  async function handleAddGuestAttendee() {
    const name = prompt('Nhập tên khách vãng lai tham gia buổi tập:');
    if (name?.trim()) {
      await sessionApi.addGuestAttendee(sessionId, name.trim());
      loadData();
    }
  }

  async function handleCreateTask(e: React.FormEvent) {
    e.preventDefault();
    if (!newTaskTitle.trim()) return;
    await sessionApi.createTask(sessionId, newTaskTitle.trim());
    setNewTaskTitle('');
    loadData();
  }

  async function handleToggleTask(taskId: number, current: boolean) {
    await sessionApi.updateTask(sessionId, taskId, !current);
    loadData();
  }

  async function handleDeleteTask(taskId: number) {
    await sessionApi.deleteTask(sessionId, taskId);
    loadData();
  }

  if (loading || !session) {
    return <div className="min-h-screen bg-slate-900 text-slate-400 p-8 text-center">Đang tải chi tiết buổi tập...</div>;
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/sessions')} className="p-2 bg-slate-800 rounded-xl hover:bg-slate-700 text-slate-300">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white">Buổi sinh hoạt ngày {session.sessionDate}</h1>
            <p className="text-xs text-slate-400">{session.venueName || 'Chưa định địa điểm'} | {session.startTime} - {session.endTime || 'Chưa định'}</p>
          </div>
        </div>

        {/* RSVP Banner */}
        <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-white">Đăng ký tham gia (RSVP)</h3>
            <p className="text-xs text-slate-400">Xác nhận sự có mặt của bạn cho buổi tập này</p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => handleRsvp('ATTENDING')} className="flex items-center gap-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-bold px-4 py-2 rounded-xl text-xs">
              <CheckCircle className="w-4 h-4" /> Tham gia
            </button>
            <button onClick={() => handleRsvp('ABSENT')} className="flex items-center gap-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 font-bold px-4 py-2 rounded-xl text-xs">
              <XCircle className="w-4 h-4" /> Vắng mặt
            </button>
          </div>
        </div>

        {/* Two Column Layout: Attendees & Tasks */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Attendees List */}
          <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-white">Danh sách tham gia ({attendees.length})</h3>
              {isAdmin && (
                <button
                  onClick={handleAddGuestAttendee}
                  className="flex items-center gap-1 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 font-bold px-3 py-1.5 rounded-xl text-xs transition-colors"
                >
                  <UserPlus className="w-3.5 h-3.5" /> + Khách vãng lai
                </button>
              )}
            </div>
            <div className="space-y-2.5 max-h-96 overflow-y-auto pr-1">
              {attendees.map((a) => (
                <div key={a.id} className="flex items-center justify-between bg-slate-900/60 border border-slate-700/50 p-3 rounded-xl">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-xs font-semibold text-white">{a.memberName}</p>
                      {a.memberRole === 'GUEST' && (
                        <span className="text-[10px] font-bold px-1.5 py-0.2 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded">GUEST</span>
                      )}
                    </div>
                    <span className={`text-[10px] font-bold ${a.rsvpStatus === 'ATTENDING' ? 'text-emerald-400' : 'text-rose-400'}`}>
                      RSVP: {a.rsvpStatus}
                    </span>
                  </div>
                  {isAdmin && (
                    <button
                      onClick={() => handleCheckIn(a.memberId, a.checkedIn)}
                      className={`text-xs px-3 py-1 rounded-lg font-bold border transition-colors ${
                        a.checkedIn ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30' : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      {a.checkedIn ? '✓ Đã điểm danh' : 'Điểm danh'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Session Tasks */}
          <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl">
            <h3 className="text-base font-bold text-white mb-4">Checklist công việc</h3>

            {isAdmin && (
              <form onSubmit={handleCreateTask} className="flex gap-2 mb-4">
                <input
                  type="text"
                  value={newTaskTitle}
                  onChange={(e) => setNewTaskTitle(e.target.value)}
                  placeholder="Thêm việc (VD: Mua nước, Đặt sân)..."
                  className="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                />
                <button type="submit" className="bg-emerald-500 text-slate-950 font-bold px-3 py-2 rounded-xl text-xs flex items-center gap-1">
                  <Plus className="w-4 h-4" /> Thêm
                </button>
              </form>
            )}

            <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
              {tasks.map((t) => (
                <div key={t.id} className="flex items-center justify-between bg-slate-900/60 border border-slate-700/50 p-3 rounded-xl">
                  <div className="flex items-center gap-2.5">
                    <button onClick={() => isAdmin && handleToggleTask(t.id, t.isDone)} className="text-slate-400 hover:text-emerald-400">
                      <CheckSquare className={`w-5 h-5 ${t.isDone ? 'text-emerald-400' : 'text-slate-600'}`} />
                    </button>
                    <span className={`text-xs ${t.isDone ? 'line-through text-slate-500' : 'text-slate-200 font-medium'}`}>{t.title}</span>
                  </div>
                  {isAdmin && (
                    <button onClick={() => handleDeleteTask(t.id)} className="text-slate-500 hover:text-rose-400 p-1">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Add `/sessions/:id` route to `App.tsx`**

```tsx
import { SessionDetailPage } from './pages/SessionDetailPage';
// Add to Routes:
<Route path="/sessions/:id" element={<ProtectedRoute><SessionDetailPage /></ProtectedRoute>} />
```

- [ ] **Step 5: Build frontend to verify compilation**

Run: `cd frontend && npm run build`
Expected: PASS (0 TypeScript errors)

- [ ] **Step 6: Commit Task 7**

```bash
git add frontend/
git commit -m "feat(session): implement SessionDetailPage with RSVP buttons, check-in controls, and task checklist"
```

---

### Task 8: Phase 2 E2E Final Verification

- [ ] **Step 1: Run complete Maven test suite**

Run: `cd backend && mvn clean test`
Expected: BUILD SUCCESS (All backend unit & integration tests PASS)

- [ ] **Step 2: Run frontend production build**

Run: `cd frontend && npm run build`
Expected: PASS (0 TypeScript errors)

- [ ] **Step 3: Run end-to-end demo & verification**

Verify end-to-end functionality across Backend and Frontend on `http://localhost:5173`.

- [ ] **Step 4: Commit and push final Phase 2 deliverables**

```bash
git add .
git commit -m "chore(phase2): complete Phase 2 Sessions & Scheduling module"
git push origin develop
```

---

## Self-Review

### Spec Coverage Checklist

| Spec Requirement | Task |
|---|---|
| Recurring schedule table DDL & CRUD | Task 1 |
| Sessions table DDL & lifecycle | Task 2 |
| Session attendees table DDL, RSVP & Check-in | Task 3 |
| Session task checklist DDL & task CRUD | Task 4 |
| Frontend Schedules Page | Task 5 |
| Frontend Sessions Page & Status badges | Task 6 |
| Frontend Session Detail Page, RSVP & Checklist | Task 7 |
| E2E Verification & Commit to `develop` | Task 8 |

---
