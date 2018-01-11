package com.uuzu.log.web;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import springfox.documentation.annotations.ApiIgnore;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by lixing on 2017/3/29.
 */
@Slf4j
@ApiIgnore
@Controller
public class PageController {

    ThreadLocal<Long> dateTime = new ThreadLocal<>();


    @GetMapping("/")
    public ResponseEntity<String> page(){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @GetMapping("date")
    public ResponseEntity<Map<String,Object>> date(){
        try {
            Map<String,Object> resultMap = new LinkedHashMap();
            dateTime.set(System.currentTimeMillis());
            resultMap.put("status",200);
            resultMap.put("timestamp",String.valueOf(dateTime.get()));
            resultMap.put("datetime",new DateTime(dateTime.get()).toString("yyyy-MM-dd HH:mm:ss"));
            return ResponseEntity.ok(resultMap);
        } catch (Exception e) {
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
