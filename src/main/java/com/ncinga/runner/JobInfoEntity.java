package com.ncinga.runner;


import com.ncinga.runner.enums.JobStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@Document("job_info")
public class JobInfoEntity {
    @Id
    private String id;
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
    private String result;
    private String className;
    private String method;
    private Map<String, String> prams;
}
