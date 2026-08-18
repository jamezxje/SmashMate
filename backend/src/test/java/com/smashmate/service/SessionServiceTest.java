package com.smashmate.service;

import com.smashmate.dto.request.CreateSessionRequest;
import com.smashmate.dto.request.UpdateSessionRequest;
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

    @Test
    void updateStatus_changesSessionStatus() {
        var req = new CreateSessionRequest();
        req.setSessionDate(LocalDate.now().plusDays(2));
        req.setStartTime(LocalTime.of(19, 0));
        var created = sessionService.createSession("admin@smashmate.local", req);

        var updated = sessionService.updateStatus(created.getId(), SessionStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void updateSession_updatesVenueAndNotes() {
        var req = new CreateSessionRequest();
        req.setSessionDate(LocalDate.now().plusDays(3));
        req.setStartTime(LocalTime.of(20, 0));
        var created = sessionService.createSession("admin@smashmate.local", req);

        var uReq = new UpdateSessionRequest();
        uReq.setVenueName("San Cau Long Moi");
        uReq.setNotes("Dem theo cau moi");
        var updated = sessionService.updateSession(created.getId(), uReq);
        assertThat(updated.getVenueName()).isEqualTo("San Cau Long Moi");
        assertThat(updated.getNotes()).isEqualTo("Dem theo cau moi");
    }
}
