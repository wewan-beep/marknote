package com.marknote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marknote.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
