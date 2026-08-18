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
