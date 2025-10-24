package com.ncinga.runner.controllers;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.dtos.ResponseCode;
import com.ncinga.runner.dtos.ResponseMessage;
import com.ncinga.runner.services.JobNotifier;
import com.ncinga.runner.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobSubmitController {
    private final JobService jobService;
    private final JobNotifier jobNotifier;

    @PostMapping("/execute")
    public Mono<ResponseMessage> execute(@RequestBody JobInfo jobInfo) {
        return jobService.execute(jobInfo)
                .map(job -> ResponseMessage.getInstance(ResponseCode.EXECUTE_REQUEST_SUCCESS, job, null))
                .onErrorResume(error -> {
                    log.error("Job execution failed", error);
                    return Mono.just(ResponseMessage.getInstance(
                            ResponseCode.EXECUTE_REQUEST_FAILED,
                            null,
                            Map.of(
                                    "error", error.getClass().getSimpleName(),
                                    "message", error.getMessage()
                            )
                    ));
                });
    }


    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<JobInfo> streamJobEvents() {
        return jobNotifier.getEvents();
    }
}

