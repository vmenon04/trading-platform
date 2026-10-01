package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.*;

// to keep history we make each status change a new row with the same trade_id.
// the row with the latest trade_time is the current status.
// we use clock_timestamp() rather than NOW() since NOW() is fixed for a whole transaction
@Mapper
public interface AccountTradeMapper {

    // columns aliased to the AccountTrade field names, so MyBatis fills the right fields
    // (the status column goes into the tradeStatus field)
    String COLUMNS = "trade_id AS tradeId, trade_time AS tradeTime, account_id AS accountId, instrument_id AS instrumentId, "
            + "trade_type AS tradeType, quantity, price, status AS tradeStatus";

    // current state of a trade (its latest row)
    @Select("SELECT " + COLUMNS + " FROM account_trades WHERE trade_id = #{trade_Id} ORDER BY trade_time DESC LIMIT 1")
    AccountTrade findById(Integer trade_Id);

    // current state of each of the account's trades (one row per trade)
    @Select("SELECT DISTINCT ON (trade_id) " + COLUMNS + " FROM account_trades WHERE account_id = #{account_Id} "
            + "ORDER BY trade_id, trade_time DESC")
    List<AccountTrade> findByAccountId(Integer account_Id);

    // every status a trade went through, oldest first
    @Select("SELECT " + COLUMNS + " FROM account_trades WHERE trade_id = #{trade_Id} ORDER BY trade_time")
    List<AccountTrade> findHistoryById(Integer trade_Id);

    // #{...} are AccountTrade field names
    @Insert("INSERT INTO account_trades(trade_time, account_id, instrument_id, trade_type, quantity, price, status) VALUES (#{tradeTime}, #{accountId}, #{instrumentId}, #{tradeType}, #{quantity}, #{price}, #{tradeStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "tradeId", keyColumn = "trade_id")
    void insert(AccountTrade accountTrade);

    @Delete("DELETE FROM account_trades WHERE trade_id = #{trade_Id}")
    void deleteById(Integer trade_Id);

    // Vasu's additions for accounttrademapper

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

