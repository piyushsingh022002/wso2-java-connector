package com.example.connector.service;

import com.example.connector.config.Wso2Config;
import com.example.connector.dto.CanonicalUser;
import com.example.connector.dto.cymmetri.CymmetriUser;
import com.example.connector.dto.scim.ScimUserResponse;
import com.example.connector.dto.scim.Wso2ScimUser;
import com.example.connector.dto.scim.Wso2UserListResponse;
import com.example.connector.mapper.UserMapper;
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
import java.util.Collections;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisioningService {

    // ---------------------------------------------------------
    //           INJECTED CLIENTS + CONFIG
    // ---------------------------------------------------------
    private final WebClient cymmetriClient;
    private final WebClient wso2Client;
    private final Wso2Config wso2Config;
    private final UserMapper userMapper;

    // =========================================================
    //                   UPSERT ENTRYPOINT
    // =========================================================
    public ScimUserResponse upsertUser(CymmetriUser user) {

        // ENFORCE active=true STRICTLY
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException(
                    "Rejected: externalId=" + user.getExternalId() +
                    " is inactive. Active=true is strictly required."
            );
        }

        boolean exists = cymmetriUserExists(user.getExternalId());

        return exists ? updateCymmetriUser(user) : createCymmetriUser(user);
    }

    // =========================================================
    //           PARALLEL UPSERT FOR BULK PROCESSING
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
    //                   CREATE USER â†’ CYMMETRI
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
    //                   UPDATE USER â†’ CYMMETRI
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
    //             CHECK IF USER EXISTS IN CYMMETRI
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

    // =========================================================
    //                WSO2 USER PULL (GET /scim2/Users)
    // =========================================================
    public Mono<List<Wso2ScimUser>> pullUsersFromWso2() {
    String endpoint = wso2Config.getScimUsersEndpoint();
    log.info("Pulling users from WSO2 GET {}", endpoint);

    return wso2Client.get()
        .uri(endpoint)
        .retrieve()
        .bodyToMono(Wso2UserListResponse.class)
        .doOnNext(resp -> log.info("WSO2 raw response mapped: {}", resp))
        .map(resp -> {
            List<Wso2ScimUser> users = resp.getResources();
            if (users != null) {
                users.forEach(user -> log.info("User fetched: {}", user));
                return users;
            } else {
                return Collections.<Wso2ScimUser>emptyList();
            }
        })
        .doOnError(ex -> log.error("WSO2 PULL failed", ex))
        .onErrorResume(ex -> Mono.just(Collections.<Wso2ScimUser>emptyList()));
}




    // =========================================================
    //                WSO2 USER PUSH (POST /Users)
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
                        .doOnError(WebClientResponseException.class, ex ->
                                log.error("WSO2 PUSH failed externalId={} status={} body={}",
                                        wsoUser.getExternalId(), ex.getStatusCode(), ex.getResponseBodyAsString())
                        )
                        .onErrorResume(ex -> Mono.empty()); // skip failed user but continue others
            });
}


    // =========================================================
    //         WSO2 PUSH SINGLE SCIM USER (POST /scim2/Users)
    // =========================================================
    public Mono<ScimUserResponse> pushScimUserToWso2(com.example.connector.dto.scim.ScimUser scimUser) {
    log.info("Pushing single SCIM user to WSO2 userName={}", scimUser.getUserName());

    return wso2Client.post()
            .uri("/scim2/Users")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(scimUser)
            .retrieve()
            .bodyToMono(ScimUserResponse.class)
            .doOnNext(resp -> log.info("WSO2 SCIM push response for {}: {}", scimUser.getUserName(), resp))
            .doOnError(ex -> log.error("WSO2 SCIM CREATE failed userName={}", scimUser.getUserName(), ex))
            .onErrorResume(ex -> Mono.empty()); // You can customize error handling
   }


    public Mono<ScimUserResponse> pushCanonicalAsScimToWso2(CanonicalUser canonical) {
    com.example.connector.dto.scim.ScimUser scimUser = userMapper.toScim(canonical);
    return pushScimUserToWso2(scimUser);
   }



    // =========================================================
    //         WSO2 UPDATE USER (PUT /scim2/Users/{id})
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


