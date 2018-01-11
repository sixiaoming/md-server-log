package com.uuzu.log.repository;

import com.uuzu.common.pojo.ServiceApiLog;
import com.uuzu.log.domain.ChinaDmpServiceApiLogs;
import com.uuzu.log.domain.ServiceApiLogs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by zhoujin on 2017/7/28.
 */
@Repository
public interface ChinaDmpServiceApiLogsRepository extends MongoRepository<ServiceApiLog,String>{
}
