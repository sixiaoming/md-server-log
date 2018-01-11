package com.uuzu.log.repository;

import com.uuzu.common.pojo.CallBackRequest;
import com.uuzu.common.pojo.UserPrice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Created by zhoujin on 2017/09/18.
 */
@Repository
public interface UserPriceRepository extends MongoRepository<UserPrice,String> {


    @Query("{'cost_id':?0}")
    UserPrice findByCostID(String cost_id);
}
