package com.uuzu.log.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uuzu.common.pojo.*;
import com.uuzu.log.domain.*;
import com.uuzu.log.repository.UserPriceRepository;
import com.uuzu.log.service.httpapi.CostApiService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lixing on 2017/5/23.
 */
@Service
public class ServiceApiLogsService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CostApiService costApiService;

    @Autowired
    private UserPriceRepository userPriceRepository;

    /**
     * 分页获取dmp实时接口日志信息
     *
     * @param date
     * @param user_id
     * @param apiName
     * @param pageable
     * @return
     */
    public DmpServiceApiChart queryChinaDmpServiceApiLogsByDateAndName(String date, String user_id, String apiName, Pageable pageable) throws IOException {
        Query query = new Query();
        query.addCriteria(Criteria.where("apiName").regex(".*?" + "com.uuzu.chinadep.web" + ".*" + apiName + ".*" + date + ".*"));
        if (!"null".equals(user_id)) {
            query.addCriteria(Criteria.where("userid").is(user_id));
        }

        long count = this.mongoTemplate.count(query, ServiceApiLog.class);
        List<ServiceApiLog> chinaDmpServiceApiLogs = this.mongoTemplate.find(query.with(pageable), ServiceApiLog.class);

        //按照costid group by
        Map<String, ServiceApiLog> costMap = new HashMap<String, ServiceApiLog>();
        for (ServiceApiLog chinaDmpServiceApiLog : chinaDmpServiceApiLogs) {
            if (costMap.containsKey(chinaDmpServiceApiLog.getCostId())) {
                ServiceApiLog temp = costMap.get(chinaDmpServiceApiLog.getCostId());

                temp.setAPICALL200(new AtomicInteger(temp.getAPICALL200().addAndGet(chinaDmpServiceApiLog.getAPICALL200().intValue())));
                temp.setAPICALL404(new AtomicInteger(temp.getAPICALL404().addAndGet(chinaDmpServiceApiLog.getAPICALL404().intValue())));
                temp.setAPICALL500(new AtomicInteger(temp.getAPICALL500().addAndGet(chinaDmpServiceApiLog.getAPICALL500().intValue())));
                temp.setAPICALLNUMBERCOUNTER(new AtomicInteger(temp.getAPICALLNUMBERCOUNTER().addAndGet(chinaDmpServiceApiLog.getAPICALLNUMBERCOUNTER().intValue())));
                costMap.put(chinaDmpServiceApiLog.getCostId(), temp);
            } else {
                costMap.put(chinaDmpServiceApiLog.getCostId(), chinaDmpServiceApiLog);
            }
        }

        List<ServiceApiLog> result = new ArrayList<ServiceApiLog>();

        //分组后合并
        Iterator<String> iter = costMap.keySet().iterator();
        String key;
        while (iter.hasNext()) {
            key = iter.next();
            result.add(costMap.get(key));
        }
        Double total_price = new Double(0);

        //查询cost对应的不同的price
        for (ServiceApiLog chinaDmpServiceApiLog : result) {
            String cost = costApiService.queryByCostID(chinaDmpServiceApiLog.getCostId());
            if (StringUtils.isEmpty(cost)) {
                throw new NullPointerException("cost_id不存在");
            }
            //System.err.println(cost);
            JsonNode jsonNode = OBJECT_MAPPER.readTree(cost);
            int model_type = jsonNode.get("model_type").asInt();
            double price = jsonNode.get("price").asDouble();
            if (model_type == 1001) {
                total_price = price * (chinaDmpServiceApiLog.getAPICALLNUMBERCOUNTER().intValue() - chinaDmpServiceApiLog.getAPICALL500().intValue());
                chinaDmpServiceApiLog.setPrice(total_price.toString());
            } else if (model_type == 2001) {
                //总调用量
                UserPrice userPrice = userPriceRepository.findByCostID(chinaDmpServiceApiLog.getCostId());

                int total_num = chinaDmpServiceApiLog.getAPICALLNUMBERCOUNTER().intValue() - chinaDmpServiceApiLog.getAPICALL500().intValue();
                if (total_num <= 100000) {
                    total_price = total_num * (userPrice.getPriceMap().get("model2_A"));
                } else if (total_num <= 500000) {
                    total_price = total_num * (userPrice.getPriceMap().get("model2_B"));
                } else if (total_num <= 2000000) {
                    total_price = total_num * (userPrice.getPriceMap().get("model2_C"));
                } else if (total_num <= 10000000) {
                    total_price = total_num * (userPrice.getPriceMap().get("model2_D"));
                }else{
                    total_price = total_num * (userPrice.getPriceMap().get("model2_E"));
                }
                chinaDmpServiceApiLog.setPrice(total_price.toString());
            }else if (model_type == 4001) {
                total_price = price * (chinaDmpServiceApiLog.getAPICALL200().intValue());
                chinaDmpServiceApiLog.setPrice(total_price.toString());
            }

        }


        DmpServiceApiChart dmpServiceApiChart = new DmpServiceApiChart();
        Page<ServiceApiLog> allApiLogs = new PageImpl<>(result, pageable, count);
        dmpServiceApiChart.setChinaDmpServiceApiLogs(allApiLogs);

        return dmpServiceApiChart;
    }

    /**
     * mac投户外接口查询
     *
     * @param date
     * @param pageable
     * @return
     */
    public DmpServiceMacApiChart queryMacServcieLogByDate(String date, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("createAt").regex(".*?" + date.replaceAll("-","" ) + ".*"));

        long count = this.mongoTemplate.count(query, DmpTask.class);
        List<DmpTask> chinaDmpServiceApiLogs = this.mongoTemplate.find(query.with(pageable), DmpTask.class);


//        //查询cost对应的不同的price
//        for (DmpTask chinaDmpServiceApiLog : chinaDmpServiceApiLogs) {
//            chinaDmpServiceApiLog.setPrice(costApiService.queryPriceByCostID(chinaDmpServiceApiLog.getCostId()).replace("\"", ""));
//        }


        DmpServiceMacApiChart dmpServiceApiChart = new DmpServiceMacApiChart();
        Page<DmpTask> allApiLogs = new PageImpl<>(chinaDmpServiceApiLogs, pageable, count);
        dmpServiceApiChart.setDmpTasks(allApiLogs);

        return dmpServiceApiChart;
    }


    /**
     * 分页获取dmp实时接口日志信息
     *
     * @param date
     * @param user_id
     * @param apiName
     * @param pageable
     * @return
     */
    public DmpServiceApiChart queryReqDeviceRelationMsgByDateAndName(String date, String user_id, String apiName, Pageable pageable) throws IOException {
        Query query = new Query();
        if ("idmapping".equals(apiName)) {
            query.addCriteria(Criteria.where("apiName").regex(".*?" + "com.uuzu.chinadep.web" + ".*" + apiName + ".*" + date + ".*"));
        } else {
            query.addCriteria(Criteria.where("apiName").regex(".*?" + "com.uuzu.gapoi.web" + ".*" + apiName + ".*" + date + ".*"));
        }

        if (!"null".equals(user_id)) {
            query.addCriteria(Criteria.where("userid").is(user_id));
        }

        long count = this.mongoTemplate.count(query, ServiceApiLog.class);
        List<ServiceApiLog> chinaDmpServiceApiLogs = this.mongoTemplate.find(query.with(pageable), ServiceApiLog.class);

        Map<String, ServiceApiLog> userMap = new HashMap<String, ServiceApiLog>();
        for (ServiceApiLog chinaDmpServiceApiLog : chinaDmpServiceApiLogs) {
            if (userMap.containsKey(chinaDmpServiceApiLog.getUserid())) {
                ServiceApiLog temp = userMap.get(chinaDmpServiceApiLog.getUserid());

                temp.setAPICALL200(new AtomicInteger(temp.getAPICALL200().addAndGet(chinaDmpServiceApiLog.getAPICALL200().intValue())));
                temp.setAPICALL404(new AtomicInteger(temp.getAPICALL404().addAndGet(chinaDmpServiceApiLog.getAPICALL404().intValue())));
                temp.setAPICALL500(new AtomicInteger(temp.getAPICALL500().addAndGet(chinaDmpServiceApiLog.getAPICALL500().intValue())));
                temp.setAPICALLNUMBERCOUNTER(new AtomicInteger(temp.getAPICALLNUMBERCOUNTER().addAndGet(chinaDmpServiceApiLog.getAPICALLNUMBERCOUNTER().intValue())));
                userMap.put(chinaDmpServiceApiLog.getUserid(), temp);
            } else {
                userMap.put(chinaDmpServiceApiLog.getUserid(), chinaDmpServiceApiLog);
            }
        }

        List<ServiceApiLog> result = new ArrayList<ServiceApiLog>();

        //分组后合并
        Iterator<String> iter = userMap.keySet().iterator();
        String key;
        while (iter.hasNext()) {
            key = iter.next();
            result.add(userMap.get(key));
        }

        DmpServiceApiChart dmpServiceApiChart = new DmpServiceApiChart();
        Page<ServiceApiLog> allApiLogs = new PageImpl<>(result, pageable, count);
        dmpServiceApiChart.setChinaDmpServiceApiLogs(allApiLogs);

        return dmpServiceApiChart;
    }

    /**
     * 分页排序查询每日接口调用量等数据信息
     *
     * @param date
     * @param pageable
     * @return
     */
    public ServiceApiAndCallBackChart queryServiceApiLogsByDate(String date, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").regex(".*?" + "com.uuzu.wisemedia.web.DmpController.tagverify-" + ".*" + date));


        long count = this.mongoTemplate.count(query, ServiceApiLogs.class);
        List<ServiceApiLogs> serviceApiLogs = this.mongoTemplate.find(query.with(pageable), ServiceApiLogs.class);

        Page<ServiceApiLogs> allApiLogs = new PageImpl<>(serviceApiLogs, pageable, count);

        ServiceApiAndCallBackChart serviceApiAndCallBackChart = queryCallBackByDateAndSetViewData(date, serviceApiLogs);
        //ServiceApiAndCallBackChart serviceApiAndCallBackChart = queryCallBackByDate(date, serviceApiLogs);

        serviceApiAndCallBackChart.setServiceApiLogs(allApiLogs);

        return serviceApiAndCallBackChart;
    }


    /**
     * 查询回调数据
     *
     * @param date
     * @param serviceApiLogs
     */
    public ServiceApiAndCallBackChart queryCallBackByDateAndSetViewData(String date, List<ServiceApiLogs> serviceApiLogs) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").regex("callBack.message-" + date));

        List<CallBackRequest> callBackRequests = this.mongoTemplate.find(query, CallBackRequest.class);

        ServiceApiAndCallBackChart serviceApiAndCallBackChart = new ServiceApiAndCallBackChart();

        for (ServiceApiLogs apiLogs : serviceApiLogs) {

            DateTime creatTime = apiLogs.getCreatTime();
            int hour = creatTime.getHourOfDay();

            String id = apiLogs.getId();
            String[] split = StringUtils.split(id, "-");

            for (CallBackRequest callBackRequest : callBackRequests) {

                DateTime callBackRequestCreatTime = callBackRequest.getCreatTime();
                int callBackRequestHour = callBackRequestCreatTime.getHourOfDay();
                String strategyId = callBackRequest.getStrategyId();

                if (hour == callBackRequestHour && StringUtils.equals(split[1], strategyId)) {

                    switch (callBackRequest.getEventType()) {
                        //曝光
                        case 1:
                            long totalExposureNumber = serviceApiAndCallBackChart.getTotalExposureNumber();
                            serviceApiAndCallBackChart.setTotalExposureNumber(++totalExposureNumber);
                            long exposureNumber = apiLogs.getExposureNumber();
                            apiLogs.setExposureNumber(++exposureNumber);
                            break;
                        //点击
                        case 2:
                            long totalClickNumber = serviceApiAndCallBackChart.getTotalClickNumber();
                            serviceApiAndCallBackChart.setTotalClickNumber(++totalClickNumber);
                            long clickNumber = apiLogs.getClickNumber();
                            apiLogs.setClickNumber(++clickNumber);
                            break;
                        //转化
                        case 3:
                            long totalConversionNumber = serviceApiAndCallBackChart.getTotalConversionNumber();
                            serviceApiAndCallBackChart.setTotalConversionNumber(++totalConversionNumber);
                            long conversionNumber = apiLogs.getConversionNumber();
                            apiLogs.setConversionNumber(++conversionNumber);
                            break;
                        //激活
                        case 4:
                            long totalActivationNumber = serviceApiAndCallBackChart.getTotalActivationNumber();
                            serviceApiAndCallBackChart.setTotalActivationNumber(++totalActivationNumber);
                            long activationNumber = apiLogs.getActivationNumber();
                            apiLogs.setActivationNumber(++activationNumber);
                            break;
                        //有效
                        case 5:
                            long totalTffectiveNumber = serviceApiAndCallBackChart.getTotalEffectiveNumber();
                            serviceApiAndCallBackChart.setTotalEffectiveNumber(++totalTffectiveNumber);
                            long effectiveNumber = apiLogs.getEffectiveNumber();
                            apiLogs.setEffectiveNumber(++effectiveNumber);
                            break;
                        //付费
                        case 6:
                            long totaleFirstDayPayment = serviceApiAndCallBackChart.getTotalFirstDayPayment();
                            String rechargeAmount = callBackRequest.getMessageCallback().getRechargeAmount();
                            serviceApiAndCallBackChart.setTotalFirstDayPayment(Long.valueOf(rechargeAmount) + totaleFirstDayPayment);
                            long firstDayPayment = apiLogs.getFirstDayPayment();
                            apiLogs.setFirstDayPayment(Long.valueOf(rechargeAmount) + firstDayPayment);
                            break;
                    }
                }
            }
        }


        List<ServiceApiAndCallBackChart.StrategyData> newList = new ArrayList<>();
        List<ServiceApiAndCallBackChart.StrategyData> strategyDatas = serviceApiAndCallBackChart.getStrategyDatas();
        boolean equals = true;
        for (ServiceApiLogs serviceApiLog : serviceApiLogs) {
            String id = serviceApiLog.getId();
            String[] idList = StringUtils.split(id, "-");
            String strategyId = idList[1];

            String isAiFlag = queryIsAiFlagBy(strategyId);
            String gameName = queryGameName(strategyId);

            if (null != strategyDatas && !strategyDatas.isEmpty()) {
                for (ServiceApiAndCallBackChart.StrategyData strategyData : strategyDatas) {
                    equals = StringUtils.equals(strategyId, strategyData.getStrategyId());
                    if (equals) {
                        break;
                    } else {
                        ServiceApiAndCallBackChart.StrategyData strategyDataNew = new ServiceApiAndCallBackChart.StrategyData();
                        strategyDataNew.setStrategyId(strategyId);
                        strategyDataNew.setIsAiFlag(isAiFlag);
                        strategyDataNew.setGameName(gameName);
                        newList.add(strategyDataNew);
                    }
                }
            } else {
                ServiceApiAndCallBackChart.StrategyData strategyData = new ServiceApiAndCallBackChart.StrategyData();
                strategyData.setStrategyId(strategyId);
                strategyData.setIsAiFlag(isAiFlag);
                strategyData.setGameName(gameName);
                strategyDatas.add(strategyData);

            }

            if (!newList.isEmpty() && !equals) {
                for (ServiceApiAndCallBackChart.StrategyData newStr : newList) {
                    strategyDatas.add(newStr);
                }
                newList.clear();
            }


        }


        return serviceApiAndCallBackChart;


    }


    /**
     * 查询回流数据
     * @param date
     * @param serviceApiLogs
     * @return
     */
    /*public ServiceApiAndCallBackChart queryCallBackByDate(String date,List<ServiceApiLogs> serviceApiLogs) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").regex("callBack.message-"+date));

        List<CallBackRequest> callBackRequests = this.mongoTemplate.find(query, CallBackRequest.class);

        ServiceApiAndCallBackChart serviceApiAndCallBackChart = new ServiceApiAndCallBackChart();
        for (CallBackRequest callBackRequest : callBackRequests){

            switch (callBackRequest.getEventType()){

                //曝光
                case 1 :
                    long exposureNumber = serviceApiAndCallBackChart.getExposureNumber();
                    serviceApiAndCallBackChart.setExposureNumber(++exposureNumber);
                    break;
                 //点击
                case 2 :
                    long clickNumber = serviceApiAndCallBackChart.getClickNumber();
                    serviceApiAndCallBackChart.setClickNumber(++clickNumber);
                    break;
                 //转化
                case 3 :
                    long conversionNumber = serviceApiAndCallBackChart.getConversionNumber();
                    serviceApiAndCallBackChart.setConversionNumber(++conversionNumber);
                    break;
                //激活
                case 4 :
                    long activationNumber = serviceApiAndCallBackChart.getActivationNumber();
                    serviceApiAndCallBackChart.setActivationNumber(++activationNumber);
                    break;
                 //有效
                case 5 :
                    long effectiveNumber = serviceApiAndCallBackChart.getEffectiveNumber();
                    serviceApiAndCallBackChart.setEffectiveNumber(++effectiveNumber);
                    break;
                 //付费
                case 6 :
                    long firstDayPayment = serviceApiAndCallBackChart.getFirstDayPayment();
                    String rechargeAmount = callBackRequest.getMessageCallback().getRechargeAmount();
                    serviceApiAndCallBackChart.setFirstDayPayment(Long.valueOf(rechargeAmount)+firstDayPayment);
                    break;

            }

        }
        List<ServiceApiAndCallBackChart.StrategyData> newList = new ArrayList<>();
        List<ServiceApiAndCallBackChart.StrategyData> strategyDatas = serviceApiAndCallBackChart.getStrategyDatas();
        boolean equals;
        for (ServiceApiLogs serviceApiLog : serviceApiLogs){
            String id = serviceApiLog.getId();
            String[] idList = StringUtils.split(id, "-");
            String strategyId = idList[1];

            String isAiFlag = queryIsAiFlagBy(strategyId);

            if( null != strategyDatas && !strategyDatas.isEmpty() ) {
                for (ServiceApiAndCallBackChart.StrategyData strategyData: strategyDatas){
                    equals = StringUtils.equals(strategyId, strategyData.getStrategyId());
                    if(equals){
                        break;
                    }else {
                        ServiceApiAndCallBackChart.StrategyData strategyDataNew = new ServiceApiAndCallBackChart.StrategyData();
                        strategyDataNew.setStrategyId(strategyId);
                        strategyDataNew.setIsAiFlag(isAiFlag);
                        newList.add(strategyDataNew);
                    }
                }
            } else {
                ServiceApiAndCallBackChart.StrategyData strategyData = new ServiceApiAndCallBackChart.StrategyData();
                strategyData.setStrategyId(strategyId);
                strategyData.setIsAiFlag(isAiFlag);
                strategyDatas.add(strategyData);

            }

        }

        if( !newList.isEmpty() ){
            for(ServiceApiAndCallBackChart.StrategyData newStr : newList){
                strategyDatas.add(newStr);
            }
        }

        return serviceApiAndCallBackChart;

    }*/

    /**
     * 查询是否经过算法优化
     *
     * @param strategyId
     * @return
     */
    private String queryIsAiFlagBy(String strategyId) {
        Query query = new Query(Criteria.where("id").is(strategyId));
        List<StrategyIdToDataRanage> strategyIdToDataRanages = this.mongoTemplate.find(query, StrategyIdToDataRanage.class);
        if (!strategyIdToDataRanages.isEmpty()) {
            return strategyIdToDataRanages.get(0).getIsAiFlag();
        }
        return "3";
    }

    /**
     * 查询游戏名称
     *
     * @param strategyId
     * @return
     */
    private String queryGameName(String strategyId) {
        Query query = new Query(Criteria.where("id").is(strategyId));
        StrategyIdToDataRanage one = this.mongoTemplate.findOne(query, StrategyIdToDataRanage.class);

        Query query1 = new Query(Criteria.where("id").is(one.getAdTargetId()));
        GameNameMapping gameNameMapping = this.mongoTemplate.findOne(query1, GameNameMapping.class);

        if (null != gameNameMapping) {
            return gameNameMapping.getGameName();
        }
        return "未知游戏";
    }


}
