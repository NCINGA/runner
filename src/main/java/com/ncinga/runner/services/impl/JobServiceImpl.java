package com.ncinga.runner.services.impl;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.enums.JobStatus;
import com.ncinga.runner.services.JobNotifier;
import com.ncinga.runner.services.JobService;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;


@Slf4j
@Data

/**
 * @params jobPath file path for job dir
 */

@Service
public class JobServiceImpl implements JobService {
    private final JobNotifier notifier;

    @Value("${application.job.path}")
    private String jobPath;


    @Override
    public Mono<JobInfo> execute(JobInfo jobInfo) {
        return Mono.defer(() -> {

            jobInfo.setStatus(JobStatus.RUNNING);
            jobInfo.setStartedAt(Instant.now());
            notifier.notify(jobInfo);
            return Mono.fromCallable(() -> {
                        String jobId = jobInfo.getClient() + "-" + Instant.now();
                        File scriptPath = new File(jobPath + "/" + jobInfo.getClient());
                        jobInfo.setJobId(jobId);
                        jobInfo.setPath(scriptPath.getPath());
                        notifier.notify(jobInfo);
                        log.info("Job [{}] - Path: {}", jobId, scriptPath.getAbsolutePath());
                        if (!scriptPath.exists()) {
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "File path not found: " + scriptPath);
                        }
                        File[] files = scriptPath.listFiles();
                        if (files == null || files.length == 0) {
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "Executable files not found in: " + scriptPath);
                        }
                        File deploymentFile = Arrays.stream(files)
                                .filter(f -> f.getName().endsWith("deployment.yml"))
                                .findFirst()
                                .orElse(null);

                        if (deploymentFile == null) {
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "deployment.yml not found in: " + scriptPath);
                        }

                        String deploymentStatus;
                        String groovyFileName;
                        try (InputStream inputStream = new FileInputStream(deploymentFile)) {
                            Yaml yaml = new Yaml();
                            Map<String, Object> config = yaml.load(inputStream);
                            deploymentStatus = (String) config.get("status");
                            groovyFileName = (String) config.get("file-name");
                            log.info("Deployment config -> status: {}, file-name: {}", deploymentStatus, groovyFileName);
                        } catch (Exception e) {
                            log.error("Failed to parse deployment.yml: {}", deploymentFile.getPath(), e);
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "Failed to parse deployment.yml: " + e.getMessage());
                        }

                        if (!"active".equalsIgnoreCase(deploymentStatus)) {
                            jobInfo.setStatus(JobStatus.SKIPPED);
                            jobInfo.setErrorMessage("Job is inactive (status: " + deploymentStatus + ")");
                            log.info("Job [{}] skipped (inactive)", jobId);
                            notifier.notify(jobInfo);
                            return jobInfo;
                        }

                        File groovyFile = Arrays.stream(files)
                                .filter(f -> f.getName().equals(groovyFileName))
                                .findFirst()
                                .orElse(null);

                        if (groovyFile == null) {
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "Groovy file not found: " + groovyFileName);
                        }
                        File libDir = new File("libs");
                        File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
                        URL[] urls = new URL[jarFiles == null ? 0 : jarFiles.length];
                        if (jarFiles != null) {
                            for (int i = 0; i < jarFiles.length; i++) {
                                urls[i] = jarFiles[i].toURI().toURL();
                            }
                        }
                        try (
                                URLClassLoader jarClassLoader = new URLClassLoader(
                                        urls,
                                        Thread.currentThread().getContextClassLoader()
                                );
                                GroovyClassLoader groovyClassLoader = new GroovyClassLoader(jarClassLoader)
                        ) {
                            Class<?> groovyClass = groovyClassLoader.parseClass(groovyFile);
                            GroovyObject groovyObject = (GroovyObject) groovyClass.getDeclaredConstructor().newInstance();

                            Object[] args = jobInfo.getPrams() != null && !jobInfo.getPrams().isEmpty()
                                    ? jobInfo.getPrams().values().toArray()
                                    : new Object[]{};

                            log.info("Invoking {}.{} with args={}",
                                    jobInfo.getClassName(), jobInfo.getMethod(), Arrays.toString(args));

                            Object result = groovyObject.invokeMethod(jobInfo.getMethod(), args);

                            jobInfo.setStatus(JobStatus.COMPLETED);
                            jobInfo.setCompletedAt(Instant.now());
                            if (result != null) {
                                jobInfo.setResult(result);
                                notifier.notify(jobInfo);
                                log.info("Groovy method result: {}", result);
                            }
                            return jobInfo;

                        } catch (Exception e) {
                            log.error("Groovy method execution failed: {}", groovyFile.getPath(), e);
                            notifier.notify(jobInfo);
                            return fail(jobInfo, "Groovy method execution failed: " + e.getMessage());
                        }

                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSubscribe(sub -> {
                        log.info("Job started: {}", jobInfo.getJobId());
                        notifier.notify(jobInfo);
                    })
                    .doOnSuccess(j -> {
                        log.info("Job completed with status: {}", jobInfo.getStatus());
                        notifier.notify(jobInfo);
                    })
                    .doOnError(err -> {
                        log.error("Job failed: {}", jobInfo.getJobId(), err);
                        notifier.notify(jobInfo);
                    });
        });
    }


    private JobInfo fail(JobInfo jobInfo, String message) {
        jobInfo.setStatus(JobStatus.FAILED);
        jobInfo.setErrorMessage(message);
        jobInfo.setCompletedAt(Instant.now());
        log.error(message);
        return jobInfo;
    }

}
