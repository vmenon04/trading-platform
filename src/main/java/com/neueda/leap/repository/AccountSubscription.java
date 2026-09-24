package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper 
public interface AccountSubscription {
    @Select("SELECT * FROM account_subscriptions WHERE account_id = #{account_Id} AND model_portfolio_id = #{model_Portfolio_Id}")
    AccountSubscription findByAccountAndModelPortfolio(Integer account_Id, Integer model_Portfolio_Id);

    @Select("SELECT * FROM account_subscriptions WHERE account_id = #{account_Id}")
    List<AccountSubscription> findByAccountId(Integer account_Id);

    @Insert("INSERT INTO account_subscriptions(account_id, model_portfolio_id, subscription_date, status) VALUES(#{account_Id}, #{model_Portfolio_Id}, #{subscription_Date}, #{status})")
    void insert(AccountSubscription accountSubscription);

    @Update("UPDATE account_subscriptions SET status = #{status} WHERE account_id = #{account_Id} AND model_portfolio_id = #{model_Portfolio_Id}")
    void update(AccountSubscription accountSubscription);

    @Delete("DELETE FROM account_subscriptions WHERE account_id = #{account_Id} AND model_portfolio_id = #{model_Portfolio_Id}")
    void delete(Integer account_Id, Integer model_Portfolio_Id);
}
