package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// let's do all the checks here in the SQL statements themselves to avoid race conditions
@Mapper
public interface AccountMapper {
    @Select("SELECT balance FROM accounts WHERE account_id = #{accountId}")
    BigDecimal findBalance(int accountId);

    //returns rows updated: 0 = no such account
    @Update("UPDATE accounts SET balance = balance + #{amount} WHERE account_id = #{accountId}")
    int increaseBalance(@Param("accountId") int accountId, @Param("amount") BigDecimal amount);

    // returns rows updated: 0 = no such account OR insufficient funds (we check and update in one statement)
    @Update("UPDATE accounts SET balance = balance - #{amount} "
            + "WHERE account_id = #{accountId} AND balance >= #{amount}")
    int reduceBalance(@Param("accountId") int accountId, @Param("amount") BigDecimal amount);
}
