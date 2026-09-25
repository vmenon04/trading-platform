package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// The 'active' row is current holding and older rows are 'inactive'
// we use clock_timestamp() rather than NOW() since NOW() is fixed for a whole transaction
@Mapper
public interface AccountHoldingMapper {
    // get quantity of the current (active) holding
    @Select("SELECT quantity FROM account_holdings "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'active'")
    BigDecimal findActiveQuantity(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    // marks the current holding as inactive before we replace it
    @Update("UPDATE account_holdings SET status = 'inactive' "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'active'")
    void deactivateHolding(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    @Insert("INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status) "
            + "VALUES (#{accountId}, #{instrumentId}, clock_timestamp(), #{quantity}, #{status})")
    void insertSnapshot(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                        @Param("quantity") BigDecimal quantity, @Param("status") String status);
}
