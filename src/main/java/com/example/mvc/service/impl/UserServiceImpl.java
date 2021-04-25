package com.example.mvc.service.impl;

import com.example.mvc.annotation.Service;
import com.example.mvc.service.UserService;

/**
 * @author panzhi
 * @since 2021-04-24
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public boolean login(String userName, String userPwd) {
        if("zhangsan".equals(userName) && "123456".equals(userPwd)){
            return true;
        } else {
            return false;
        }
    }
}
