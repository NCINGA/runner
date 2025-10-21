package com.ncinga.runner.services.impl;

import com.ncinga.runner.services.FolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;


@Service
@Slf4j
public class FolderServiceImpl implements FolderService {
    @Override
    public Mono<Boolean> createFolder(String fileName) {
        return Mono.fromCallable(() -> {
            File theDir = new File(fileName);
            if (!theDir.exists()) {
                return theDir.mkdirs();
            }
            return true;
            //each  task run on parallel thread
        }).subscribeOn(Schedulers.boundedElastic());

    }
}
