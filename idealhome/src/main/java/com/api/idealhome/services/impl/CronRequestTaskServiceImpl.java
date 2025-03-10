package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.clients.NotionClient;
import com.api.idealhome.clients.TelegramClient;
import com.api.idealhome.configs.IdealistaConfigs;
import com.api.idealhome.configs.NotionConfigs;
import com.api.idealhome.configs.TelegramConfigs;
import com.api.idealhome.models.dtos.FieldDTO;
import com.api.idealhome.models.dtos.IdealistaPropertyDTO;
import com.api.idealhome.models.dtos.NotionPropertyType;
import com.api.idealhome.models.dtos.NotionRequestDTO;
import com.api.idealhome.models.dtos.PageableResponseDTO;
import com.api.idealhome.models.dtos.ParentDTO;
import com.api.idealhome.models.dtos.RowFieldsDTO;
import com.api.idealhome.models.dtos.TelegramRequestDTO;
import com.api.idealhome.models.dtos.TextContentDTO;
import com.api.idealhome.models.dtos.TextFieldDTO;
import com.api.idealhome.services.CronRequestTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final TelegramClient telegramClient;
    private final TelegramConfigs telegramConfigs;

    @Scheduled(cron = "0 0 13 * * ?", zone = "Europe/Lisbon")
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

        log.info("{} properties to be added into Notion.", newPropertiesToAdd.size());
        addNewPropertiesInNotionAndSendTelegramNotification(newPropertiesToAdd);
    }

    private void findIdealistaProperties(List<IdealistaPropertyDTO> foundIdealistaProperties) {
        PageableResponseDTO pageableResponseDTO;
        boolean hasMorePages;
        int numPage = 1;

        do {
            idealistaConfigs.getFilters().put("numPage", String.valueOf(numPage));
            pageableResponseDTO = idealistaClient.searchHomes(generateToken(), idealistaConfigs.getFilters());
            foundIdealistaProperties.addAll(pageableResponseDTO.getElementList());
            numPage++;
            hasMorePages = numPage <= pageableResponseDTO.getTotalPages() && pageableResponseDTO.isPaginable();
        } while (hasMorePages);

        log.info("Found {} properties in Idealista", pageableResponseDTO.getTotal());
    }

    private void getNotionProperties(List<String> notionPropertyIds) {
        notionClient.getPropertiesDataBase(notionConfigs.getKey(), notionConfigs.getGrantType(), notionConfigs.getScope(), notionConfigs.getVersion(), notionConfigs.getDataBaseId())
                .getResults().forEach(property -> notionPropertyIds.add(property.getProperties().getId().getTitle().getFirst().getText().getContent()));
    }

    private void addNewPropertiesInNotionAndSendTelegramNotification(List<IdealistaPropertyDTO> newPropertiesToAdd) {
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
                                .areaBruta(buildFieldDTO(NUMBER, String.valueOf(idealistaPropertyDTO.getSize())))
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
                sendTelegramMessage(idealistaPropertyDTO);
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

    private void sendTelegramMessage(IdealistaPropertyDTO idealistaPropertyDTO) {
        String telegramMessage = "*✨🆕 Novo Imóvel à Venda\\!🆕✨*\n\n" +
                "*🏡 Imóvel ID:* " + idealistaPropertyDTO.getPropertyCode() + "\n" +
                "*🏗️ Nova Construção:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(idealistaPropertyDTO.getNewDevelopment())) + "\n" +
                "*💰 Preço:* " + escapeMarkdownV2(String.valueOf(idealistaPropertyDTO.getPrice())) + "€*\n" +
                "*📍 Morada:* " + escapeMarkdownV2(idealistaPropertyDTO.getAddress() + ", " + idealistaPropertyDTO.getMunicipality()) + "\n" +
                "*🛏️ Quartos:* " + idealistaPropertyDTO.getRooms() + "\n" +
                "*🏢 Andar:* " + escapeMarkdownV2(idealistaPropertyDTO.getFloor()) + "\n" +
                "*🛗 Elevador:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(idealistaPropertyDTO.getHasLift())) + "\n" +
                "*🚗 Estacionamento:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(idealistaPropertyDTO.getParkingSpace().getHasParkingSpace())) + "\n" +
                "🔗 [Ver Imóvel](" + escapeMarkdownV2(refractURL(idealistaPropertyDTO.getUrl())) + ")\n" +
                "📝 [Mais detalhes no Notion](" + escapeMarkdownV2((notionConfigs.getDataBaseUrl())) + ")";

        TelegramRequestDTO telegramRequestDTO = TelegramRequestDTO.builder()
                .chatId(telegramConfigs.getChatId())
                .text(telegramMessage)
                .parseMode("MarkdownV2")
                .build();
        telegramClient.sendTelegramMessage(telegramConfigs.getKey(), telegramRequestDTO);
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

    // Function to escape special MarkdownV2 characters
    private String escapeMarkdownV2(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private String booleanValueToStringYesOrNo(boolean value) {
        return Boolean.TRUE.equals(value) ? "Sim" : "Não";
    }

    //TODO TEST MESSAGE THAT IS WORKING JUST FOR TEST PURPOSES
    private void sendTelegramMessageTest() {
        String telegramMessage = "*✨🆕 Novo Imóvel à Venda\\! 🆕✨*\n\n" +
                "*🏡 Imóvel ID:* Teste\n" +
                "*🏗️ Nova Construção:* Sim\n" +
                "*💰 Preço:* " + escapeMarkdownV2("200.000") + "€\n" +
                "*📍 Morada:* " + escapeMarkdownV2("Rua Teste, Teste") + "\n" +
                "*🛏️ Quartos:* 3\n" +
                "*🚿 Casas de Banho:* 3\n" +
                "*🏢 Andar:* 3 \n" +
                "*🛗 Elevador:* Sim\n" +
                "*🚗 Estacionamento:* Sim\n" +
                "🔗 [Ver Imóvel](" + escapeMarkdownV2("https://www.google.com/") + ")\n" +
                "📝 [Mais detalhes no Notion](" + escapeMarkdownV2("https://www.google.com/") + ")";

        TelegramRequestDTO telegramRequestDTO = TelegramRequestDTO.builder()
                .chatId(telegramConfigs.getChatId())
                .text(telegramMessage)
                .parseMode("MarkdownV2")
                .build();
        telegramClient.sendTelegramMessage(telegramConfigs.getKey(), telegramRequestDTO);
    }
}
