package com.example.connector.dto.cymmetri;

import lombok.Data;

import java.util.List;

/**
 * CymmetriUser:
 * SCIM-compliant user for Cymmetri provisioning.
 * Missing fields are ignored by WebClient.
 */
@Data
public class CymmetriUser {

    private String id;
    private String externalId;
    private String userName;
    private String displayName;

    private Boolean active;
    private String title;
    private String timezone;
    private String preferredLanguage;
    private String locale;
    private String nickName;
    private String profileUrl;
    private String userType;

    private List<Group> groups;

    @Data
    public static class Group {
        private String value;
        private String display;
    }
}
