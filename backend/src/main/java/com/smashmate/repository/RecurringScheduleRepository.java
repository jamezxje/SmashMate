package com.smashmate.repository;

import com.smashmate.entity.RecurringSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, Long> {
    List<RecurringSchedule> findAllByIsActiveTrue();
}
