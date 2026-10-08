package com.neueda.leap.repository;

import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

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
    String TRADE_COLUMNS = "trade_id AS tradeId, account_id AS accountId, instrument_id AS instrumentId, "
            + "trade_side AS tradeSide, quantity";

    String STATUS_COLUMNS = "status, trade_time AS tradeTime";

    String PRICE_COLUMNS = "price";

    // current state of a trade (its latest row)
    @Select("SELECT " + TRADE_COLUMNS + STATUS_COLUMNS + PRICE_COLUMNS + " FROM account_trades " +
            "LEFT JOIN account_trade_status ON account_trades.trade_id = account_trade_status.trade_id " +
            "LEFT JOIN account_trade_price ON account_trades.trade_id = account_trade_price.trade_id " +
            "WHERE trade_id = #{trade_Id} ORDER BY account_trade_status.trade_time DESC LIMIT 1")
    AccountTrade findById(Long trade_Id);

    // current state of each of the account's trades (one row per trade)
    @Select("SELECT DISTINCT ON (trade_id) " + TRADE_COLUMNS + STATUS_COLUMNS + PRICE_COLUMNS + " FROM account_trades " +
            "LEFT JOIN account_trade_status ON account_trades.trade_id = account_trade_status.trade_id " +
            "LEFT JOIN account_trade_price ON account_trades.trade_id = account_trade_price.trade_id " +
            "WHERE account_id = #{account_Id} " +
            "ORDER BY trade_id, trade_time DESC")
    List<AccountTrade> findByAccountId(Long account_Id);

    // every status a trade went through, oldest first
    @Select("SELECT " + TRADE_COLUMNS + STATUS_COLUMNS + PRICE_COLUMNS + " FROM account_trades " +
            "LEFT JOIN account_trade_status ON account_trades.trade_id = account_trade_status.trade_id " +
            "LEFT JOIN account_trade_price ON account_trades.trade_id = account_trade_price.trade_id " +
            "WHERE trade_id = #{trade_Id} ORDER BY trade_time")
    List<AccountTrade> findHistoryById(Long trade_Id);

    // #{...} are AccountTrade field names
    @Insert("INSERT INTO account_trades(account_id, instrument_id, trade_side, quantity) VALUES (#{accountId}, #{instrumentId}, #{tradeSide}::trade_side, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "tradeId", keyColumn = "trade_id")
    Long insert(TradeValidatedDTO tradeValidatedDTO);

    // Postgres INSERT and the RETURNING goes through @Select so MyBatis returns the generated trade_id
    @Select("INSERT INTO account_trades (trade_time, account_id, instrument_id, trade_side, quantity) "
            + "VALUES (clock_timestamp(), #{accountId}, #{instrumentId}, #{tradeType}::trade_side, #{quantity}) "
            + "RETURNING trade_id")
    Long insertTrade(@Param("accountId") Long accountId, @Param("instrumentId") Long instrumentId,
                     @Param("tradeSide") TradeSide tradeSide, @Param("quantity") BigDecimal quantity);

    // record the status change: copies the trade's latest row with the new status and the current time
    @Insert("INSERT INTO account_trade_status "
            + "(trade_id, trade_time, status) "
            + "SELECT trade_id, clock_timestamp(), #{status}::trade_status "
            + "FROM account_trades WHERE trade_id = #{tradeId} "
            + "ORDER BY trade_time DESC LIMIT 1")
    void insertStatus(@Param("tradeId") Long tradeId, @Param("status") TradeStatus status);
}

