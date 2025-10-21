package com.ncinga.runner.controllers;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobSubmitController {
    private final JobService jobService;

    @PostMapping("/execute")
    public Mono<JobInfo> execute(@RequestBody JobInfo jobInfo) {
        return jobService.execute(jobInfo);
    }
}

