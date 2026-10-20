package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT user_id FROM users WHERE client_id = #{clientId}")
    Long findUserIdByClientId(Long clientId);
}
