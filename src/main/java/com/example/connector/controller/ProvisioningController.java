package com.example.connector.controller;

import reactor.core.publisher.Flux;
import com.example.connector.dto.CanonicalUser;
import com.example.connector.dto.DeleteUserRequest;
import com.example.connector.dto.DeleteUserResponse;
import com.example.connector.dto.IncomingUser;
import com.example.connector.dto.scim.ScimUserResponse;
import com.example.connector.dto.scim.Wso2ScimUser;
import com.example.connector.mapper.UserMapper;
import com.example.connector.service.ProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/provision")
@RequiredArgsConstructor
public class ProvisioningController {

    private final UserMapper mapper;
    private final ProvisioningService provisioningService;

    // ------------------------------------------
    // PING
    // ------------------------------------------
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    // ------------------------------------------
    // SINGLE USER UPSERT
    // ------------------------------------------
    @PostMapping("/users")
    public ScimUserResponse upsertSingleUser(@RequestBody Wso2ScimUser wsoUser) {
        log.info("Received single user provisioning request externalId={}", wsoUser.getExternalId());
        CanonicalUser canonical = mapper.fromWso2(wsoUser);
        return provisioningService.upsertUser(mapper.toCymmetri(canonical));
    }

    // ------------------------------------------
    // BULK PARALLEL UPSERT
    // ------------------------------------------
    @PostMapping("/users/bulk")
    public List<ScimUserResponse> upsertBulk(@RequestBody List<Wso2ScimUser> wsoUsers) {
        log.info("Received BULK provisioning size={}", wsoUsers.size());

        List<CanonicalUser> canonicalUsers = wsoUsers.stream()
                .map(mapper::fromWso2)
                .toList();

        return provisioningService.upsertUsersParallel(
                canonicalUsers.stream()
                        .map(mapper::toCymmetri)
                        .toList());
    }

    // ------------------------------------------
    // PULL users FROM WSO2
    // ------------------------------------------
    @GetMapping("/users/pull")
    public Mono<List<Map<String, Object>>> pullUsersFromWso2() {
        log.info("Pulling users from WSO2...");
        return provisioningService.pullUsersFromWso2();
    }

    // ------------------------------------------
    // PUSH users TO WSO2
    // ------------------------------------------
    @PostMapping("/users/push")
    public Flux<ScimUserResponse> pushUsersToWso2(@RequestBody List<CanonicalUser> users) {
        log.info("Pushing {} canonical users to WSO2...", users.size());
        return provisioningService.pushUsersToWso2(users);
    }

    // PUSH single user TO WSO2 (Basic Auth)
    @PostMapping("/users/push/single")
    public Mono<ScimUserResponse> pushSingleUser(@RequestBody IncomingUser incoming) {
        return provisioningService.pushIncomingToWso2(incoming);
    }

    // ------------------------------------------
    // DELETE WSO2 USER BY USERNAME (Reactive)
    // ------------------------------------------
    @DeleteMapping("/api/users/delete")
    public Mono<ResponseEntity<DeleteUserResponse>> deleteUser(@RequestBody DeleteUserRequest request) {
        String username = request.getCode();
        log.info("Received delete request for username={}", username);
        return provisioningService.deleteUserByUsername(username, request)
                .map(response -> {
                    return switch (response.getStatus()) {
                        case "SUCCESS" -> ResponseEntity.ok(response);
                        case "NOT_FOUND" -> ResponseEntity.status(404).body(response);
                        default -> ResponseEntity.status(500).body(response);
                    };
                });
    }

    // PUSH or UPDATE single user TO WSO2
   @PostMapping("/users/upsert/single")
   public Mono<ScimUserResponse> upsertSingleUser(@RequestBody IncomingUser incoming) {
    return provisioningService.upsertIncomingToWso2(incoming);
   }

}
