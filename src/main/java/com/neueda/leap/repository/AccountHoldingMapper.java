package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import com.neueda.leap.entity.AccountHolding;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountHoldingMapper {

    @Select("SELECT * FROM account_holdings WHERE account_id = #{account_Id} AND instrument_id = #{instrument_Id} AND as_of_date = #{as_Of_Date}")
    AccountHolding findByAccountInstrumentAndDate(Integer account_Id, Integer instrument_Id, String as_Of_Date);

    @Select("SELECT * FROM account_holdings WHERE account_id = #{account_Id}")
    List<AccountHolding> findByAccountId(Integer account_Id);

    @Insert("INSERT INTO account_holdings(account_id, instrument_id, quantity, as_of_date, status) VALUES(#{account_Id}, #{instrument_Id}, #{quantity}, #{as_Of_Date}, #{status})")
    void insert(AccountHolding accountHolding);

    @Update("UPDATE account_holdings SET quantity = #{quantity}, status = #{status} WHERE account_id = #{account_Id} AND instrument_id = #{instrument_Id} AND as_of_date = #{as_Of_Date}")
    void update(AccountHolding accountHolding);

    @Delete("DELETE FROM account_holdings WHERE account_id = #{account_Id} AND instrument_id = #{instrument_Id} AND as_of_date = #{as_Of_Date}")
    void delete(Integer account_Id, Integer instrument_Id, String as_Of_Date);

    //vasus additions for accountholdingmapper
     @Select("SELECT quantity FROM account_holdings "
            + "WHERE account_id = #{accountId} AND instrument_id = #{instrumentId} "
            + "ORDER BY as_of_date DESC LIMIT 1")
    BigDecimal findLatestQuantity(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId);

    @Insert("INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status) "
            + "VALUES (#{accountId}, #{instrumentId}, NOW(), #{quantity}, #{status})")
    void insertSnapshot(@Param("accountId") int accountId, @Param("instrumentId") int instrumentId,
                        @Param("quantity") BigDecimal quantity, @Param("status") String status);
}
