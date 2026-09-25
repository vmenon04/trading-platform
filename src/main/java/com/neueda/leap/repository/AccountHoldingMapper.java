package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import com.neueda.leap.entity.AccountHolding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccountHoldingMapper {

    // columns aliased to the AccountHolding field names, so MyBatis fills the right fields
    String COLUMNS = "account_id AS accountId, instrument_id AS instrumentId, as_of_date AS asOfDate, quantity, status";

    @Select("SELECT " + COLUMNS + " FROM account_holdings WHERE account_id = #{account_Id} AND instrument_id = #{instrument_Id} AND as_of_date = #{as_Of_Date}")
    AccountHolding findByAccountInstrumentAndDate(@Param("account_Id") Integer account_Id,
                                                  @Param("instrument_Id") Integer instrument_Id,
                                                  @Param("as_Of_Date") LocalDateTime as_Of_Date);

    // every snapshot for the account, including INACTIVE history rows
    @Select("SELECT " + COLUMNS + " FROM account_holdings WHERE account_id = #{account_Id}")
    List<AccountHolding> findByAccountId(Integer account_Id);

//     @Insert("INSERT INTO account_holdings(account_id, instrument_id, quantity, as_of_date, status) VALUES(#{account_Id}, #{instrument_Id}, #{quantity}, #{as_Of_Date}, #{status})")
//     void insert(AccountHolding accountHolding);

    // #{...} are AccountHolding field names
    @Update("UPDATE account_holdings SET quantity = #{quantity}, status = #{status} WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND as_of_date = #{asOfDate}")
    void update(AccountHolding accountHolding);

    @Delete("DELETE FROM account_holdings WHERE account_id = #{account_Id} AND instrument_id = #{instrument_Id} AND as_of_date = #{as_Of_Date}")
    void delete(@Param("account_Id") Integer account_Id, @Param("instrument_Id") Integer instrument_Id,
                @Param("as_Of_Date") LocalDateTime as_Of_Date);

    //vasus additions for accountholdingmapper
    // The 'ACTIVE' row is the current holding; older rows are 'INACTIVE' history
    // we use clock_timestamp() rather than NOW() since NOW() is fixed for a whole transaction
    // get quantity of the current (active) holding; null if there is none
    @Select("SELECT quantity FROM account_holdings "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'ACTIVE'")
    BigDecimal findActiveQuantity(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    // marks the current holding as inactive before we replace it
    @Update("UPDATE account_holdings SET status = 'INACTIVE' "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} AND status = 'ACTIVE'")
    void deactivateHolding(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    @Insert("INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status) "
            + "VALUES (#{accountId}, #{instrumentId}, clock_timestamp(), #{quantity}, #{status})")
    void insertSnapshot(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                        @Param("quantity") BigDecimal quantity, @Param("status") String status);
}
