package com.ncinga.runner.repositories;

import com.ncinga.runner.JobInfoEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobInfoRepository extends ReactiveMongoRepository<JobInfoEntity, String> {
}
