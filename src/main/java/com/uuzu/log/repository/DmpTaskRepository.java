package com.uuzu.log.repository;

import com.uuzu.common.pojo.DmpTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Created by Administrator on 2017/6/3.
 */
@Repository
public interface DmpTaskRepository extends MongoRepository<DmpTask,String> {



    @Query("{'busiserialno':?0}")
    DmpTask findByBusiserialno(String busiserialno);
}
