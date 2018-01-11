package com.uuzu.log.repository;

import com.uuzu.log.domain.ServiceApiLogs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * Created by lixing on 2017/5/3.
 */
@Repository
public interface ServiceApiLogsRepository extends MongoRepository<ServiceApiLogs,String>{
}
