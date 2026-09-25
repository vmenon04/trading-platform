package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import com.neueda.leap.entity.ModelPortfolioHolding;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ModelPortfolioHoldingMapper {

    // columns aliased to the ModelPortfolioHolding field names, so MyBatis fills the right fields
    String COLUMNS = "model_portfolio_id AS modelPortfolioId, instrument_id AS instrumentId, effective_date AS effectiveDate, "
            + "target_weight_pct AS targetWeightPct, status";

    @Select("SELECT " + COLUMNS + " FROM model_portfolio_holdings WHERE model_portfolio_id = #{model_Portfolio_Id} AND instrument_id = #{instrument_Id} AND effective_date = #{effective_Date}")
    ModelPortfolioHolding findByIdAndInstrumentAndEffectiveDate(@Param("model_Portfolio_Id") Integer model_Portfolio_Id,
                                                                @Param("instrument_Id") Integer instrument_Id,
                                                                @Param("effective_Date") LocalDate effective_Date);

    @Select("SELECT " + COLUMNS + " FROM model_portfolio_holdings")
    List<ModelPortfolioHolding> findAll();

    // #{...} are ModelPortfolioHolding field names
    @Insert("INSERT INTO model_portfolio_holdings(model_portfolio_id, instrument_id, effective_date, target_weight_pct, status) VALUES(#{modelPortfolioId}, #{instrumentId}, #{effectiveDate}, #{targetWeightPct}, #{status})")
    void insert(ModelPortfolioHolding modelPortfolioHolding);

    @Update("UPDATE model_portfolio_holdings SET target_weight_pct = #{targetWeightPct}, status = #{status} WHERE model_portfolio_id = #{modelPortfolioId} AND instrument_id = #{instrumentId} AND effective_date = #{effectiveDate}")
    void update(ModelPortfolioHolding modelPortfolioHolding);

    @Delete("DELETE FROM model_portfolio_holdings WHERE model_portfolio_id = #{model_Portfolio_Id} AND instrument_id = #{instrument_Id} AND effective_date = #{effective_Date}")
    void delete(@Param("model_Portfolio_Id") Integer model_Portfolio_Id, @Param("instrument_Id") Integer instrument_Id,
                @Param("effective_Date") LocalDate effective_Date);
}
