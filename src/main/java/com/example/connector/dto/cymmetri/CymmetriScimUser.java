package com.example.connector.dto.cymmetri;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Minimal SCIM-compatible user payload for Cymmetri create/update.
 * Add or remove attributes as required by Cymmetri SCIM contract.
 *
 * Example produced JSON:
 * {
 *   "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
 *   "externalId": "wso2-id-123",
 *   "userName": "jdoe",
 *   "name": { "givenName":"John", "familyName":"Doe" },
 *   "emails": [ { "value":"john@company.com", "primary": true } ],
 *   "active": true,
 *   "displayName": "John Doe"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CymmetriScimUser {
    private List<String> schemas;
    private String externalId;
    private String userName;
    private Map<String, String> name; // keys: givenName, familyName
    private List<Map<String, Object>> emails; // list of { "value": "...", "primary": true }
    private Boolean active;
    private String displayName;
    private String locale;
    private String preferredLanguage;
    private String title;
    private String profileUrl;
    private String timezone;
    private String nickName;
    private String userType;

    // Getters / setters

    public List<String> getSchemas() { return schemas; }
    public void setSchemas(List<String> schemas) { this.schemas = schemas; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Map<String, String> getName() { return name; }
    public void setName(Map<String, String> name) { this.name = name; }

    public List<Map<String, Object>> getEmails() { return emails; }
    public void setEmails(List<Map<String, Object>> emails) { this.emails = emails; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
