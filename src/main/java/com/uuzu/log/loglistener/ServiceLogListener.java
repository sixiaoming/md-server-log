package com.uuzu.log.loglistener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uuzu.log.domain.ServiceApiLogs;
import com.uuzu.common.redis.RedisLocker;
import com.uuzu.log.repository.ServiceApiLogsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lixing on 2017/4/14.
 */
@Service
@Slf4j
@RefreshScope
public class ServiceLogListener {

    private static final ObjectMapper OBJECT_MAPPER= new ObjectMapper();
    private static final String LOCK_NAME = "dmpWiseMediaLock";

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private RedisLocker redisLocker;
    @Autowired
    private ServiceApiLogsRepository serviceApiLogsRepository;

    /**
     * 接口计数器
     */
    @Value("${serverlog.counternumber}")
    private int COUNTERNUMBER;
    private static AtomicInteger APICALLNUMBERCOUNTER = new AtomicInteger(0);
    private static AtomicInteger APICALLSUCCESSNUMBERCOUNTER = new AtomicInteger(0);
    private static AtomicInteger APIHITNUMBERCOUNTER = new AtomicInteger(0);
    private static AtomicInteger TAGSHITNUMBERCOUNTER = new AtomicInteger(0);


    /**
     * 对日志进行消费，统计接口调用次数，成功率，命中率，投放率等重要信息
     * @param logMessage
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = "dmpWiseMedia.log", type = ExchangeTypes.FANOUT),
            value = @Queue(value = "apilog")))
    public synchronized void serviceServerLog(String logMessage) throws Exception {

            try {
                DateTime thisTime = new DateTime();
                // 日志信息截取
                boolean flag = true;
                JsonNode logMessageNode = OBJECT_MAPPER.readTree(logMessage);
                String message = String.valueOf(logMessageNode.get("message"));

                // 统计广告投放接口


                if(StringUtils.isNotBlank(message)) {
                    ServiceApiLogs apiLogs;

                    JsonNode messageNode = OBJECT_MAPPER.readTree(message);
                    String url = "";
                    JsonNode jUrl = messageNode.get("url");
                    JsonNode controller = messageNode.get("controller");

                    String apiName = null;
                    if(null != jUrl){
                        url = jUrl.asText();

                        if(StringUtils.equals(controller.asText(),"com.uuzu.wisemedia.web.DmpController.tagverify")){
                            JsonNode paremeter = messageNode.get("paremeter");
                            JsonNode paremeterJson = OBJECT_MAPPER.readTree(paremeter.get(0).toString());
                            JsonNode dataRange = paremeterJson.get("dataRange");
                            apiName = controller.asText() +"-"+dataRange.asText();
                        }else {
                            apiName = controller.asText();
                            flag = false;
                        }
                    }else {
                        JsonNode strategyId = messageNode.get("strategyId");
                        if(!StringUtils.equals(strategyId.asText(),"null")){
                            apiName = controller.asText() +"-"+strategyId.asText();
                        }else {
                            apiName = controller.asText();
                            flag = false;
                        }

                    }
                    String time = logMessageNode.get("time").asText(); //用于根据小时来统计


                    //消费请求报文日志

                    String[] times = StringUtils.splitByWholeSeparator(time, ":");
                    if(flag && StringUtils.isNotBlank(url) && !StringUtils.equals(url,"")){

                        if(APICALLNUMBERCOUNTER.get() >= COUNTERNUMBER){
                            apiLogs = serviceApiLogsRepository.findOne(apiName + "-" + times[0]);
                            if(null == apiLogs){
                                //不存在，插入
                                apiLogs = new ServiceApiLogs(apiName+"-"+times[0],apiName,APICALLNUMBERCOUNTER.incrementAndGet());
                                doNumberCounter(apiLogs,APICALLNUMBERCOUNTER);
                            }else {
                                //存在，更新
                                long apiCallNumber = apiLogs.getApiCallNumber();
                                apiLogs.setApiCallNumber(apiCallNumber+APICALLNUMBERCOUNTER.incrementAndGet());
                                apiLogs.setUpdateTime(thisTime);
                                doNumberCounter(apiLogs,APICALLNUMBERCOUNTER);
                            }
                        } else {
                            //只做计数器累加
                            APICALLNUMBERCOUNTER.getAndIncrement();
                        }



                    } else {
                        boolean elseFalg = false;
                        //回执报文日志处理
                        JsonNode responseParemeter = messageNode.get("responseParemeter");
                        JsonNode executionTime = messageNode.get("executionTime");
                        JsonNode statusCodeValue = null;
                        if(null != responseParemeter){
                            statusCodeValue = responseParemeter.get("statusCodeValue");
                            elseFalg = true;
                        }



                        //数据miss和命中并且响应时间小于30ms -> 问询成功量
                        if(null != statusCodeValue && flag && (statusCodeValue.asInt() == 403 || statusCodeValue.asInt() == 200)){

                            if(executionTime.asInt() <= 30){
                                if(APICALLSUCCESSNUMBERCOUNTER.get() >= COUNTERNUMBER){
                                    apiLogs = serviceApiLogsRepository.findOne(apiName + "-" + times[0]);
                                    long apiCallSuccessNumber = apiLogs.getApiCallSuccessNumber();
                                    if(null == apiLogs){
                                        apiLogs = new ServiceApiLogs(apiName+"-"+times[0],apiName);
                                    }
                                    apiLogs.setApiCallSuccessNumber(apiCallSuccessNumber+APICALLSUCCESSNUMBERCOUNTER.incrementAndGet());
                                    apiLogs.setUpdateTime(thisTime);
                                    doNumberCounter(apiLogs,APICALLSUCCESSNUMBERCOUNTER);
                                } else {
                                    APICALLSUCCESSNUMBERCOUNTER.getAndIncrement();
                                }

                            }


                            if(statusCodeValue.asInt() == 200){

                                if(APIHITNUMBERCOUNTER.get() >= COUNTERNUMBER){
                                    apiLogs = serviceApiLogsRepository.findOne(apiName + "-" + times[0]);
                                    if(null == apiLogs){
                                        apiLogs = new ServiceApiLogs(apiName+"-"+times[0],apiName);
                                    }
                                    long apiHitNumber = apiLogs.getApiHitNumber();
                                    apiLogs.setApiHitNumber(apiHitNumber+APIHITNUMBERCOUNTER.incrementAndGet());
                                    apiLogs.setUpdateTime(thisTime);
                                    doNumberCounter(apiLogs,APIHITNUMBERCOUNTER);
                                } else {
                                    APIHITNUMBERCOUNTER.getAndIncrement();
                                }



                                // 判断响应时间小于30ms并且dataRange=1
                                JsonNode body = responseParemeter.get("body");
                                JsonNode dataRange = body.get("dataRange");
                                if(executionTime.asInt() <= 30 && dataRange.asInt() == 1){
                                    if(TAGSHITNUMBERCOUNTER.get() >= COUNTERNUMBER){
                                        apiLogs = serviceApiLogsRepository.findOne(apiName + "-" + times[0]);
                                        if(null == apiLogs){
                                            apiLogs = new ServiceApiLogs(apiName+"-"+times[0],apiName);
                                        }
                                        long tagsHitNumber = apiLogs.getTagsHitNumber();
                                        apiLogs.setTagsHitNumber(tagsHitNumber+TAGSHITNUMBERCOUNTER.incrementAndGet());
                                        apiLogs.setUpdateTime(thisTime);
                                        doNumberCounter(apiLogs,TAGSHITNUMBERCOUNTER);
                                    } else {
                                        TAGSHITNUMBERCOUNTER.getAndIncrement();
                                    }

                                }


                            }

                        } else {
                            if(elseFalg){
                                //除了广告投放接口，其余接口只记录成功调用量
                                apiLogs = serviceApiLogsRepository.findOne(apiName + "-" + times[0]);
                                if(null == apiLogs){
                                    apiLogs = new ServiceApiLogs(apiName+"-"+times[0],apiName);
                                }
                                long apiCallSuccessNumber = apiLogs.getApiCallSuccessNumber();
                                apiLogs.setUpdateTime(thisTime);
                                apiLogs.setApiCallSuccessNumber(++ apiCallSuccessNumber);
                                doNumberCounter(apiLogs,new AtomicInteger());
                            }
                        }

                    }
                }

            } catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
            }finally {
                taskExecutor.execute(()-> log.info(logMessage));
            }



    }

    /**
     * 分布式插入锁
     * @param serviceApiLogs
     * @param numberCounter
     * @throws Exception
     */
    private void doNumberCounter(ServiceApiLogs serviceApiLogs, AtomicInteger numberCounter) throws Exception {
        redisLocker.lock(LOCK_NAME,() ->{

            this.serviceApiLogsRepository.save(serviceApiLogs);
            numberCounter.set(0);
            return null;
        });
    }


    /**
     * 路由日志消费，记录接口调用次数
     * @param logMessage
     */
    /*@RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = "serverRouting.log", type = ExchangeTypes.FANOUT),
            value = @Queue))
    public void routingServerLog(String logMessage){







    }*/


}
