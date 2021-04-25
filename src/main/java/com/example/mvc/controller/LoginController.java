package com.example.mvc.controller;

import com.example.mvc.annotation.Autowired;
import com.example.mvc.annotation.Controller;
import com.example.mvc.annotation.RequestMapping;
import com.example.mvc.annotation.RequestParam;
import com.example.mvc.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author panzhi
 * @since 2021-04-24
 */
@Controller
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param request
     * @param response
     * @param userName
     * @param userPwd
     * @return
     */
    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam("userName") String userName,
                        @RequestParam("userPwd") String userPwd){
        boolean result = userService.login(userName, userPwd);
        if(result){
            return "登录成功！";
        } else {
            return "登录失败！";
        }
    }
}
