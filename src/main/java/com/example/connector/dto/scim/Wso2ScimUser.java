package com.example.connector.dto.scim;

import lombok.Data;

import java.util.List;

/**
 * Wso2ScimUser:
 * Strict SCIM 2.0 model received from WSO2.
 */
@Data
public class Wso2ScimUser {

    private String id;
    private String externalId;
    private String userName;
    private Boolean active;

    private Name name;
    private List<Email> emails;
    private List<Group> groups;
    private Meta meta;

    @Data
    public static class Name {
        private String givenName;
        private String familyName;
    }

    @Data
    public static class Email {
        private String value;
        private String type;
        private Boolean primary;
    }

    @Data
    public static class Group {
        private String value;
        private String display;
    }

    @Data
    public static class Meta {
        private String created;
        private String lastModified;
        private String location;
        private String resourceType;
    }
}
