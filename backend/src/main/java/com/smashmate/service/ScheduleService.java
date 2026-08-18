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
