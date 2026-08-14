package com.investedu.smartassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investedu.smartassistant.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM `user` WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM `user` WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM `user` WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
}