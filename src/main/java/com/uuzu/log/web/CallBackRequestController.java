package com.uuzu.log.web;

import com.uuzu.common.pojo.CallBackRequest;
import com.uuzu.log.service.CallbackRequestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by lixing on 2017/7/12.
 */
@Slf4j
@Api(description = "MOB 广告数据回流")
@Controller
@RequestMapping("dmpask")
public class CallBackRequestController {


    @Autowired
    private CallbackRequestService callbackRequestService;

    /**
     *  供广告商实时在线反馈投放回调数据
     * @param strategyId
     * @param eventType
     * @param userId
     * @return
     */
    @GetMapping("callbackOnline")
    @ApiOperation(value = "广告投放回调接口(实时在线)" , notes = "实时记录投放后的事件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "s", value = "策略id", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "e", value = "事件类型(1:曝光、2:点击、3:转化、4:激活(key point)、5:有效（key point）、6:付费)", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "i", value = "设备号", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "u", value = "用户id", required = true, dataType = "String", paramType = "query")
    })
    public ResponseEntity<String> callbackApiOnline(@RequestParam("s") String strategyId,
                                                    @RequestParam("e") Integer eventType,
                                                    @RequestParam("i") String imei,
                                                    @RequestParam("u") String userId){
        try {
            CallBackRequest callBackRequest = this.callbackRequestService.saveCallBackRequest(strategyId, eventType, imei, userId);
            if(null != callBackRequest){
                return ResponseEntity.ok("{\"code\": \"1\"}");
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("{\"code\": \"0\"}");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"code\": \"0\"}");

    }

}
