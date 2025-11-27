package com.example.connector.dto.scim;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmailListDeserializer extends JsonDeserializer<List<Wso2ScimUser.Email>> {

    @Override
    public List<Wso2ScimUser.Email> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        List<Wso2ScimUser.Email> emails = new ArrayList<>();
        JsonNode node = p.getCodec().readTree(p);

        // Case 1: emails is an array of objects (with value, type, primary)
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isObject()) {
                    Wso2ScimUser.Email email = new Wso2ScimUser.Email();
                    if (item.has("value"))
                        email.setValue(item.get("value").asText());
                    if (item.has("type"))
                        email.setType(item.get("type").asText());
                    if (item.has("primary"))
                        email.setPrimary(item.get("primary").asBoolean());
                    emails.add(email);
                } else if (item.isTextual()) {
                    // Case 2: emails is a simple array of strings
                    emails.add(new Wso2ScimUser.Email(item.asText())); // Simple email string
                }
            }
        } else if (node.isTextual()) {
            // Case 3: Single email string
            emails.add(new Wso2ScimUser.Email(node.asText())); // Single email string
        }

        return emails;
    }
}
