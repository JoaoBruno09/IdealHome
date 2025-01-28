package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.models.TokenResponseDTO;
import com.api.idealhome.services.CronRequestTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CronRequestTaskServiceImpl implements CronRequestTaskService {

    private final IdealistaClient idealistaClient;

    @Value("${idealista.secret:}")
    private String idealistaSecret;

    @Value("${idealista.key:}")
    private String idealistaKey;

    @Override
    public void executeRequest() {
        System.out.print(generateToken());
    }

    private String generateToken() {
        String keySecret = idealistaKey + idealistaSecret;
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(keySecret.getBytes());

        return Optional.ofNullable(idealistaClient.generateNewToken(authorizationHeader, "client_credentials", "read")).map(TokenResponseDTO::getAccessToken)
                .orElse(null);
    }
}
