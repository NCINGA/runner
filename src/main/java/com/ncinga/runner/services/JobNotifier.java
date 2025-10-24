package com.ncinga.runner.services;

import com.ncinga.runner.dtos.JobInfo;
import reactor.core.publisher.Flux;

public interface JobNotifier {
    void notify(JobInfo job);
    Flux<JobInfo> getEvents();
}
