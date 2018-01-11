package com.uuzu.log.domain;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

/**
 * Created by zhoujin on 2017/7/28.
 * 接口调用统计
 */
@Data
public class ChinaDmpServiceApiLogs {

    @Id
    private String id;
    private String apiName;
    private long apiCallNumber = 0; //问询量
    private long apiCallSuccessNumber = 0;//问询成功量
    private long apiHitNumber = 0;  //问询命中量
    private long api500Number = 0;
    private long api404Number = 0;
    private String user_id;
    private String costId;//计费id
    private DateTime creatTime = new DateTime();
    private DateTime updateTime = new DateTime();

    public ChinaDmpServiceApiLogs() {
    }

    public ChinaDmpServiceApiLogs(String id, String apiName, String costId) {
        this.id = id;
        this.apiName = apiName;
        this.costId = costId;
    }

    public ChinaDmpServiceApiLogs(String id, String apiName, long apiCallNumber, String costId,String user_id) {
        this.id = id;
        this.apiName = apiName;
        this.apiCallNumber = apiCallNumber;
        this.costId = costId;
        this.user_id = user_id;
    }
}
