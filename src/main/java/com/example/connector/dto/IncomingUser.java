package com.example.connector.dto;

import lombok.Data;

/**
 * Matches the JSON in your screenshot / example.
 * Field names intentionally follow your payload (snake_case) to match Jackson
 * defaults
 * — if you use camelCase in JSON, change fields accordingly or
 * add @JsonProperty.
 */
@Data
public class IncomingUser {
    private String code;
    private String date_of_birth;
    private String gender_identity;
    private String started_at;

    private Boolean is_active;
    private Boolean is_terminated;
    private Boolean can_logon;

    private String portal_username;
    private String department;
    private String location;
    private String position;
    private String employment_status;
    private String first_names;
    private String full_name;
    private String email;
}
