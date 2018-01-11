package com.uuzu.log.web;

import com.uuzu.log.domain.ServiceApiAndCallBackChart;
import com.uuzu.log.domain.ServiceApiLogs;
import com.uuzu.log.repository.ServiceApiLogsRepository;
import com.uuzu.log.service.ServiceApiLogsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


/**
 * Created by lixing on 2017/5/22.
 */
@Controller
@Api(description = "接口调用日志统计")
@RequestMapping("logs")
public class ServiceApiLogsController {


    @Autowired
    private ServiceApiLogsService serviceApiLogsService;



    /**
     * 根据日期查询接口调用详情
     * @param date
     * @return
     */
    @GetMapping("queryServiceApiLogs/{date}")
    @ApiOperation(value = "广告投放日志统计" , notes = "实时统计线上dsp接口日志数据")
    @ApiImplicitParam(name = "date", value = "查询日期", required = true, dataType = "String", paramType = "path")
    public ResponseEntity<ServiceApiAndCallBackChart> queryServiceApiLogs(@PathVariable("date")String date,
                                                              @PageableDefault(sort = {"creatTime"}, size = 24) Pageable pageable){
        StringBuilder builder = new StringBuilder("com.uuzu.wisemedia.web.DmpController.tagverify");

        ServiceApiAndCallBackChart serviceApiAndCallBackChart = null;

        try {
            serviceApiAndCallBackChart = this.serviceApiLogsService.queryServiceApiLogsByDate(date, pageable);


            if( !serviceApiAndCallBackChart.getServiceApiLogs().getContent().isEmpty()){
               return ResponseEntity.ok(serviceApiAndCallBackChart);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceApiAndCallBackChart);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(serviceApiAndCallBackChart);
    }


}
