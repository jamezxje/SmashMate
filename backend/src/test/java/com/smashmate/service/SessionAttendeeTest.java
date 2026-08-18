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

    @Test
    void addGuestAttendee_createsGuestAndChecksIn() {
        var req = new CreateSessionRequest();
        req.setSessionDate(LocalDate.now().plusDays(3));
        req.setStartTime(LocalTime.of(18, 0));
        var session = sessionService.createSession("admin@smashmate.local", req);

        var guestAttendee = sessionService.addGuestAttendee(session.getId(), "Khach Vang Lai A");
        assertThat(guestAttendee.getMemberName()).isEqualTo("Khach Vang Lai A");
        assertThat(guestAttendee.getRsvpStatus()).isEqualTo(RsvpStatus.ATTENDING);
        assertThat(guestAttendee.getCheckedIn()).isTrue();
    }
}
