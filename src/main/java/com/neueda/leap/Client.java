package com.neueda.leap;

import java.util.List;

public class Client {
    private int client_id;
    private String name; // i think we should split this up into first name / last name
    private String birth_date; // store this as a LocalDate instead of a String
    private List<Client> client_accounts;
}
