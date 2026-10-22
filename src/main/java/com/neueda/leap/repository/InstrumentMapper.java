package com.neueda.leap.repository;

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

    // columns aliased to the Instrument field names, so MyBatis fills the right fields
    // (asset_class holds the InstrumentType name, e.g. 'STOCK')
    String COLUMNS = "instrument_id AS instrumentId, name, ticker, asset_class AS instrumentType";

    @Select("SELECT " + COLUMNS + " FROM instruments WHERE instrument_id = #{instrumentId}")
    Instrument findById(Long instrumentId);

    @Select("SELECT " + COLUMNS + " FROM instruments")
    List<Instrument> findAll();

    @Select("SELECT " + COLUMNS + " FROM instruments WHERE ticker = #{ticker}")
    Instrument findByTicker(String ticker);

    // the id an instrument already has in our database, or null; the oldest row if a ticker appears twice
    @Select("SELECT instrument_id FROM instruments WHERE ticker = #{ticker} ORDER BY instrument_id LIMIT 1")
    Long findIdByTicker(String ticker);

    @Insert("INSERT INTO instruments(ticker, name, asset_class) VALUES(#{ticker}, #{name}, #{instrumentType})")
    @Options(useGeneratedKeys = true, keyProperty = "instrumentId", keyColumn = "instrument_id")
    void insert(Instrument instrument);

    @Update("UPDATE instruments SET name = #{name}, ticker = #{ticker}, asset_class = #{instrumentType} WHERE instrument_id = #{instrumentId}")
    void update(Instrument instrument);

    @Delete("DELETE FROM instruments WHERE instrument_id = #{instrumentId}")
    void delete(Long instrumentId);
}
