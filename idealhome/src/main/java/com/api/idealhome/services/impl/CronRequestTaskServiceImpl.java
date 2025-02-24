package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.clients.NotionClient;
import com.api.idealhome.configs.IdealistaConfigs;
import com.api.idealhome.configs.NotionConfigs;
import com.api.idealhome.models.dtos.FieldDTO;
import com.api.idealhome.models.dtos.IdealistaPropertyDTO;
import com.api.idealhome.models.dtos.NotionPropertyType;
import com.api.idealhome.models.dtos.NotionRequestDTO;
import com.api.idealhome.models.dtos.PageableResponseDTO;
import com.api.idealhome.models.dtos.ParentDTO;
import com.api.idealhome.models.dtos.RowFieldsDTO;
import com.api.idealhome.models.dtos.TextContentDTO;
import com.api.idealhome.models.dtos.TextFieldDTO;
import com.api.idealhome.services.CronRequestTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.api.idealhome.models.dtos.NotionPropertyType.CHECKBOX;
import static com.api.idealhome.models.dtos.NotionPropertyType.NUMBER;
import static com.api.idealhome.models.dtos.NotionPropertyType.TEXT;
import static com.api.idealhome.models.dtos.NotionPropertyType.TITLE;
import static com.api.idealhome.models.dtos.NotionPropertyType.URL;

@Slf4j
@RequiredArgsConstructor
@Service
public class CronRequestTaskServiceImpl implements CronRequestTaskService {

    private final IdealistaClient idealistaClient;
    private final IdealistaConfigs idealistaConfigs;
    private final NotionClient notionClient;
    private final NotionConfigs notionConfigs;

    @Override
    public void fetchResults() {
        List<IdealistaPropertyDTO> foundIdealistaProperties = new ArrayList<>();
        List<String> notionPropertyIds = new ArrayList<>();

        findIdealistaProperties(foundIdealistaProperties);
        getNotionProperties(notionPropertyIds);

        //Filter properties on Porto District, has more than one room, and do not exist already in notion
        List<IdealistaPropertyDTO> newPropertiesToAdd = foundIdealistaProperties.stream().filter(property ->
                "Porto".equalsIgnoreCase(property.getProvince()) && property.getRooms() > 1 && notionPropertyIds.stream().noneMatch(id ->
                        id.contains(property.getPropertyCode()))).toList();

        addNewPropertiesInNotion(newPropertiesToAdd);

    }

    private void findIdealistaProperties(List<IdealistaPropertyDTO> foundIdealistaProperties) {
        PageableResponseDTO pageableResponseDTO;
        boolean hasMorePages;

        do {
            pageableResponseDTO = idealistaClient.searchHomes(generateToken(), idealistaConfigs.getFilters());
            log.info(String.valueOf(pageableResponseDTO.getTotal()));
            foundIdealistaProperties.addAll(pageableResponseDTO.getElementList());
            hasMorePages = pageableResponseDTO.getActualPage() <= pageableResponseDTO.getTotalPages() && pageableResponseDTO.isPaginable();
        } while (hasMorePages);


    }

    private void getNotionProperties(List<String> notionPropertyIds) {
        notionClient.getPropertiesDataBase(notionConfigs.getKey(), notionConfigs.getGrantType(), notionConfigs.getScope(), notionConfigs.getVersion(), notionConfigs.getDataBaseId())
                .getResults().forEach(property -> notionPropertyIds.add(property.getProperties().getId().getTitle().getFirst().getText().getContent()));
    }

    private void addNewPropertiesInNotion(List<IdealistaPropertyDTO> newPropertiesToAdd) {
        List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();

        for (IdealistaPropertyDTO idealistaPropertyDTO : newPropertiesToAdd) {
            completableFutureList.add(CompletableFuture.runAsync(() -> {
                boolean isNewDevelopment = idealistaPropertyDTO.getNewDevelopment();
                NotionRequestDTO notionRequest = NotionRequestDTO.builder()
                        .parent(ParentDTO.builder().databaseId(notionConfigs.getDataBaseId()).build())
                        .properties(RowFieldsDTO.builder()
                                .id(buildFieldDTO(TITLE, idealistaPropertyDTO.getPropertyCode()))
                                .url(buildFieldDTO(URL, refractURL(idealistaPropertyDTO.getUrl())))
                                .preco(buildFieldDTO(NUMBER, String.valueOf(idealistaPropertyDTO.getPrice())))
                                .areaBruta(buildFieldDTO(NUMBER, String.valueOf(idealistaPropertyDTO.getSize()) + "m2"))
                                .municipio(buildFieldDTO(TEXT, capitalizeStringFirstLetter(idealistaPropertyDTO.getMunicipality())))
                                .morada(buildFieldDTO(TEXT, capitalizeStringFirstLetter(idealistaPropertyDTO.getAddress())))
                                .andar(buildFieldDTO(TEXT, idealistaPropertyDTO.getFloor() + "º"))
                                .estado(buildFieldDTO(TEXT, capitalizeStringFirstLetter(idealistaPropertyDTO.getStatus())))
                                .quartos(buildFieldDTO(NUMBER, String.valueOf(idealistaPropertyDTO.getRooms())))
                                .novo(buildFieldDTO(CHECKBOX, String.valueOf(isNewDevelopment)))
                                .terminado(isNewDevelopment ? buildFieldDTO(CHECKBOX, String.valueOf(idealistaPropertyDTO.getNewDevelopmentFinished())) : null)
                                .precoArea(buildFieldDTO(NUMBER, String.valueOf(idealistaPropertyDTO.getPriceByArea())))
                                .build())
                        .build();

                notionClient.addNewProperty(notionConfigs.getKey(), notionConfigs.getVersion(), notionRequest);
            }));
        }

        try {
            CompletableFuture.allOf(completableFutureList.toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private FieldDTO buildFieldDTO(NotionPropertyType fieldType, String stringValue) {
        return switch (fieldType) {
            case TITLE ->
                    FieldDTO.builder().title(List.of(TextFieldDTO.builder().text(TextContentDTO.builder().content(stringValue).build()).build())).build();
            case NUMBER -> FieldDTO.builder().number(Double.parseDouble(stringValue)).build();
            case URL -> FieldDTO.builder().url(stringValue).build();
            case CHECKBOX -> FieldDTO.builder().checkbox(Boolean.parseBoolean(stringValue)).build();
            case TEXT ->
                    FieldDTO.builder().richText(List.of(TextFieldDTO.builder().text(TextContentDTO.builder().content(stringValue).build()).build())).build();
        };
    }

    private String generateToken() {
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((idealistaConfigs.getKey() + idealistaConfigs.getSecret()).getBytes());

        return Optional.ofNullable(idealistaClient.generateNewToken(authorizationHeader, idealistaConfigs.getGrantType(), idealistaConfigs.getScope())).map(tokenResponseDTO ->
                        "Bearer " + tokenResponseDTO.getAccessToken())
                .orElse(null);
    }

    private String capitalizeStringFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String refractURL(String url) {
        return url.contains("/empreendimento") ? url.replace("/empreendimento", "/imovel") : url;
    }
}
