package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.clients.NotionClient;
import com.api.idealhome.configs.IdealistaConfigs;
import com.api.idealhome.configs.NotionConfigs;
import com.api.idealhome.models.dtos.IdealistaPropertyDTO;
import com.api.idealhome.models.dtos.PageableResponseDTO;
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
    private final NotionClient notionClient;
    private final NotionConfigs notionConfigs;

    @Override
    public void executeRequest() {
        //fetchResults();
    }

    private void fetchResults() {
        List<IdealistaPropertyDTO> foundIdealistaProperties = new ArrayList<>();
        PageableResponseDTO pageableResponseDTO;
        boolean hasMorePages;

        pageableResponseDTO = idealistaClient.searchHomes(generateToken(), idealistaConfigs.getFilters());

        /*do {
            pageableResponseDTO = idealistaClient.searchHomes(generateToken(), idealistaConfigs.getFilters());
            foundIdealistaProperties.addAll(pageableResponseDTO.getElementList());
            hasMorePages = pageableResponseDTO.getActualPage() <= pageableResponseDTO.getTotalPages() && pageableResponseDTO.isPaginable();
        } while (hasMorePages);*/

        List<String> notionPropertyIds = new ArrayList<>();
        notionClient.getPropertiesDataBase(notionConfigs.getKey(), notionConfigs.getGrantType(), notionConfigs.getScope(), notionConfigs.getVersion(), notionConfigs.getDataBaseId())
                .getResults().forEach(property -> notionPropertyIds.add(property.getProperties().getId().getTitle().getFirst().getPlainText()));

        //Filter properties on Porto District, has more than one room, and do not exist already in notion
        List<IdealistaPropertyDTO> newPropertiesToAdd = new ArrayList<>();
        newPropertiesToAdd = foundIdealistaProperties.stream().filter(property ->
                "Porto".equalsIgnoreCase(property.getProvince()) && property.getRooms() > 1 && notionPropertyIds.stream().noneMatch((id) ->
                        id.contains(property.getPropertyCode()))).toList();

    }

    private String generateToken() {
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((idealistaConfigs.getKey() + idealistaConfigs.getSecret()).getBytes());

        return Optional.ofNullable(idealistaClient.generateNewToken(authorizationHeader, idealistaConfigs.getGrantType(), idealistaConfigs.getScope())).map(tokenResponseDTO ->
                        "Bearer " + tokenResponseDTO.getAccessToken())
                .orElse(null);
    }
}
