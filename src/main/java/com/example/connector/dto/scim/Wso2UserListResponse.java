package com.example.connector.dto.scim;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class Wso2UserListResponse {

    private int totalResults;

    @JsonProperty("Resources") 
    private List<Wso2ScimUser> resources; // WSO2 uses capital "R"
}
