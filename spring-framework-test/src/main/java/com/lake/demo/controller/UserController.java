package com.lake.demo.controller;

import com.lake.demo.service.IUserService;
import com.lake.mvcframework.annotation.MyAutowired;
import com.lake.mvcframework.annotation.MyController;
import com.lake.mvcframework.annotation.MyRequestMapping;
import com.lake.mvcframework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 配置Controller类，全部使用自己定义的注解
 *
 * @author Lake Fang
 * @date 2019-03-31 11:37
 */
@MyController
@MyRequestMapping("/user")
public class UserController {

    @MyAutowired
    private IUserService userService;

    @MyRequestMapping("/name")
    public String name(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam(value = "name") String name) {
        return userService.getNameById(name);
    }

    @MyRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @MyRequestParam("name") String name) {
        String result = userService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @MyRequestParam("a") Integer a, @MyRequestParam("b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/remove")
    public void remove(HttpServletRequest req, HttpServletResponse resp,
                       @MyRequestParam("id") Integer id) {
    }
}
