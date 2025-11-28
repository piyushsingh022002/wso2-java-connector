package com.example.connector.service;

import com.example.connector.config.Wso2Config;
import com.example.connector.dto.CanonicalUser;
import com.example.connector.dto.IncomingUser;
import com.example.connector.dto.cymmetri.CymmetriUser;
import com.example.connector.dto.scim.ScimUserRequest;
import com.example.connector.dto.scim.ScimUserResponse;
import com.example.connector.dto.scim.Wso2ScimUser;
import com.example.connector.dto.scim.Wso2UserListResponse;
import com.example.connector.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Flux;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisioningService {

    // ---------------------------------------------------------
    // INJECTED CLIENTS + CONFIG
    // ---------------------------------------------------------
    private final WebClient cymmetriClient;
    private final WebClient wso2Client;
    private final Wso2Config wso2Config;
    private final UserMapper userMapper;

    // =========================================================
    // UPSERT ENTRYPOINT
    // =========================================================
    public ScimUserResponse upsertUser(CymmetriUser user) {

        // ENFORCE active=true STRICTLY
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException(
                    "Rejected: externalId=" + user.getExternalId() +
                            " is inactive. Active=true is strictly required.");
        }

        boolean exists = cymmetriUserExists(user.getExternalId());

        return exists ? updateCymmetriUser(user) : createCymmetriUser(user);
    }

    // =========================================================
    // PARALLEL UPSERT FOR BULK PROCESSING
    // =========================================================
    public List<ScimUserResponse> upsertUsersParallel(List<CymmetriUser> users) {

        return users.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return upsertUser(user);
                    } catch (Exception ex) {
                        log.error("Bulk upsert failed for externalId={}", user.getExternalId(), ex);
                        throw ex;
                    }
                }))
                .map(CompletableFuture::join)
                .toList();
    }

    // =========================================================
    // CREATE USER â†’ CYMMETRI
    // =========================================================
    private ScimUserResponse createCymmetriUser(CymmetriUser user) {

        log.info("Creating Cymmetri user externalId={}", user.getExternalId());

        try {
            return cymmetriClient.post()
                    .uri("/scim2/Users")
                    .bodyValue(user)
                    .retrieve()
                    .bodyToMono(ScimUserResponse.class)
                    .block();

        } catch (WebClientResponseException ex) {
            log.error("Cymmetri CREATE failed externalId={}, status={}",
                    user.getExternalId(), ex.getStatusCode());
            throw ex;
        }
    }

    // =========================================================
    // UPDATE USER â†’ CYMMETRI
    // =========================================================
    private ScimUserResponse updateCymmetriUser(CymmetriUser user) {

        log.info("Updating Cymmetri user externalId={}", user.getExternalId());

        try {
            // IMPORTANT: This endpoint will be replaced after Cymmetri confirms its format.
            return cymmetriClient.put()
                    .uri("/scim2/Users/" + user.getExternalId())
                    .bodyValue(user)
                    .retrieve()
                    .bodyToMono(ScimUserResponse.class)
                    .block();

        } catch (WebClientResponseException ex) {
            log.error("Cymmetri UPDATE failed externalId={}, status={}",
                    user.getExternalId(), ex.getStatusCode());
            throw ex;
        }
    }

    // =========================================================
    // CHECK IF USER EXISTS IN CYMMETRI
    // =========================================================
    private boolean cymmetriUserExists(String externalId) {

        try {
            cymmetriClient.get()
                    .uri("/scim2/Users?filter=externalId%20eq%20%22" + externalId + "%22")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;

        } catch (WebClientResponseException ex) {

            if (ex.getStatusCode() == HttpStatus.NOT_FOUND)
                return false;

            throw ex;
        }
    }

    // WSO2 USER PULL (GET /scim2/Users)
    public Mono<List<Map<String, Object>>> pullUsersFromWso2() {
        String endpoint = wso2Config.getScimUsersEndpoint();
        log.info("Pulling users from WSO2 GET {}", endpoint);

        return wso2Client.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Wso2UserListResponse.class)
                .map(resp -> {
                    List<Wso2ScimUser> users = resp.getResources();
                    if (users != null) {
                        List<Map<String, Object>> formattedUsers = new ArrayList<>();

                        users.forEach(user -> {
                            Map<String, Object> u = new HashMap<>();

                            // Map WSO2 fields to desired format
                            u.put("id", user.getId()); // unique ID
                            u.put("code", user.getUserName()); // can customize as needed
                            u.put("first_names", user.getName() != null && user.getName().getGivenName() != null
                                    ? user.getName().getGivenName()
                                    : "");
                            u.put("last_name", user.getName() != null && user.getName().getFamilyName() != null
                                    ? user.getName().getFamilyName()
                                    : "");
                            u.put("full_name", u.get("last_name") + ", " + u.get("first_names"));

                            // DOB and gender
                            u.put("date_of_birth", user.getBirthDate()); // map accordingly
                            u.put("gender_identity", user.getGender() != null ? user.getGender() : "");

                            // Status flags
                            u.put("is_active", user.getActive() != null ? user.getActive() : false);
                            u.put("is_terminated", user.getTerminated() != null ? user.getTerminated() : false);
                            u.put("is_on_leave", user.getOnLeave() != null ? user.getOnLeave() : false);

                            // Job info
                            u.put("pay_point", "");
                            u.put("department", user.getDepartment() != null ? user.getDepartment() : "");
                            u.put("location", user.getLocation() != null ? user.getLocation() : "");

                            String branchGroup = "";
                            if (user.getGroups() != null && !user.getGroups().isEmpty()) {
                                branchGroup = user.getGroups().get(0).getDisplay(); // or getValue(), depending on what
                                                                                    // you want
                            }
                            u.put("group", branchGroup);
                            u.put("position", user.getPosition() != null ? user.getPosition() : "");
                            u.put("employment_status",
                                    user.getEmploymentStatus() != null ? user.getEmploymentStatus() : "");

                            // Avatar and email
                            u.put("avatar", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
                            u.put("email", (user.getEmails() != null && !user.getEmails().isEmpty())
                                    ? user.getEmails().get(0).getValue()
                                    : "");

                            // Start/end dates
                            u.put("started_at", user.getStartedAt() != null ? user.getStartedAt() : null);
                            u.put("finished_at", user.getFinishedAt() != null ? user.getFinishedAt() : null);

                            formattedUsers.add(u);
                        });

                        return formattedUsers;
                    } else {
                        return Collections.<Map<String, Object>>emptyList();
                    }
                })
                .doOnError(ex -> log.error("WSO2 PULL failed", ex))
                .onErrorResume(ex -> Mono.just(Collections.<Map<String, Object>>emptyList()));
    }

    // =========================================================
    // WSO2 USER PUSH (POST /Users)
    // =========================================================
    public Flux<ScimUserResponse> pushUsersToWso2(List<CanonicalUser> users) {
        log.info("Pushing {} users to WSO2 SCIM2...", users.size());

        return Flux.fromIterable(users)
                .flatMap(canonical -> {
                    Wso2ScimUser wsoUser = userMapper.toWso2(canonical);

                    return wso2Client.post()
                            .uri("/scim2/Users")
                            .bodyValue(wsoUser)
                            .retrieve()
                            .bodyToMono(ScimUserResponse.class)
                            .doOnError(WebClientResponseException.class,
                                    ex -> log.error("WSO2 PUSH failed externalId={} status={} body={}",
                                            wsoUser.getExternalId(), ex.getStatusCode(), ex.getResponseBodyAsString()))
                            .onErrorResume(ex -> Mono.empty()); // skip failed user but continue others
                });
    }

    // WSO2 PUSH SINGLE SCIM USER (POST /scim2/Users)
    public Mono<ScimUserResponse> pushScimUserToWso2(ScimUserRequest scimUser) {
        log.info("Pushing SCIM user to WSO2 userName={}", scimUser.getUserName());
        log.info("SCIM PAYLOAD = {}", scimUser);

        return wso2Client.post()
                .uri("/scim2/Users")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(scimUser)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(ScimUserResponse.class);
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("WSO2 ERROR BODY: {}", errorBody);
                                    return Mono.error(new RuntimeException(errorBody));
                                });
                    }
                });

    }

    public Mono<ScimUserResponse> pushIncomingToWso2(IncomingUser incoming) {
        ScimUserRequest scim = userMapper.toScim(incoming);
        return pushScimUserToWso2(scim);
    }

    // =========================================================
    // WSO2 UPDATE USER (PUT /scim2/Users/{id})
    // =========================================================
    public ScimUserResponse updateUserInWso2(String userId, Wso2ScimUser user) {

        log.info("Updating user in WSO2 userId={}", userId);

        try {
            return wso2Client.put()
                    .uri("/scim2/Users/" + userId)
                    .bodyValue(user)
                    .retrieve()
                    .bodyToMono(ScimUserResponse.class)
                    .block();

        } catch (WebClientResponseException ex) {
            log.error("WSO2 UPDATE failed userId={} status={} body={}",
                    userId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        }
    }
}
