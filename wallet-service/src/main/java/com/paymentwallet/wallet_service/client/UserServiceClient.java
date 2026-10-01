package com.paymentwallet.wallet_service.client;

import com.paymentwallet.wallet_service.dto.UserLookupResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(
            @Value("${user-service.base-url}")
            String userServiceBaseUrl
    ) {

        this.restClient =
                RestClient.builder()
                        .baseUrl(userServiceBaseUrl)
                        .build();
    }

    public UserLookupResponse findByWalletPayId(
            String walletPayId,
            String authorizationHeader
    ) {

        return restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/users/lookup")
                                .queryParam(
                                        "walletPayId",
                                        walletPayId
                                )
                                .build()
                )
                .header(
                        "Authorization",
                        authorizationHeader
                )
                .retrieve()
                .body(UserLookupResponse.class);
    }

    public List<UserLookupResponse> findUsersByIds(
            List<Long> userIds,
            String authorizationHeader
    ) {

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<UserLookupResponse> response =
                restClient
                        .post()
                        .uri("/users/lookup-by-ids")
                        .header(
                                "Authorization",
                                authorizationHeader
                        )
                        .body(userIds)
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        List<UserLookupResponse>
                                        >() {
                                }
                        );

        return response == null
                ? List.of()
                : response;
    }
}