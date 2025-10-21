package com.ncinga.runner.dtos;

import com.ncinga.runner.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobInfo {
    private String client;
    private JobStatus status;
    private Instant createAt = Instant.now();
    private Instant startedAt;
    private Instant submitAt;
    private String extension;
    private String jobId;
    private String path;
    private String errorMessage;
    private Instant completedAt;
    private Object result;
    private String className;
    private String method;
    private Map<String, String> prams;
}
