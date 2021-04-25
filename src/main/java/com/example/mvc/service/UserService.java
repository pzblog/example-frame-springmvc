package com.example.mvc.service;

public interface UserService {

    /**
     * 登录
     * @param userName
     * @param userPwd
     * @return
     */
    boolean login(String userName, String userPwd);
}
