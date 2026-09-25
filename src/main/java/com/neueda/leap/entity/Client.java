package com.neueda.leap.entity;

import java.time.LocalDate;
import java.util.ArrayList;

public class Client {
    private Long clientId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private ArrayList<Account> clientAccounts;

    public Client() {
        this.clientAccounts = new ArrayList<>();
    }

    public Client(String firstName, String lastName, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.clientAccounts = new ArrayList<>();
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public ArrayList<Account> getClientAccounts() {
        return clientAccounts;
    }

    public void setClientAccounts(ArrayList<Account> clientAccounts) {
        this.clientAccounts = clientAccounts;
    }
}
