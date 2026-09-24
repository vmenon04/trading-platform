package com.neueda.leap.repository;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;

// TODO: temporary code so tests work, we need to replace this with code from the feature/repository branch
@Mapper
public interface AccountMapper {
    BigDecimal findBalance(int accountId);

    void updateBalance(int accountId, BigDecimal newBalance);
}
