package com.uuzu.log.service;

import com.uuzu.common.pojo.CallBackRequest;
import com.uuzu.log.repository.CallbackRequestRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lixing on 2017/7/12.
 */
@Service
public class CallbackRequestService {

    @Autowired
    private CallbackRequestRepository callbackRequestRepository;


    public CallBackRequest saveCallBackRequest(String strategyId, Integer eventType, String imei, String userId){
        DateTime dateTime = new DateTime();
        CallBackRequest callBackRequest = new CallBackRequest();
        CallBackRequest.MessageCallback messageCallback = new CallBackRequest.MessageCallback();

        callBackRequest.setImei(imei);
        callBackRequest.setStrategyId(strategyId);
        callBackRequest.setEventType(eventType);
        callBackRequest.setUserId(userId);
        callBackRequest.setId("callBack.message-"+dateTime.toString("yyyy-MM-dd HH:mm:ss:SSS"));

        messageCallback.setStrategyId(strategyId);
        messageCallback.setEventTime(dateTime.toString("yyyy-MM-dd HH:mm:ss:SSS"));
        callBackRequest.setMessageCallback(messageCallback);
        return this.callbackRequestRepository.save(callBackRequest);
    }

}
