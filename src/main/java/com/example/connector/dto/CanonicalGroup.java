package com.example.connector.dto;

import lombok.Data;

/**
 * CanonicalGroup:
 * Internal neutral representation of groups.
 */
@Data
public class CanonicalGroup {
    private String value;      // group ID
    private String display;    // group name
}
