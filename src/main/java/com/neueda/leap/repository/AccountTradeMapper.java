package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.*;

@Mapper 
public interface AccountTradeMapper {

    @Select("SELECT * FROM account_trades WHERE trade_id = #{trade_Id}")
    AccountTrade findById(Integer trade_Id);

    @Select("SELECT * FROM account_trades WHERE account_id = #{account_Id}")
    List<AccountTrade> findByAccountId(Integer account_Id);


    @Insert("INSERT INTO account_trades(trade_time, account_id, instrument_id, trade_type, quantity, price, status) VALUES (#{trade_Time}, #{account_Id}, #{instrument_Id}, #{trade_Type}, #{quantity}, #{price}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "trade_Id")
    void insert(AccountTrade accountTrade);

    @Delete("DELETE FROM account_trades WHERE trade_id = #{trade_Id}")
    void deleteById(Integer trade_Id);

    // Vasu's additions for accounttrademapper

    // Postgres INSERT ... RETURNING goes through @Select so MyBatis returns the generated trade_id
    @Select("INSERT INTO account_trades (trade_time, account_id, instrument_id, trade_type, quantity, price, status) "
            + "VALUES (NOW(), #{accountId}, #{instrumentId}, #{tradeType}, #{quantity}, #{price}, #{status}) "
            + "RETURNING trade_id")
    int insertTrade(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                    @Param("tradeType") String tradeType, @Param("quantity") BigDecimal quantity,
                    @Param("price") BigDecimal price, @Param("status") String status);

    // Replaces the old updateStatus(AccountTrade): MyBatis can't have two methods with the same name
    @Update("UPDATE account_trades SET status = #{status} WHERE trade_id = #{tradeId}")
    void updateStatus(@Param("tradeId") int tradeId, @Param("status") String status);
}

