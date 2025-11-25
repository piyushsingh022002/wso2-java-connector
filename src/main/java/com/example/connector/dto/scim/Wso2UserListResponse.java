package com.example.connector.dto.scim;

import lombok.Data;
import java.util.List;

@Data
public class Wso2UserListResponse {
    private int totalResults;
    private List<Wso2ScimUser> Resources; // WSO2 uses capital "R"
}
