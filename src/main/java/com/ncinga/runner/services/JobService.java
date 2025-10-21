package com.ncinga.runner.services;

import com.ncinga.runner.dtos.JobInfo;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface JobService {
    Mono<JobInfo> submit(MultipartFile file, JobInfo jobInfo, String joId);
    Mono<Map<String, Object>> runAllJobs();
    Mono<JobInfo> executeJob(JobInfo jobInfo);

}
