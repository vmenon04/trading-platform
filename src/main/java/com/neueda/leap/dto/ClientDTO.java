package com.neueda.leap.dto;

import java.time.LocalDate;
import java.util.List;

public class ClientDTO {
    private int clientId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private List<AccountDTO> clientAccounts;
    
    public ClientDTO(int clientId, String firstName, String lastName, LocalDate birthDate, List<AccountDTO> clientAccounts) {
        this.clientId = clientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.clientAccounts = clientAccounts;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public List<AccountDTO> getClientAccounts() {
        return clientAccounts;
    }

    public void addClientAccount(AccountDTO accountDTO) {
        this.clientAccounts.add(accountDTO);
    }

    public void removeClientAccount(AccountDTO accountDTO) {
        this.clientAccounts.remove(accountDTO);
    }
}
