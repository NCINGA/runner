package com.ncinga.runner.services;

import com.ncinga.runner.dtos.JobInfo;
import reactor.core.publisher.Mono;

public interface JobRunnerService {
    Mono<JobInfo> run(JobInfo jobInfo);

    Mono<JobInfo> submitAndRun(JobInfo jobInfo);

    public Mono<JobInfo> execute(JobInfo jobInfo);
}
