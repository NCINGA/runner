package com.ncinga.runner.controllers;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.services.JobNotifier;
import com.ncinga.runner.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobSubmitController {
    private final JobService jobService;
    private final JobNotifier jobNotifier;

    @PostMapping("/execute")
    public Mono<JobInfo> execute(@RequestBody JobInfo jobInfo) {
        return jobService.execute(jobInfo);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<JobInfo> streamJobEvents() {
        return jobNotifier.getEvents();
    }
}

