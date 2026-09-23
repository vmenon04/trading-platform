package com.neueda.leap.entity;

import java.time.LocalDate;
import java.util.List;

public class Client {
    private int client_id;
    private String first_name;
    private String last_name;
    private LocalDate birth_date;
    private List<Account> client_accounts;
}
