package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// to keep history we make each status change a new row with the same trade_id.
// the row with the latest trade_time is the current status.
// we use clock_timestamp() rather than NOW() since NOW() is fixed for a whole transaction
@Mapper
public interface AccountTradeMapper {
    // Postgres INSERT and the RETURNING goes through @Select so MyBatis returns the generated trade_id
    @Select("INSERT INTO account_trades (trade_time, account_id, instrument_id, trade_type, quantity, price, status) "
            + "VALUES (clock_timestamp(), #{accountId}, #{instrumentId}, #{tradeType}, #{quantity}, #{price}, #{status}) "
            + "RETURNING trade_id")
    int insertTrade(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                    @Param("tradeType") String tradeType, @Param("quantity") BigDecimal quantity,
                    @Param("price") BigDecimal price, @Param("status") String status);

    // record the status change: copies the trade's latest row with the new status and the current time
    @Insert("INSERT INTO account_trades "
            + "(trade_id, trade_time, account_id, instrument_id, trade_type, quantity, price, status) "
            + "SELECT trade_id, clock_timestamp(), account_id, instrument_id, trade_type, quantity, price, #{status} "
            + "FROM account_trades WHERE trade_id = #{tradeId} "
            + "ORDER BY trade_time DESC LIMIT 1")
    void insertStatus(@Param("tradeId") int tradeId, @Param("status") String status);
}
