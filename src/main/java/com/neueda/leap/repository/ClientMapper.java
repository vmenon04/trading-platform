package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.Client;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ClientMapper {

    // columns aliased to the Client field names, so MyBatis fills the right fields
    String COLUMNS = "client_id AS clientId, first_name AS firstName, last_name AS lastName, email, birth_date AS birthDate";

    // #{...} are Client field names
    @Insert("INSERT INTO clients(first_name, last_name, email, birth_date) VALUES(#{firstName}, #{lastName}, #{email}, #{birthDate})")
    @Options(useGeneratedKeys = true, keyProperty = "clientId", keyColumn = "client_id")
    void insert(Client client);

    @Select("SELECT " + COLUMNS + " FROM clients WHERE client_id = #{client_Id}")
    Client findById(Integer client_Id);

    @Select("SELECT " + COLUMNS + " FROM clients")
    List<Client> findAll();

    @Update("UPDATE clients SET first_name = #{firstName}, last_name = #{lastName}, email = #{email}, birth_date = #{birthDate} WHERE client_id = #{clientId}")
    void update(Client client);

    @Delete("DELETE FROM clients WHERE client_id = #{client_Id}")
    void delete(Integer client_Id);
}
