package com.smashmate.service;

import com.smashmate.dto.request.CreateScheduleRequest;
import com.smashmate.dto.request.UpdateScheduleRequest;
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

    @Test
    void toggleActive_togglesScheduleState() {
        var req = new CreateScheduleRequest();
        req.setDayOfWeek(4);
        req.setStartTime(LocalTime.of(19, 0));
        req.setEndTime(LocalTime.of(21, 0));
        var created = scheduleService.createSchedule(req);

        var toggled = scheduleService.toggleActive(created.getId());
        assertThat(toggled.getIsActive()).isFalse();
    }

    @Test
    void updateSchedule_updatesVenueAndTimes() {
        var req = new CreateScheduleRequest();
        req.setDayOfWeek(6);
        req.setStartTime(LocalTime.of(8, 0));
        req.setEndTime(LocalTime.of(10, 0));
        var created = scheduleService.createSchedule(req);

        var uReq = new UpdateScheduleRequest();
        uReq.setVenueName("San Cau Long New Venue");
        var updated = scheduleService.updateSchedule(created.getId(), uReq);
        assertThat(updated.getVenueName()).isEqualTo("San Cau Long New Venue");
    }
}
