package com.ten.aditum.statistics.service;

import com.ten.aditum.statistics.entity.AccessInterval;
import com.ten.aditum.statistics.mapper.AccessIntervalDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AccessIntervalService {

    @Resource
    private AccessIntervalDao accessIntervalDao;

    public int insert(AccessInterval pojo) {
        return accessIntervalDao.insert(pojo);
    }

    public int insertList(List<AccessInterval> pojos) {
        return accessIntervalDao.insertList(pojos);
    }

    public List<AccessInterval> select(AccessInterval pojo) {
        return accessIntervalDao.select(pojo);
    }

    public int update(AccessInterval pojo) {
        return accessIntervalDao.update(pojo);
    }

}
