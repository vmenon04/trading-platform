package com.neueda.leap.service;

import com.neueda.leap.repository.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Long getUserIdByClientId(Long clientId) {
        return userMapper.findUserIdByClientId(clientId);
    }
}
