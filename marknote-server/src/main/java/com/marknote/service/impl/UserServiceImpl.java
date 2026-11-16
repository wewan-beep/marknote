package com.marknote.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marknote.entity.User;
import com.marknote.mapper.UserMapper;
import com.marknote.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
