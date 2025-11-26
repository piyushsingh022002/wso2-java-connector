package com.example.connector.dto.scim;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ScimUser: DTO for creating/updating SCIM2 User payloads
 */
@Data
public class ScimUser {

    private List<String> schemas = new ArrayList<>(
            Arrays.asList("urn:ietf:params:scim:schemas:core:2.0:User")
    );

    private String userName;
    private String password;
    private Name name;
    private List<Email> emails;
    private Boolean active;
    private List<GroupRef> groups;

    @Data
    public static class Name {
        private String givenName;
        private String familyName;
    }

    @Data
    public static class Email {
        private String value;
        private Boolean primary;
    }

    @Data
    public static class GroupRef {
        private String value;
        private String display; // optional
    }
}
