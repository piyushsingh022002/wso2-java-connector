package com.example.connector.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.example.connector.dto.*;
import com.example.connector.dto.cymmetri.CymmetriUser;
import com.example.connector.dto.scim.Wso2ScimUser;
import com.example.connector.dto.scim.ScimUser;
import com.example.connector.dto.scim.ScimUserRequest;

import org.springframework.stereotype.Component;

import java.util.Collections;
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

    // // Canonical → Generic SCIM (Wso2)
    // public ScimUserRequest toScim(CanonicalUser canonical) {
    // ScimUserRequest req = new ScimUserRequest();

    // req.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
    // req.setUserName(canonical.getUserName());
    // req.setPassword(canonical.getPassword());

    // // --- name mapping ---
    // ScimUserRequest.Name name = new ScimUserRequest.Name();

    // // givenName from displayName
    // name.setGivenName(canonical.getDisplayName() != null ?
    // canonical.getDisplayName() : canonical.getUserName());

    // // familyName = same (since canonical has no last name)
    // name.setFamilyName(canonical.getDisplayName() != null ?
    // canonical.getDisplayName() : canonical.getUserName());

    // req.setName(name);

    // // --- email mapping ---
    // ScimUserRequest.Email email = new ScimUserRequest.Email();
    // email.setPrimary(true);

    // // canonical user has no email → generate fallback
    // String emailValue = canonical.getUserName() + "@example.com";
    // email.setValue(emailValue);

    // req.setEmails(Collections.singletonList(email));

    // return req;
    // }

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

    // from cymmetri to wso2

    public ScimUserRequest toScim(IncomingUser in) {
        ScimUserRequest req = new ScimUserRequest();

        req.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));

        // Username → priority order
        String username = firstNonNull(
                in.getPortal_username(),
                in.getEmail(),
                in.getCode());
        req.setUserName(username);

        // Password → your existing SCIM mapper uses fallback
        req.setPassword("Temp123!"); // or drop if your existing mapper handles fallback

        // --- NAME ---
        ScimUserRequest.Name name = new ScimUserRequest.Name();
        String display = firstNonNull(
                in.getFull_name(),
                in.getFirst_names(),
                in.getPortal_username(),
                username);
        name.setGivenName(display);
        name.setFamilyName(display);
        req.setName(name);

        // --- EMAIL ---
        ScimUserRequest.Email email = new ScimUserRequest.Email();
        email.setPrimary(true);
        email.setValue(
                firstNonNull(in.getEmail(), username + "@example.com"));
        req.setEmails(Collections.singletonList(email));

        return req;
    }

    private String firstNonNull(String... vals) {
        if (vals == null)
            return null;
        for (String v : vals) {
            if (v != null && !v.isBlank())
                return v;
        }
        return null;
    }

}
