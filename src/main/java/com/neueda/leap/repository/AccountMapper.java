package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.neueda.leap.entity.Account;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

//an account can be created, all accounts need to be found for a client, and an account can be updated or deleted.
@Mapper
public interface AccountMapper {

    // columns aliased to the Account field names, so MyBatis fills the right fields
    // (accounts has no client_id column, so clientId is not filled here; the link is in client_accounts)
    String COLUMNS = "account_id AS accountId, account_type AS accountType, balance";

    @Select("SELECT " + COLUMNS + " FROM accounts WHERE account_id = #{account_Id}")
    Account findById(Integer account_Id);

    @Select("SELECT " + COLUMNS + " FROM accounts")
    List<Account> findAll();

    // #{...} are Account field names
    @Insert("INSERT INTO accounts(account_type, balance) VALUES(#{accountType}, #{balance})")
    @Options(useGeneratedKeys = true, keyProperty = "accountId", keyColumn = "account_id")
    void insert(Account account);

    @Update("UPDATE accounts SET account_type = #{accountType}, balance = #{balance} WHERE account_id = #{accountId}")
    void update(Account account);

    @Delete("DELETE FROM accounts WHERE account_id = #{account_Id}")
    void delete(Integer account_Id);

    // Vasu's additions for accountmapper

    @Select("SELECT balance FROM accounts WHERE account_id = #{accountId}")
    BigDecimal findBalance(int accountId);

    //returns rows updated: 0 = no such account
    @Update("UPDATE accounts SET balance = balance + #{amount} WHERE account_id = #{accountId}")
    int increaseBalance(@Param("accountId") int accountId, @Param("amount") BigDecimal amount);

    // returns rows updated: 0 = no such account OR insufficient funds (we check and update in one statement)
    @Update("UPDATE accounts SET balance = balance - #{amount} "
            + "WHERE account_id = #{accountId} AND balance >= #{amount}")
    int reduceBalance(@Param("accountId") int accountId, @Param("amount") BigDecimal amount);
}
