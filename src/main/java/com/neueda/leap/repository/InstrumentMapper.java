package com.neueda.leap.repository;

import com.neueda.leap.entity.Instrument;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// TODO: temporary code so tests work, we need to replace this with code from the feature/repository branch
@Mapper
public interface InstrumentMapper {
    Instrument findById(int instrumentId);

    Instrument findByTicker(String ticker);

    List<Instrument> findAll();
}
