package com.ncinga.runner.services;

import com.ncinga.runner.dtos.JobInfo;
import reactor.core.publisher.Mono;

/**
 * @author shehan.salinda@ncinga.net
 * @date 2025-10-17
 * This interface use to run job method invoking JobInfo metadata type
 * @params JobInfo metadata job details
 * @return Mono job detail (execute id, statue ...etc)
 * @throw
 *
 */
public interface JobService {
    Mono<JobInfo> execute(JobInfo jobInfo);
}
