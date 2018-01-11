package com.uuzu.log.web;

import com.uuzu.common.pojo.UserApiaskNum;
import com.uuzu.log.service.UserApiaskNumService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Created by lixing on 2017/6/5.
 */
@ApiIgnore
@Controller
public class UserApiaskNumController {

    @Autowired
    private UserApiaskNumService userApiaskNumService;

    /**
     * 用户流量限制
     * @param userId
     * @param num
     * @return
     */
    @PostMapping("/saveUserApiaskNum")
    public ResponseEntity<UserApiaskNum> saveUserApiaskNum(@RequestParam("userId")String userId,
                                                           @RequestParam("num")String num){
        UserApiaskNum userApiaskNum = new UserApiaskNum();
        try {
            userApiaskNum.setUserId(userId);
            userApiaskNum.setNum(num);
            userApiaskNum = this.userApiaskNumService.saveUserApiaskNum(userApiaskNum);

            if(null != userApiaskNum){
                return ResponseEntity.ok(userApiaskNum);
            }
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(userApiaskNum);
        } catch (Exception e) {

        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(userApiaskNum);
    }


    /**
     * 查询现有的用户限制调用情况
     * @return
     */
    @GetMapping("/queryUserApiaskNum")
    public ResponseEntity<List<UserApiaskNum>> queryUserApiaskNum(){
        List<UserApiaskNum> userApiaskNums = null;
        try {
            userApiaskNums = this.userApiaskNumService.queryUserApiaskNum();

            if(null != userApiaskNums && !userApiaskNums.isEmpty()){
                return ResponseEntity.ok(userApiaskNums);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userApiaskNums);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(userApiaskNums);
    }


}
