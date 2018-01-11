package com.uuzu.log.web;

import com.uuzu.log.domain.DmpServiceApiChart;
import com.uuzu.log.domain.DmpServiceMacApiChart;
import com.uuzu.log.domain.ServiceApiAndCallBackChart;
import com.uuzu.log.service.ServiceApiLogsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * Created by zhoujin on 2017/8/31.
 */
@Controller
@Api(description = "dmp接口调用日志统计")
@RequestMapping("logs")
public class DmpServiceApiLogsController {


    @Autowired
    private ServiceApiLogsService serviceApiLogsService;


    /**
     * 根据日期查询接口调用详情
     *
     * @param date
     * @return
     */
    @GetMapping("queryDmpServiceApiLogs/{date}/{api}/{user_id}")
    @ApiOperation(value = "dmp接口", notes = "实时统计线上dsp接口日志数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "date", value = "查询日期", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "api", value = "api名称", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "user_id", value = "用户名", required = true, dataType = "String", paramType = "path")
    })
    public ResponseEntity<DmpServiceApiChart> queryServiceApiLogs(@PathVariable("date") String date,
                                                                  @PathVariable("user_id") String user_id,
                                                                  @PathVariable("api") String api,
                                                                  @PageableDefault(sort = {"creatTime"}, size = 500) Pageable pageable) {
        DmpServiceApiChart dmpServiceApiChart = null;

        try {
            if ("reqDeviceRelationMsg".equals(api) || "deviceRelationInfoPage".equals(api) || "idmapping".equals(api)) {
                dmpServiceApiChart = this.serviceApiLogsService.queryReqDeviceRelationMsgByDateAndName(date, user_id, api, pageable);
            } else {
                dmpServiceApiChart = this.serviceApiLogsService.queryChinaDmpServiceApiLogsByDateAndName(date, user_id, api, pageable);
            }


            if (!dmpServiceApiChart.getChinaDmpServiceApiLogs().getContent().isEmpty()) {
                return ResponseEntity.ok(dmpServiceApiChart);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(dmpServiceApiChart);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dmpServiceApiChart);
    }


    /**
     * 根据日期查询mac投户外接口
     *
     * @param date
     * @return
     */
    @GetMapping("queryDmpMacServiceApiLogs/{date}")
    @ApiOperation(value = "dmp接口", notes = "实时统计线上dsp接口日志数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "date", value = "查询日期", required = true, dataType = "String", paramType = "path")
    })
    public ResponseEntity<DmpServiceMacApiChart> queryServiceApiLogs(@PathVariable("date") String date,
                                                                     @PageableDefault(sort = {"creatTime"}, size = 24) Pageable pageable) {
        DmpServiceMacApiChart dmpServiceMacApiChart = null;

        try {
            dmpServiceMacApiChart = this.serviceApiLogsService.queryMacServcieLogByDate(date, pageable);


            if (!dmpServiceMacApiChart.getDmpTasks().getContent().isEmpty()) {
                return ResponseEntity.ok(dmpServiceMacApiChart);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(dmpServiceMacApiChart);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dmpServiceMacApiChart);
    }


}
