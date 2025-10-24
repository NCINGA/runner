package com.ncinga.runner.services.impl;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.services.JobNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class JobNotifierImpl implements JobNotifier {
    private final Sinks.Many<JobInfo> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void notify(JobInfo job) {
        sink.tryEmitNext(job);
    }

    @Override
    public Flux<JobInfo> getEvents() {
        return sink.asFlux();
    }
}
