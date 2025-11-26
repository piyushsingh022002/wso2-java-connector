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

        if (node.isArray()) {
            for (JsonNode item : node) {
                Wso2ScimUser.Email email = new Wso2ScimUser.Email();
                if (item.has("value")) email.setValue(item.get("value").asText());
                if (item.has("type")) email.setType(item.get("type").asText());
                if (item.has("primary")) email.setPrimary(item.get("primary").asBoolean());
                emails.add(email);
            }
        } else if (node.isTextual()) {
            emails.add(new Wso2ScimUser.Email(node.asText()));
        }

        return emails;
    }
}
