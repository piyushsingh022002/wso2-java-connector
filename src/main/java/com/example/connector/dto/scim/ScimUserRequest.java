package com.example.connector.dto.scim;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScimUserRequest {
    private List<String> schemas = new ArrayList<>();
    private String userName;
    private String password;

    private Name name;
    private List<Email> emails = new ArrayList<>();

    @Data
    public static class Name {
        private String givenName;
        private String familyName;
    }

    @Data
    public static class Email {
        private Boolean primary;
        private String value;
    }
}
