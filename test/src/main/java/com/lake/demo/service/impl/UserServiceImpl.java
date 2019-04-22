package com.lake.demo.service.impl;

import com.lake.demo.service.IUserService;
import com.lake.mvcframework.annotation.MyService;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * lake-spring
 *
 * @author Lake Fang
 * @date 2019-03-31 11:39
 */
@MyService
public class UserServiceImpl implements IUserService {
    @Override
    public String getNameById(String name) {
        return "hai " + name;
    }

    @Override
    public String get(String name) {
        return "Hello " + name;
    }
}