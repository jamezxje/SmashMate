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
