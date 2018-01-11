package com.uuzu.log.loglistener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.util.JSON;
import com.uuzu.common.pojo.ServiceApiLog;
import com.uuzu.common.redis.RedisLocker;
import com.uuzu.log.repository.ChinaDmpServiceApiLogsRepository;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * dmp项目，日志消息监听器
 * Created by zhoujin on 2017/7/28.
 */
@Service
@Slf4j
@RefreshScope
public class ChinaDmpServiceLogListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String LOCK_NAME = "chinaDmpLock";

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private RedisLocker redisLocker;
    @Autowired
    private ChinaDmpServiceApiLogsRepository chinaDmpServiceApiLogsRepository;

    private static ConcurrentHashMap<String, ServiceApiLog> concurrentHashMap = new ConcurrentHashMap<String, ServiceApiLog>();

    private static AtomicInteger APICALLNUMBERCOUNTER = new AtomicInteger(0);//api调用次数

    /**
     * 接口计数器
     */
    @Value("${serverlog.counternumber}")
    private int COUNTERNUMBER;


    /**
     * 对日志进行消费，统计接口调用次数，成功率，命中率，投放率等重要信息
     *
     * @param logMessage
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = "dmpChinadep.log", type = ExchangeTypes.FANOUT),
            value = @Queue(value = "contolLog")))
    public synchronized void serviceServerLog(String logMessage) throws Exception {
        try {
            DateTime thisTime = new DateTime();
            // 日志信息截取
            boolean flag = true;
            JsonNode logMessageNode = OBJECT_MAPPER.readTree(logMessage);
            String message = String.valueOf(logMessageNode.get("message"));

            // dmp项目请求
            if (StringUtils.isNotBlank(message)) {
                ServiceApiLog apiLogs;

                JsonNode messageNode = OBJECT_MAPPER.readTree(message);
                String url = "";
                JsonNode jUrl = messageNode.get("url");
                JsonNode controller = messageNode.get("controller");

                String time = logMessageNode.get("time").asText(); //用于根据小时来统计

                String[] times = StringUtils.splitByWholeSeparator(time, ":");

                String apiName = null;
                String costid = null;
                String userid = null;
                if (null != jUrl) {
                    url = jUrl.asText();

                    if (StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpTagVerifyController.post")
                            || StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpController.qrydataV2")
                            || StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpController.qrydataV3")
                            || StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpController.qrydataV4")
                            || StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpMacControl.mac")
                            || StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpRelatedIdForTagController.relatedIdforTag")
                            ) {
                        JsonNode paremeter = messageNode.get("paremeter");
                        JsonNode paremeterJson = OBJECT_MAPPER.readTree(paremeter.get(0).toString());
                        JsonNode dataRange = paremeterJson.get("dataRange");
                        apiName = controller.asText() + "-" + dataRange.asText() + "-" + times[0];
                        costid = dataRange.asText();
                        userid = paremeterJson.get("userId").asText();
                    } else if (StringUtils.equals(controller.asText(), "com.uuzu.gapoi.web.DeviceRelationMsgController.deviceRelationInfoPage")) {
                        JsonNode paremeter = messageNode.get("paremeter");
                        JsonNode paremeterJson = OBJECT_MAPPER.readTree(paremeter.get(0).toString());
                        JsonNode dataRange = paremeterJson.get("dataRange");
                        apiName = controller.asText() + "-" + dataRange.asText() + "-" + times[0];
                        costid = OBJECT_MAPPER.readTree(dataRange.asText().replace("'","\"")).get("costId").asText();
                        userid = paremeterJson.get("userId").asText();
                    } else if (StringUtils.equals(controller.asText(), "com.uuzu.gapoi.web.DeviceRelationMsgController.reqDeviceRelationMsg")) {
                        JsonNode paremeter = messageNode.get("paremeter");
                        JsonNode paremeterJson = OBJECT_MAPPER.readTree(paremeter.get(0).toString());
                        apiName = controller.asText() + "-" + times[0];
                        userid = paremeterJson.get("userId").asText();
                        costid = "mob_gapoi" + userid;
                    } else if (StringUtils.equals(controller.asText(), "com.uuzu.chinadep.web.DmpIdMappingController.idmapping")) {
                        JsonNode paremeter = messageNode.get("paremeter");
                        apiName = controller.asText() + "-" + times[0];
                        userid = paremeter.get("user_id").asText();
                        costid = "mob_dmp_idmapping" + userid;
                    } else {
                        apiName = controller.asText();
                        flag = false;
                    }
                } else {
                    JsonNode strategyId = messageNode.get("strategyId");
                    if (!StringUtils.equals(strategyId.asText(), "null")) {
                        apiName = controller.asText() + "-" + strategyId.asText() + "-" + times[0];
                        costid = strategyId.asText();
                    } else {
                        apiName = controller.asText();
                        flag = false;
                    }
                }


                String key = costid + "-" + times[0];

                if (times[0].contains(" 00")) {
                    if (concurrentHashMap.size() != 1) {
                        for (Map.Entry<String, ServiceApiLog> entry : concurrentHashMap.entrySet()) {
                            doNumberCounter(entry.getValue(), APICALLNUMBERCOUNTER);
                        }
                        concurrentHashMap.clear();
                    }
                }


                //根据costid获取统计对象
                if (!concurrentHashMap.containsKey(key) && StringUtils.isNotBlank(userid)) {
                    apiLogs = new ServiceApiLog(key, apiName, costid, userid);
                    concurrentHashMap.put(key, apiLogs);
                } else {
                    apiLogs = concurrentHashMap.get(key);
                }

                if (apiLogs != null) {
                    //消费请求报文日志
                    if (flag && StringUtils.isNotBlank(url) && !StringUtils.equals(url, "")) {
                        //接口调用次数加1
                        apiLogs.getAPICALLNUMBERCOUNTER().getAndIncrement();

                    } else {
                        //回执报文日志处理
                        JsonNode responseParemeter = messageNode.get("responseParemeter");
                        JsonNode statusCodeValue = null;
                        if (null != responseParemeter) {
                            statusCodeValue = responseParemeter.get("statusCodeValue");
                        }

                        //统计接口状态为200的次数
                        if (null != statusCodeValue && flag && statusCodeValue.asInt() == 200) {
                            apiLogs.getAPICALL200().getAndIncrement();
                        }
                        //统计接口状态为404的次数
                        if (null != statusCodeValue && flag && statusCodeValue.asInt() == 404) {
                            apiLogs.getAPICALL404().getAndIncrement();
                        }
                        //统计接口状态为500的次数
                        if (null != statusCodeValue && flag && statusCodeValue.asInt() == 500) {
                            apiLogs.getAPICALL500().getAndIncrement();
                        }

                    }

                    APICALLNUMBERCOUNTER.getAndIncrement();

                    //超过限制，存mongo
                    if (APICALLNUMBERCOUNTER.get() > COUNTERNUMBER) {
//                        if(times[0].contains(" 00")){
//                            ServiceApiLog oldLog = this.chinaDmpServiceApiLogsRepository.findOne(key);
//                            if(oldLog==null){
//                                for (Map.Entry<String, ServiceApiLog> entry : concurrentHashMap.entrySet()) {
//                                    doNumberCounter(entry.getValue(), APICALLNUMBERCOUNTER);
//
//                                }
//                                concurrentHashMap.clear();
//                                concurrentHashMap.put(key, apiLogs);
//                            }
//                        }

                        for (Map.Entry<String, ServiceApiLog> entry : concurrentHashMap.entrySet()) {
                            doNumberCounter(entry.getValue(), APICALLNUMBERCOUNTER);

                        }
                    }
                }
            }

        } catch (
                Exception e)

        {
            e.printStackTrace();
            log.error(e.getMessage());
        } finally

        {
            taskExecutor.execute(() -> log.info(logMessage));
        }


    }

    /**
     * 分布式插入锁
     *
     * @param serviceApiLog
     * @param numberCounter
     * @throws Exception
     */
    private void doNumberCounter(ServiceApiLog serviceApiLog, AtomicInteger numberCounter) throws Exception {
        redisLocker.lock(LOCK_NAME, () -> {
            ServiceApiLog oldLog = this.chinaDmpServiceApiLogsRepository.findOne(serviceApiLog.getId());
            if (oldLog != null) {
                serviceApiLog.setAPICALL200(new AtomicInteger(oldLog.getAPICALL200().intValue() + serviceApiLog.getAPICALL200().intValue()));
                serviceApiLog.setAPICALL404(new AtomicInteger(oldLog.getAPICALL404().intValue() + serviceApiLog.getAPICALL404().intValue()));
                serviceApiLog.setAPICALL500(new AtomicInteger(oldLog.getAPICALL500().intValue() + serviceApiLog.getAPICALL500().intValue()));
                serviceApiLog.setAPICALLNUMBERCOUNTER(new AtomicInteger(oldLog.getAPICALLNUMBERCOUNTER().intValue() + serviceApiLog.getAPICALLNUMBERCOUNTER().intValue()));
            }
            this.chinaDmpServiceApiLogsRepository.save(serviceApiLog);

            serviceApiLog.setAPICALLNUMBERCOUNTER(new AtomicInteger(0));
            serviceApiLog.setAPICALL500(new AtomicInteger(0));
            serviceApiLog.setAPICALL404(new AtomicInteger(0));
            serviceApiLog.setAPICALL200(new AtomicInteger(0));

            numberCounter.set(0);
            return null;
        });
    }


}
