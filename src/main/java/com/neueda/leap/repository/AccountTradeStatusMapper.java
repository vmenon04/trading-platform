package com.neueda.leap.repository;

import com.neueda.leap.entity.AccountTradeStatus;
import com.neueda.leap.enums.TradeStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AccountTradeStatusMapper {

    String COLUMNS = "status, trade_time as tradeTime";

    @Select("SELECT status FROM account_trade_status WHERE trade_id = #{tradeId} ORDER BY trade_time DESC LIMIT 1")
    TradeStatus getTradeStatusByTradeId(Long tradeId);

    @Select("SELECT " + COLUMNS + " FROM account_trade_status WHERE trade_id = #{tradeId} ORDER BY trade_time")
    List<AccountTradeStatus> getTradeStatusHistoryByTradeId(Long tradeId);

    @Insert("INSERT INTO account_trade_status (trade_id, status, trade_time) VALUES (#{tradeId}, #{status}::trade_status, clock_timestamp())")
    void insertTradeStatus(Long tradeId, TradeStatus status);

}
