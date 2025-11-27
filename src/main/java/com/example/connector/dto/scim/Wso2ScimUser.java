package com.example.connector.dto.scim;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Wso2ScimUser:
 * Strict SCIM 2.0 model received from WSO2.
 * Supports both pull (with string/email deserialization) and push.
 */
@Data
public class Wso2ScimUser {

    private String id;
    private String externalId;
    private String userName;
    private Boolean active;

    private Name name;

    @JsonDeserialize(using = EmailListDeserializer.class)
    private List<Email> emails;

    private List<Group> groups;
    private Meta meta;

    // New fields to match your response
    private String birthDate; // date_of_birth
    private String gender; // gender_identity
    private Boolean terminated; // is_terminated
    private Boolean onLeave; // is_on_leave
    private String department;
    private String location;
    private String branchGroup; // maps to "group" in your output
    private String position;
    private String employmentStatus;
    private String avatarUrl;
    private String startedAt;
    private String finishedAt;

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

        public Email() {
        }

        public Email(String value) {
            this.value = value;
        }
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
