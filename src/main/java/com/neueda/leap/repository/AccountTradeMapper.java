package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountTradeMapper {
    // Postgres INSERT and the RETURNING goes through @Select so MyBatis returns the generated trade_id
    @Select("INSERT INTO account_trades (trade_time, account_id, instrument_id, trade_type, quantity, price, status) "
            + "VALUES (NOW(), #{accountId}, #{instrumentId}, #{tradeType}, #{quantity}, #{price}, #{status}) "
            + "RETURNING trade_id")
    int insertTrade(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                    @Param("tradeType") String tradeType, @Param("quantity") BigDecimal quantity,
                    @Param("price") BigDecimal price, @Param("status") String status);

    @Update("UPDATE account_trades SET status = #{status} WHERE trade_id = #{tradeId}")
    void updateStatus(@Param("tradeId") int tradeId, @Param("status") String status);
}
