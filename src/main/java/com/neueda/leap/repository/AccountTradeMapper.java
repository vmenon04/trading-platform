package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import com.neueda.leap.entity.*;

// a trade lives in three tables:
//   account_trades       - who, what, which side, how much
//   account_trade_status - one row per status change, with its time; the first row is when
//                          the trade was placed, the latest row is its current status
//   trade_total_price    - what it cost in total; the unit price is total / quantity
// we use clock_timestamp() rather than NOW() since NOW() is fixed for a whole transaction
@Mapper
public interface AccountTradeMapper {

    // each trade with its placed time, unit price and current status,
    // aliased to the AccountTrade field names
    String SELECT_TRADES = """
            SELECT t.trade_id      AS tradeId,
                   (SELECT MIN(s.trade_time) FROM account_trade_status s
                    WHERE s.trade_id = t.trade_id) AS tradeTime,
                   t.account_id    AS accountId,
                   t.instrument_id AS instrumentId,
                   t.trade_side    AS tradeType,
                   t.quantity,
                   p.total_price / t.quantity AS price,
                   (SELECT s.status FROM account_trade_status s
                    WHERE s.trade_id = t.trade_id
                    ORDER BY s.trade_time DESC LIMIT 1) AS tradeStatus
            FROM account_trades t
            LEFT JOIN trade_total_price p ON p.trade_id = t.trade_id
            """;

    @Select(SELECT_TRADES + "WHERE t.trade_id = #{trade_Id}")
    AccountTrade findById(Integer trade_Id);

    @Select(SELECT_TRADES + "WHERE t.account_id = #{account_Id} ORDER BY t.trade_id")
    List<AccountTrade> findByAccountId(Integer account_Id);

    // one row per status the trade went through, oldest first; tradeTime is when that status was recorded
    @Select("""
            SELECT t.trade_id      AS tradeId,
                   s.trade_time    AS tradeTime,
                   t.account_id    AS accountId,
                   t.instrument_id AS instrumentId,
                   t.trade_side    AS tradeType,
                   t.quantity,
                   p.total_price / t.quantity AS price,
                   s.status        AS tradeStatus
            FROM account_trades t
            JOIN account_trade_status s ON s.trade_id = t.trade_id
            LEFT JOIN trade_total_price p ON p.trade_id = t.trade_id
            WHERE t.trade_id = #{trade_Id}
            ORDER BY s.trade_time
            """)
    List<AccountTrade> findHistoryById(Integer trade_Id);

    // saves a new trade and returns its trade_id.
    // goes through @Select so MyBatis hands back the RETURNING value
    @Select("""
            INSERT INTO account_trades (account_id, instrument_id, trade_side, quantity)
            VALUES (#{accountId}, #{instrumentId}, #{tradeSide}, #{quantity})
            RETURNING trade_id
            """)
    int insertTradeRow(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                       @Param("tradeSide") String tradeSide, @Param("quantity") BigDecimal quantity);

    @Insert("""
            INSERT INTO account_trade_status (trade_id, status, trade_time)
            VALUES (#{tradeId}, #{status}, clock_timestamp())
            """)
    void insertStatus(@Param("tradeId") int tradeId, @Param("status") String status);

    @Insert("INSERT INTO trade_total_price (trade_id, total_price) VALUES (#{tradeId}, #{total})")
    void insertTotal(@Param("tradeId") int tradeId, @Param("total") BigDecimal total);

    // saves the trade, its first status (which records when it was placed) and its total
    default int insertTrade(int accountId, int instrumentId, String tradeSide,
                            BigDecimal quantity, BigDecimal price, String status) {
        int tradeId = insertTradeRow(accountId, instrumentId, tradeSide, quantity);
        insertStatus(tradeId, status);
        if (price != null) {
            insertTotal(tradeId, price.multiply(quantity));
        }
        return tradeId;
    }

    @Delete("DELETE FROM account_trade_status WHERE trade_id = #{trade_Id}")
    void deleteStatuses(Integer trade_Id);

    @Delete("DELETE FROM trade_total_price WHERE trade_id = #{trade_Id}")
    void deleteTotal(Integer trade_Id);

    @Delete("DELETE FROM account_trades WHERE trade_id = #{trade_Id}")
    void deleteTradeRow(Integer trade_Id);

    // status and total rows point at the trade, so they are deleted first
    default void deleteById(Integer trade_Id) {
        deleteStatuses(trade_Id);
        deleteTotal(trade_Id);
        deleteTradeRow(trade_Id);
    }
}
