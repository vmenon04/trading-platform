package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.ModelPortfolio;
import java.util.List;


@Mapper
public interface ModelPortfolioMapper {
    @Select ("SELECT * FROM model_portfolios WHERE model_portoflio_id = #{model_Portoflio_Id}")
    ModelPortfolio findById(Integer model_Portoflio_Id);


    @Select("SELECT * FROM model_portfolios")
    List<ModelPortfolio> findAll();

    @Select("SELECT * FROM model_portfolios WHERE name = #{name}")
    ModelPortfolio findByName(String name);

    @Insert("INSERT INTO model_portfolios(name) VALUES(#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "model_Portoflio_Id")
    void insert(ModelPortfolio modelPortfolio);

    @Update("UPDATE model_portfolios SET name = #{name} WHERE model_portoflio_id = #{model_Portoflio_Id}")
    void update(ModelPortfolio modelPortfolio);

    @Delete("DELETE FROM model_portfolios WHERE model_portoflio_id = #{model_Portoflio_Id}")
    void delete(Integer model_Portoflio_Id);
}
