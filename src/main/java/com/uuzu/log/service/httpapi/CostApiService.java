package com.uuzu.log.service.httpapi;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Created by lixing on 2017/6/20.
 */
@FeignClient(name = "MOBUSER" , fallback = CostApiServiceHystrix.class)
@Component
public interface CostApiService {

    @RequestMapping(method = RequestMethod.POST, path = "openApi/cost/queryPriceByCostID")
    String queryPriceByCostID(@RequestParam(value = "costid") String costid);

    @RequestMapping(method = RequestMethod.POST, path = "openApi/cost/queryByCostID")
    String queryByCostID(@RequestParam(value = "id") String id);
}
