package com.neueda.leap.repository;

import com.neueda.leap.entity.AccountSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface AccountSubscriptionMapper {
    // columns aliased to the AccountSubscription field names, so MyBatis fills the right fields
    String COLUMNS = "account_id AS accountId, model_portfolio_id AS modelPortfolioId, subscription_date AS subscriptionDate, status";

    @Select("SELECT " + COLUMNS + " FROM account_subscriptions WHERE account_id = #{account_Id} AND model_portfolio_id = #{model_Portfolio_Id}")
    AccountSubscription findByAccountAndModelPortfolio(@Param("account_Id") Integer account_Id,
                                                       @Param("model_Portfolio_Id") Integer model_Portfolio_Id);

    @Select("SELECT " + COLUMNS + " FROM account_subscriptions WHERE account_id = #{account_Id}")
    List<AccountSubscription> findByAccountId(Integer account_Id);

    // #{...} are AccountSubscription field names
    @Insert("INSERT INTO account_subscriptions(account_id, model_portfolio_id, subscription_date, status) VALUES(#{accountId}, #{modelPortfolioId}, #{subscriptionDate}, #{status})")
    void insert(AccountSubscription accountSubscription);

    @Update("UPDATE account_subscriptions SET status = #{status} WHERE account_id = #{accountId} AND model_portfolio_id = #{modelPortfolioId}")
    void update(AccountSubscription accountSubscription);

    @Delete("DELETE FROM account_subscriptions WHERE account_id = #{account_Id} AND model_portfolio_id = #{model_Portfolio_Id}")
    void delete(@Param("account_Id") Integer account_Id, @Param("model_Portfolio_Id") Integer model_Portfolio_Id);
}
