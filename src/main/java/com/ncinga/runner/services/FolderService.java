package com.ncinga.runner.services;

import reactor.core.publisher.Mono;

public interface FolderService {
    Mono<Boolean> createFolder(String jobId);
}

