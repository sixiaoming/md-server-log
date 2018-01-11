package com.uuzu.log.service;

import com.uuzu.common.pojo.UserApiaskNum;
import com.uuzu.log.repository.UserApiaskNumRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lixing on 2017/6/5.
 */
@Service
public class UserApiaskNumService {

    @Autowired
    private UserApiaskNumRepository userApiaskNumRepository;

    public UserApiaskNum saveUserApiaskNum(UserApiaskNum userApiaskNum) {
        DateTime dateTime = new DateTime();
        UserApiaskNum one = this.userApiaskNumRepository.findOne(userApiaskNum.getUserId());
        if(null != one){
            one.setNum(userApiaskNum.getNum());
            one.setUpdateTime(dateTime.toString("yyyy-MM-dd HH:mm:ss"));
            return this.userApiaskNumRepository.save(one);
        }
        userApiaskNum.setCreatTime(dateTime.toString("yyyy-MM-dd HH:mm:ss"));
        userApiaskNum.setUpdateTime(dateTime.toString("yyyy-MM-dd HH:mm:ss"));
        return this.userApiaskNumRepository.save(userApiaskNum);
    }

    public List<UserApiaskNum> queryUserApiaskNum(){
        Sort sort = new Sort(Sort.Direction.DESC,"creatTime");
        List<UserApiaskNum> userApiaskNums = this.userApiaskNumRepository.findAll(sort);
        return userApiaskNums;
    }

}
