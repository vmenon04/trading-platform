package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.Instrument;
import java.util.List;

@Mapper 
public interface InstrumentMapper {

    @Select("SELECT * FROM instruments WHERE instrument_id = #{instrument_Id}")
    Instrument findById(Integer instrument_Id);
    
    @Select("SELECT * FROM instruments")
    List<Instrument> findAll();

    @Select("SELECT * FROM instruments WHERE ticker = #{ticker}")
    Instrument findByTicker(String ticker);

    @Insert("INSERT INTO instruments(ticker, name, asset_class) VALUES(#{ticker}, #{name}, #{asset_Class})")
    @Options(useGeneratedKeys = true, keyProperty = "instrument_Id")
    void insert(Instrument instrument);

    @Update("UPDATE instruments SET name = #{name}, ticker = #{ticker}, asset_class = #{asset_Class} WHERE instrument_id = #{instrument_Id}")
    void update(Instrument instrument);

    @Delete("DELETE FROM instruments WHERE instrument_id = #{instrument_Id}")
    void delete(Integer instrument_Id);
}
