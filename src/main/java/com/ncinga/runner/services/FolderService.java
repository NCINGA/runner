package com.ncinga.runner.services;

import reactor.core.publisher.Mono;

/**
 * @author shehan.salinda@ncinga.net
 * @date 2025-10-17
 * This interface use for create folder on host matching. *
 * @params fileName use for creating dir if file name exist ignore file creation
 */
public interface FolderService {
    Mono<Boolean> createFolder(String fileName);
}

