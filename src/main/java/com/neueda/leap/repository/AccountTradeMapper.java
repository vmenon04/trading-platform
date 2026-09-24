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

    @Update("UPDATE account_trades SET status = #{status} WHERE trade_id = #{trade_Id}")
    void updateStatus(AccountTrade trade);

    @Delete("DELETE FROM account_trades WHERE trade_id = #{trade_Id}")
    void deleteById(Integer trade_Id);

    // Vasu's additions for accounttrademapper
    @Select("SELECT quantity FROM account_holdings "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'active'")
    BigDecimal findActiveQuantity(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    // marks the current holding as inactive before we replace it
    @Update("UPDATE account_holdings SET status = 'inactive' "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'active'")
    void deactivateHolding(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    @Insert("INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status) "
            + "VALUES (#{accountId}, #{instrumentId}, NOW(), #{quantity}, #{status})")
    void insertSnapshot(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                        @Param("quantity") BigDecimal quantity, @Param("status") String status);
    
}

