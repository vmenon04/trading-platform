package com.neueda.leap.repository;

import org.apache.ibatis.annotations.*;
import com.neueda.leap.entity.Client;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ClientMapper {

    // columns aliased to the Client field names, so MyBatis fills the right fields
    String COLUMNS = "client_id AS clientId, external_client_id AS externalClientId, first_name AS firstName, last_name AS lastName, email, birth_date AS birthDate";

    // #{...} are Client field names
    @Insert("INSERT INTO clients(first_name, last_name, email, birth_date) VALUES(#{firstName}, #{lastName}, #{email}, #{birthDate})")
    @Options(useGeneratedKeys = true, keyProperty = "clientId", keyColumn = "client_id")
    void insert(Client client);

    @Select("SELECT " + COLUMNS + " FROM clients WHERE client_id = #{clientId}")
    Client findById(Long clientId);

    @Select("SELECT client_id as clientId FROM clients WHERE external_client_id = #{externalClientId}")
    Long findInternalIdByExternalClientId(@Param("externalClientId") UUID externalClientId);

    @Select("SELECT accounts.external_account_id::text FROM accounts "
        + "JOIN client_accounts ON accounts.account_id = client_accounts.account_id "
        + "WHERE client_accounts.client_id = #{clientId} "
        + "ORDER BY accounts.external_account_id")
    List<String> findExternalAccountIdStringsByClientId(@Param("clientId") Long clientId);

    default List<UUID> findExternalAccountIdsByClientId(Long clientId) {
        return findExternalAccountIdStringsByClientId(clientId).stream()
                .map(UUID::fromString)
                .toList();
    }

    @Select("SELECT " + COLUMNS + " FROM clients")
    List<Client> findAll();

    @Update("UPDATE clients SET first_name = #{firstName}, last_name = #{lastName}, email = #{email}, birth_date = #{birthDate} WHERE client_id = #{clientId}")
    void update(Client client);

    @Delete("DELETE FROM clients WHERE client_id = #{clientId}")
    void delete(Long clientId);
}
