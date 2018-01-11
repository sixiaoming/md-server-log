package com.uuzu.log.service.httpapi;

import org.springframework.stereotype.Component;


/**
 * Created by lixing on 2017/6/20.
 * 断路器
 */
@Component
public class CostApiServiceHystrix implements CostApiService {

    @Override
    public String queryPriceByCostID(String costid) {
        return null;
    }

    @Override
    public String queryByCostID(String id) {
        return null;
    }
}
