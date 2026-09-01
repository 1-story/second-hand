package com.hdu.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.hdu.secondhand.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
