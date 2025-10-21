package com.ncinga.runner.services.impl;

import com.ncinga.runner.dtos.JobInfo;
import com.ncinga.runner.enums.JobStatus;
import com.ncinga.runner.services.JobRunnerService;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@Data
public class JobRunnerServiceImpl implements JobRunnerService {

    @Value("${application.job.path}")
    private String jobPath;

    private GroovyScriptEngine groovyScriptEngine = null;
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("groovy-job-executor-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public Mono<JobInfo> run(JobInfo jobInfo) {
        jobInfo.setStatus(JobStatus.RUNNING);
        jobInfo.setStartedAt(Instant.now());
        CompletableFuture.runAsync(() -> executeGroovyScript(jobInfo), executorService);
        return Mono.just(jobInfo);
    }

    @Override
    public Mono<JobInfo> execute(JobInfo jobInfo) {
        return Mono.defer(() -> {
            jobInfo.setStatus(JobStatus.RUNNING);
            jobInfo.setStartedAt(Instant.now());

            return Mono.fromCallable(() -> {
                        String jobId = jobInfo.getClient() + "-" + Instant.now();
                        File scriptPath = new File(jobPath + "/" + jobInfo.getClient());
                        jobInfo.setJobId(jobId);
                        jobInfo.setPath(scriptPath.getPath());

                        if (!scriptPath.exists()) {
                            return fail(jobInfo, "File path not found: " + scriptPath);
                        }

                        File[] files = scriptPath.listFiles();
                        if (files == null || files.length == 0) {
                            return fail(jobInfo, "Executable files not found: " + scriptPath);
                        }

                        File deploymentFile = Arrays.stream(files)
                                .filter(f -> f.getName().endsWith("deployment.yml"))
                                .findFirst()
                                .orElse(null);

                        if (deploymentFile == null) {
                            return fail(jobInfo, "deployment.yml not found in: " + scriptPath);
                        }

                        String deploymentStatus;
                        String groovyFileName;

                        try (InputStream inputStream = new FileInputStream(deploymentFile)) {
                            Yaml yaml = new Yaml();
                            Map<String, Object> config = yaml.load(inputStream);
                            deploymentStatus = (String) config.get("status");
                            groovyFileName = (String) config.get("file-name");
                            log.info("Deployment config - status: {}, file-name: {}", deploymentStatus, groovyFileName);
                        } catch (Exception e) {
                            log.error("Failed to parse deployment.yml: {}", deploymentFile.getPath(), e);
                            return fail(jobInfo, "Failed to parse deployment.yml: " + e.getMessage());
                        }


                        if (!"active".equalsIgnoreCase(deploymentStatus)) {
                            jobInfo.setStatus(JobStatus.SKIPPED);
                            jobInfo.setErrorMessage("Job is inactive (status: " + deploymentStatus + ")");
                            log.info("Job skipped - status is: {}", deploymentStatus);
                            return jobInfo;
                        }

                        File groovyFile = Arrays.stream(files)
                                .filter(f -> f.getName().equals(groovyFileName))
                                .findFirst()
                                .orElse(null);

                        if (groovyFile == null) {
                            return fail(jobInfo, "Groovy file not found: " + groovyFileName);
                        }

                        try (GroovyClassLoader gcl = new GroovyClassLoader()) {
                            Class<?> groovyClass = gcl.parseClass(groovyFile);
                            GroovyObject groovyObject = (GroovyObject) groovyClass.getDeclaredConstructor().newInstance();

                            Object[] args = jobInfo.getPrams() != null && !jobInfo.getPrams().isEmpty()
                                    ? jobInfo.getPrams().values().toArray()
                                    : new Object[]{};

                            log.info("Invoking Groovy method: {}.{} with args={}",
                                    jobInfo.getClassName(), jobInfo.getMethod(), Arrays.toString(args));

                            Object result = groovyObject.invokeMethod(jobInfo.getMethod(), args);

                            jobInfo.setStatus(JobStatus.COMPLETED);
                            jobInfo.setCompletedAt(Instant.now());
                            if (result != null) {
                                jobInfo.setResult(result);
                                log.info("Groovy method result: {}", result);
                            }
                            return jobInfo;

                        } catch (Exception e) {
                            log.error("Groovy method execution failed: {}", groovyFile.getPath(), e);
                            return fail(jobInfo, "Groovy method execution failed: " + e.getMessage());
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSubscribe(sub -> log.info("Job started: {}", jobInfo.getJobId()))
                    .doOnSuccess(j -> log.info("Job completed with status: {}", jobInfo.getStatus()))
                    .doOnError(err -> log.error("Job failed: {}", jobInfo.getJobId(), err));
        });
    }

    private JobInfo fail(JobInfo jobInfo, String message) {
        jobInfo.setStatus(JobStatus.FAILED);
        jobInfo.setErrorMessage(message);
        jobInfo.setCompletedAt(Instant.now());
        log.error(message);
        return jobInfo;
    }


    private void executeGroovyScript(JobInfo jobInfo) {
        try {
            File scriptFile = new File(jobInfo.getPath());
            if (!scriptFile.exists()) {
                handleScriptError(jobInfo, "Script file not found: " + jobInfo.getPath());
                return;
            }

            log.info("Starting Groovy script execution using GroovyScriptEngine: {}", jobInfo.getPath());

            GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(scriptFile.getParentFile().getAbsolutePath());

            Binding binding = new Binding();
            Map<String, Object> vars = createScriptBinding(jobInfo);
            vars.forEach(binding::setVariable);
            Object result = groovyScriptEngine.run(scriptFile.getName(), binding);

            jobInfo.setStatus(JobStatus.COMPLETED);
            jobInfo.setCompletedAt(Instant.now());

            if (result != null) {
                jobInfo.setResult(result.toString());
                log.info("Groovy script result: {}", result);
            }

            log.info("Groovy script executed successfully: {}", jobInfo.getPath());

        } catch (IOException e) {
            log.error("Failed to read or load script file: {}", jobInfo.getPath(), e);
            handleScriptError(jobInfo, "Failed to load script file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Groovy script execution failed: {}", jobInfo.getPath(), e);
            handleScriptError(jobInfo, "Script execution failed: " + e.getMessage());
        }
    }


    private void handleScriptError(JobInfo jobInfo, String errorMessage) {
        jobInfo.setStatus(JobStatus.FAILED);
        jobInfo.setErrorMessage(errorMessage);
        jobInfo.setCompletedAt(Instant.now());
        // jobInfoRepository.save(convertToEntity(jobInfo)).subscribe();
    }

    public CompletableFuture<JobInfo> runAsync(JobInfo jobInfo) {
        jobInfo.setStatus(JobStatus.RUNNING);
        jobInfo.setStartedAt(Instant.now());

        return CompletableFuture.supplyAsync(() -> {
            executeGroovyScript(jobInfo);
            return jobInfo;
        }, executorService);
    }

    public Mono<JobInfo> submitAndRun(JobInfo jobInfo) {
        if (!validateScript(jobInfo.getPath())) {
            jobInfo.setStatus(JobStatus.FAILED);
            jobInfo.setErrorMessage("Script validation failed");
            return Mono.just(jobInfo);
        }
        jobInfo.setStatus(JobStatus.SUBMITTED);
        jobInfo.setSubmitAt(Instant.now());
        run(jobInfo);
        return Mono.just(jobInfo);
    }

    private Map<String, Object> createScriptBinding(JobInfo jobInfo) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("jobInfo", jobInfo);
        binding.put("jobId", jobInfo.getJobId());
        binding.put("client", jobInfo.getClient());
        binding.put("jobPath", jobPath);
        binding.put("logger", log);
        binding.put("now", Instant.now());
        return binding;
    }

    public Mono<JobInfo> runWithTimeout(JobInfo jobInfo, long timeoutSeconds) {
        jobInfo.setStatus(JobStatus.RUNNING);
        jobInfo.setStartedAt(Instant.now());

        CompletableFuture<JobInfo> future = CompletableFuture.supplyAsync(() -> {
            executeGroovyScript(jobInfo);
            return jobInfo;
        }, executorService);

        return Mono.fromFuture(future)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(e -> {
                    jobInfo.setStatus(JobStatus.FAILED);
                    jobInfo.setErrorMessage("Script execution timed out after " + timeoutSeconds + " seconds");
                    jobInfo.setCompletedAt(Instant.now());
                    log.error("Script execution timed out: {}", jobInfo.getPath());
                    return Mono.just(jobInfo);
                });
    }

    public boolean validateScript(String scriptPath) {
        try {
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                return false;
            }

            String scriptContent = Files.readString(scriptFile.toPath());
            return true;

        } catch (Exception e) {
            log.error("Script validation failed: {}", scriptPath, e);
            return false;
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
