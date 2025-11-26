package com.example.connector.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.example.connector.dto.*;
import com.example.connector.dto.cymmetri.CymmetriUser;
import com.example.connector.dto.scim.Wso2ScimUser;
import com.example.connector.dto.scim.ScimUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserMapper:
 * - WSO2 SCIM → Canonical → Cymmetri
 * - Missing attributes ignored
 * - Enforces externalId presence
 */
@Component
public class UserMapper {

    // ----------------------------------
    // SCIM (WSO2) → Canonical
    // ----------------------------------
    public CanonicalUser fromWso2(Wso2ScimUser s) {

        CanonicalUser c = new CanonicalUser();

        c.setId(s.getId());
        c.setExternalId(s.getExternalId());
        c.setActive(s.getActive());
        c.setUserName(s.getUserName());

        if (s.getName() != null) {
            c.setDisplayName(s.getName().getGivenName() + " " + s.getName().getFamilyName());
        }

        if (s.getGroups() != null) {
            c.setGroups(
                    s.getGroups().stream()
                            .map(g -> {
                                CanonicalGroup cg = new CanonicalGroup();
                                cg.setValue(g.getValue());
                                cg.setDisplay(g.getDisplay());
                                return cg;
                            })
                            .collect(Collectors.toList()));
        }

        return c;
    }

    // ----------------------------------
    // Canonical → WSO2 SCIM
    // ----------------------------------
    public Wso2ScimUser toWso2(CanonicalUser c) {

        if (c.getExternalId() == null || c.getExternalId().isBlank()) {
            throw new IllegalStateException("externalId is REQUIRED for WSO2 operations");
        }

        Wso2ScimUser user = new Wso2ScimUser();

        user.setId(c.getId());
        user.setExternalId(c.getExternalId());
        user.setUserName(c.getUserName());
        user.setActive(c.getActive());

        if (c.getDisplayName() != null) {
            String[] parts = c.getDisplayName().split(" ", 2);
            Wso2ScimUser.Name name = new Wso2ScimUser.Name();
            name.setGivenName(parts.length > 0 ? parts[0] : "");
            name.setFamilyName(parts.length > 1 ? parts[1] : "");
            user.setName(name);
        }

        if (c.getGroups() != null) {
            user.setGroups(
                    c.getGroups().stream().map(g -> {
                        Wso2ScimUser.Group wg = new Wso2ScimUser.Group();
                        wg.setValue(g.getValue());
                        wg.setDisplay(g.getDisplay());
                        return wg;
                    }).collect(Collectors.toList()));
        }

        return user;
    }

    // ----------------------------------
    // Canonical → Generic SCIM (ScimUser)
    // ----------------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ScimUser toScim(CanonicalUser c) {
        ScimUser user = new ScimUser();

        user.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
        user.setUserName(c.getUserName());
        user.setActive(c.getActive() != null ? c.getActive() : true);

        // Set password
        user.setPassword(c.getPassword()); // <-- must be included

        // Name
        String displayName = c.getDisplayName() != null ? c.getDisplayName() : c.getUserName();
        String[] parts = displayName.split(" ", 2);
        ScimUser.Name name = new ScimUser.Name();
        name.setGivenName(parts[0]);
        name.setFamilyName(parts.length > 1 ? parts[1] : "");
        user.setName(name);

        // Emails
        ScimUser.Email email = new ScimUser.Email();
        email.setValue(c.getUserName() + "@example.com");
        email.setPrimary(true);
        user.setEmails(List.of(email));

        // Set empty groups array
        user.setGroups(List.of());

        return user;
    }

    // ----------------------------------
    // Canonical → Cymmetri
    // ----------------------------------
    public CymmetriUser toCymmetri(CanonicalUser c) {

        if (c.getExternalId() == null || c.getExternalId().isBlank()) {
            throw new IllegalStateException("externalId is REQUIRED for Cymmetri operations");
        }

        CymmetriUser user = new CymmetriUser();

        user.setId(c.getId());
        user.setExternalId(c.getExternalId());
        user.setUserName(c.getUserName());
        user.setDisplayName(c.getDisplayName());
        user.setActive(c.getActive());

        user.setLocale(c.getLocale());
        user.setTitle(c.getTitle());
        user.setNickName(c.getNickName());
        user.setUserType(c.getUserType());
        user.setProfileUrl(c.getProfileUrl());
        user.setTimezone(c.getTimezone());
        user.setPreferredLanguage(c.getPreferredLanguage());

        if (c.getGroups() != null) {
            user.setGroups(
                    c.getGroups().stream().map(g -> {
                        CymmetriUser.Group cg = new CymmetriUser.Group();
                        cg.setValue(g.getValue());
                        cg.setDisplay(g.getDisplay());
                        return cg;
                    }).collect(Collectors.toList()));
        }

        return user;
    }
}
