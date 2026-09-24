package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountHoldingMapper {
    // get quantity from the latest snapshot
    @Select("SELECT quantity FROM account_holdings "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} "
            + "ORDER BY as_of_date DESC LIMIT 1")
    BigDecimal findLatestQuantity(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    @Insert("INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status) "
            + "VALUES (#{accountId}, #{instrumentId}, NOW(), #{quantity}, #{status})")
    void insertSnapshot(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                        @Param("quantity") BigDecimal quantity, @Param("status") String status);
}
