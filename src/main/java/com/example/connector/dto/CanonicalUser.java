package com.example.connector.dto;

import lombok.Data;

import java.util.List;

/**
 * CanonicalUser:
 * A universal internal representation of a user.
 * ALL inbound providers (WSO2, Cymmetri) map into this.
 * ALL outbound requests are generated from this.
 *
 * Missing attributes are ignored.
 */
@Data
public class CanonicalUser {

    private String id;
    private String externalId;

    private String userName;
    private String displayName;
    private String title;
    private String locale;
    private String timezone;
    private String preferredLanguage;
    private String nickName;
    private String profileUrl;
    private String userType;

    private Boolean active;

    private List<CanonicalGroup> groups;
}
