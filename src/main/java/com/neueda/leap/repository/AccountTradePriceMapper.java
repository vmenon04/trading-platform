package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountTradePriceMapper {

    String COLUMNS = "price_per_unit, total_price";

    @Select("SELECT " + COLUMNS + " FROM account_trade_price WHERE trade_id = #{tradeId}")
    // TODO: selects two columns into one Double; prices should be BigDecimal to match the NUMERIC columns
    Double getTradePriceByTradeId(Long tradeId);

    // TODO: the prices should be BigDecimal, not Double
    @Insert("INSERT INTO account_trade_price (trade_id, price_per_unit, total_price) VALUES (#{tradeId}, #{pricePerUnit}, #{totalPrice})")
    void insertTradePrice(Long tradeId, Double pricePerUnit, Double totalPrice);

}
