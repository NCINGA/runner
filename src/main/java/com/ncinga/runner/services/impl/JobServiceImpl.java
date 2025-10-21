package com.ncinga.runner.services.impl;

import com.ncinga.runner.JobInfoEntity;
import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.enums.JobStatus;
import com.ncinga.runner.repositories.JobInfoRepository;
import com.ncinga.runner.services.JobRunnerService;
import com.ncinga.runner.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobInfoRepository jobInfoRepository;
    private final JobRunnerService jobRunnerService;
    @Value("${application.job.path}")
    private String jobPath;

    @Override
    public Mono<JobInfo> submit(MultipartFile file, JobInfo jobInfo, String jobId) {
        return Mono.fromCallable(() -> {
                    String uploadDir = jobPath + "/" + jobInfo.getClient();
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Path filePath = uploadPath.resolve(file.getOriginalFilename());
                    file.transferTo(filePath.toFile());
                    jobInfo.setSubmitAt(Instant.now());
                    jobInfo.setJobId(jobId);
                    jobInfo.setPath(filePath.toAbsolutePath().toString());

                    return jobInfo;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(updatedJobInfo ->
                        jobInfoRepository.save(updatedJobInfo.toEntity(JobInfoEntity.class))
                                .map(saved -> {
                                    log.info("Job description: {}", updatedJobInfo);
                                    return JobInfo.fromEntity(saved, JobInfo.class);
                                })
                )
                .flatMap(savedJobInfo ->
                        jobRunnerService.submitAndRun(savedJobInfo)
                                .map(executedJobInfo -> {
                                    log.info("Running : {}", executedJobInfo);
                                    return executedJobInfo;
                                })
                                .onErrorResume(error -> {
                                    log.error("Job execution failed: {}", error.getMessage());
                                    return Mono.just(savedJobInfo);
                                })
                )
                .onErrorResume(e -> {
                    log.error("Error while submitting job", e);
                    return Mono.error(new RuntimeException("Job submission failed: " + e.getMessage()));
                });
    }

//    @Scheduled(fixedRate = 3000)
    @Override
    public Mono<Map<String, Object>> runAllJobs() {
        return Mono.fromCallable(() -> {
            File rootFolder = new File(jobPath);
            if (!rootFolder.exists() || !rootFolder.isDirectory()) {
                log.warn("Invalid jobs root path: {}", jobPath);
                return Map.of("error", "Invalid jobs root path: " + jobPath);
            }
            List<String> groovyFiles = scanGroovyFiles(rootFolder);
            groovyFiles.forEach(job -> {
                try {
                    JobInfo newJob = validateExecution(job);
                    if (newJob.getStatus().equals(JobStatus.ACTIVE)) {
                        newJob.setStartedAt(Instant.now());
                        log.info("Job running info {}", newJob.toString());
                        jobRunnerService.run(newJob);
                    } else if (newJob.getStatus().equals(JobStatus.STOP)) {
                        log.info("Job stopped {}", newJob.toString());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });

            return Map.of(
                    "status", "success",
                    "count", groovyFiles.size(),
                    "files", groovyFiles
            );
        });
    }

    @Override
    public Mono<JobInfo> executeJob(JobInfo jobInfo) {
        return jobRunnerService.execute(jobInfo);
    }


    private JobInfo validateExecution(String jobPath) throws IOException {
        String deploymentPath = new File(jobPath).getParent();
        File deploymentYML = new File(deploymentPath, "deployment.yml");
        if (!deploymentYML.exists()) {
            throw new FileNotFoundException("deployment.yml not found at " + deploymentYML.getAbsolutePath());
        }

        log.error("File found: {}", deploymentYML.getAbsolutePath());
        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(deploymentYML.toPath())) {
            Map<String, Object> config = yaml.load(inputStream);
            if (config.get("status").equals("active")) {
                JobInfo newJob = new JobInfo();
                newJob.setPath(jobPath);
                newJob.setStatus(JobStatus.ACTIVE);
                newJob.setStartedAt(Instant.now());

                return newJob;
            } else if (config.get("status").equals("stop")) {
                JobInfo newJob = new JobInfo();
                newJob.setPath(jobPath);
                newJob.setStatus(JobStatus.STOP);
                newJob.setStartedAt(Instant.now());
                return newJob;
            }
            return new JobInfo();
        }
    }

    private List<String> scanGroovyFiles(File folder) {
        List<String> result = new ArrayList<>();
        File[] filesAndDirs = folder.listFiles();

        if (filesAndDirs != null) {
            for (File f : filesAndDirs) {
                if (f.isDirectory()) {
                    result.addAll(scanGroovyFiles(f));
                } else if (f.isFile() && f.getName().endsWith(".groovy")) {
                    result.add(f.getAbsolutePath());
                }
            }
        }

        return result;
    }
}
