package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClientAccountsMapper {

    @Select("SELECT client_id as clientId FROM client_accounts WHERE account_id = #{accountId}")
    String getClientIdByAccountId(Long accountId);
}
