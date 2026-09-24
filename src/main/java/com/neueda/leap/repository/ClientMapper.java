package com.neueda.leap.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.leap.entity.Client;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper 
public interface ClientMapper {
    
    @Insert("INSERT INTO clients(name, email) VALUES(#{name}, #{email})")
    void insert(Client client);

    @Select("SELECT * FROM clients WHERE client_id = #{client_Id}")
    Client findById(Integer client_Id);

    @Select("SELECT * FROM clients")
    List<Client> findAll();

    @Update("UPDATE clients SET name = #{name}, email = #{email} WHERE client_id = #{client_Id}")
    void update(Client client); 

    @Delete("DELETE FROM clients WHERE client_id = #{client_Id}")
    void delete(Integer client_Id);
}
