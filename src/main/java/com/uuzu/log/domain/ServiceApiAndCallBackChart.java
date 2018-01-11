package com.uuzu.log.domain;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by lixing on 2017/5/24.
 */
@Data
public class ServiceApiAndCallBackChart {

    private Page<ServiceApiLogs> serviceApiLogs;
    private List<StrategyData> StrategyDatas = new ArrayList<>();
    private String isAiFlag;         //是否算法优化
    private long totalExposureNumber = 0; //曝光量
    private long totalClickNumber = 0; //点击量
    private long totalConversionNumber = 0; //转化量
    private long totalActivationNumber = 0; //激活量
    private long totalEffectiveNumber = 0; //有效量
    private long totalFirstDayPayment = 0; //首日付费金额

    @Data
    public static class StrategyData{
        private String strategyId;
        private String isAiFlag;
        private String gameName;
    }

}
