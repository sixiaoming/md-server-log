package com.uuzu.log.domain;

import com.uuzu.common.pojo.ServiceApiLog;
import lombok.Data;
import org.springframework.data.domain.Page;



/**
 * Created by lixing on 2017/5/24.
 */
@Data
public class DmpServiceApiChart {

    private Page<ServiceApiLog> chinaDmpServiceApiLogs;

}
