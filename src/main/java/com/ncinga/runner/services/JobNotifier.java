package com.ncinga.runner.services;

import com.ncinga.runner.dtos.JobInfo;
import reactor.core.publisher.Flux;

/**
 * This interface is using to emit realtime log
 */
public interface JobNotifier {
    void notify(JobInfo job);
    Flux<JobInfo> getEvents();
}
