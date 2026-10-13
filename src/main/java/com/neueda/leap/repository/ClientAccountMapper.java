package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClientAccountMapper {

    @Select("SELECT client_id as clientId FROM client_accounts WHERE account_id = #{accountId}")
    List<Long> getClientIdByAccountId(Long accountId);
}
