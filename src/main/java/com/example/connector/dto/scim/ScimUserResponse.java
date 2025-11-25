package com.example.connector.dto.scim;

import lombok.Data;

import java.util.List;

@Data
public class ScimUserResponse {

    private String id;
    private String externalId;
    private String userName;
    private Boolean active;

    private List<Group> groups;

    @Data
    public static class Group {
        private String value;
        private String display;
    }
}
