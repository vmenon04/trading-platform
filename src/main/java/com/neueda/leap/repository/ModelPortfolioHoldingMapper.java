package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import com.neueda.leap.entity.ModelPortfolioHolding;
import java.util.List;

@Mapper
public interface ModelPortfolioHoldingMapper {

    @Select("SELECT * FROM model_portfolio_holdings WHERE model_portfolio_holding_id = #{model_Portfolio_Holding_Id} AND instrument_id = #{instrument_Id} AND effective_date = #{effective_Date}")
    ModelPortfolioHolding findByIdAndInstrumentAndEffectiveDate(Integer model_Portfolio_Holding_Id, Integer instrument_Id, String effective_Date);
    
    @Select("SELECT * FROM model_portfolio_holdings WHERE model_portfolio_holding_id = #{model_Portfolio_Holding_Id}")
    List<ModelPortfolioHolding> findAll();

    @Insert("INSERT INTO model_portfolio_holdings(model_portfolio_holding_id, instrument_id, effective_date, target_weight_pct, status) VALUES(#{model_Portfolio_Holding_Id}, #{instrument_Id}, #{effective_Date}, #{target_Weight_Pct}, #{status})")
    void insert(ModelPortfolioHolding modelPortfolioHolding);

    @Update("UPDATE model_portfolio_holdings SET target_weight_pct = #{target_Weight_Pct}, status = #{status} WHERE model_portfolio_holding_id = #{model_Portfolio_Holding_Id} AND instrument_id = #{instrument_Id} AND effective_date = #{effective_Date}")
    void update(ModelPortfolioHolding modelPortfolioHolding);

    @Delete("DELETE FROM model_portfolio_holdings WHERE model_portfolio_holding_id = #{model_Portfolio_Holding_Id} AND instrument_id = #{instrument_Id} AND effective_date = #{effective_Date}")
    void delete(Integer model_Portfolio_Holding_Id, Integer instrument_Id, String effective_Date);
}
