package com.uuzu.log.domain;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

/**
 * Created by lixing on 2017/5/3.
 * 接口调用统计
 */
@Data
public class ServiceApiLogs {

    @Id
    private String id;
    private String apiName;
    private long apiCallNumber = 0; //问询量
    private long apiCallSuccessNumber = 0;//问询成功量
    private long apiHitNumber = 0;  //问询命中量
    private long tagsHitNumber = 0; //投放命中量
    @Transient
    private long exposureNumber = 0; //曝光量
    @Transient
    private long clickNumber = 0; //点击量
    @Transient
    private long conversionNumber = 0; //转化量
    @Transient
    private long activationNumber = 0; //激活量
    @Transient
    private long effectiveNumber = 0; //有效量
    @Transient
    private long firstDayPayment = 0; //首日付费金额
    private DateTime creatTime = new DateTime();
    private DateTime updateTime = new DateTime();

    public ServiceApiLogs() {
    }

    public ServiceApiLogs(String id, String apiName) {
        this.id = id;
        this.apiName = apiName;
    }

    public ServiceApiLogs(String id, String apiName, long apiCallNumber) {
        this.id = id;
        this.apiName = apiName;
        this.apiCallNumber = apiCallNumber;
    }
}
