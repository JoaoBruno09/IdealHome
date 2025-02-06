package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.configs.IdealistaConfigs;
import com.api.idealhome.models.dtos.PageableResponseDTO;
import com.api.idealhome.models.dtos.PropertyDTO;
import com.api.idealhome.services.CronRequestTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CronRequestTaskServiceImpl implements CronRequestTaskService {

    private final IdealistaClient idealistaClient;
    private final IdealistaConfigs idealistaConfigs;

    @Override
    public void executeRequest() {
        fetchResults();
    }

    private String generateToken() {
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((idealistaConfigs.getKey() + idealistaConfigs.getSecret()).getBytes());

        return Optional.ofNullable(idealistaClient.generateNewToken(authorizationHeader, idealistaConfigs.getGrantType(), idealistaConfigs.getScope())).map(tokenResponseDTO ->
                        "Bearer " + tokenResponseDTO.getAccessToken())
                .orElse(null);
    }

    private void fetchResults() {
        List<PropertyDTO> foundProperties = new ArrayList<>();
        PageableResponseDTO pageableResponseDTO;
        boolean hasMorePages;

        do {
            pageableResponseDTO = idealistaClient.searchHomes(generateToken(), idealistaConfigs.getFilters());
            foundProperties.addAll(pageableResponseDTO.getElementList());
            hasMorePages = pageableResponseDTO.getActualPage() <= pageableResponseDTO.getTotalPages() && pageableResponseDTO.isPaginable();
        } while (hasMorePages);

        //Filter properties on Porto District
        foundProperties = foundProperties.stream().filter(property -> "Porto".equalsIgnoreCase(property.getProvince()) && property.getRooms() > 1).toList();
    }
}
