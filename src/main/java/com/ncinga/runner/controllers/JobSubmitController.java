package com.ncinga.runner.controllers;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.enums.JobStatus;
//import com.ncinga.runner.services.ActiveMQProducerService;
import com.ncinga.runner.services.JobRunnerService;
import com.ncinga.runner.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")  // Match your cURL path
@RequiredArgsConstructor
@Slf4j
public class JobSubmitController {
    private final JobService jobService;

    @PostMapping(
            path = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<JobInfo>> submit(
            @RequestPart("file") MultipartFile file,
            @RequestPart("client") String client,
            @RequestPart("status") String status,
            @RequestPart("extension") String extension,
            @RequestPart("params") String params
    ) {
        log.info("Received file: {}, size: {}, client: {}, status: {}, extension: {} params:{}",
                file.getOriginalFilename(), file.getSize(), client, status, extension, params);

        try {
            if (file.isEmpty()) {
                log.error("File is empty");
                return Mono.just(ResponseEntity.badRequest().build());
            }
            JobInfo jobInfo = new JobInfo();
            jobInfo.setClient(client);
            jobInfo.setStatus(JobStatus.valueOf(status.toUpperCase()));
            jobInfo.setExtension(extension);
            log.info("Processing job with info: {}", jobInfo);
            String joId = UUID.randomUUID().toString();
            return jobService.submit(file, jobInfo, joId)
                    .map(result -> {
                        log.info("Job processed successfully: {}", result);
                        return ResponseEntity.ok(result);
                    })
                    .onErrorResume(error -> {
                        log.error("Error processing job: {}", error.getMessage(), error);
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });

        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status, e);
            return Mono.just(ResponseEntity.badRequest().build());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    @GetMapping("/all")
    public Mono<String> runJobs() {
        jobService.runAllJobs().map(result -> result).subscribe();
        return Mono.just("Start all jobs");
    }

    @PostMapping("/execute")
    public Mono<JobInfo> execute(@RequestBody JobInfo jobInfo) throws IOException {
        return jobService.executeJob(jobInfo);
    }
}

