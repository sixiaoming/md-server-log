package com.uuzu.log.repository;

import com.uuzu.common.pojo.CallBackRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by lixing on 2017/7/12.
 */
@Repository
public interface CallbackRequestRepository extends MongoRepository<CallBackRequest,String> {
}
