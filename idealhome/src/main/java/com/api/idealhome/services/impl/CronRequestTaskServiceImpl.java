package com.api.idealhome.services.impl;

import com.api.idealhome.clients.IdealistaClient;
import com.api.idealhome.clients.NotionClient;
import com.api.idealhome.clients.TelegramClient;
import com.api.idealhome.configs.IdealistaConfigs;
import com.api.idealhome.configs.NotionConfigs;
import com.api.idealhome.configs.TelegramConfigs;
import com.api.idealhome.models.dtos.DateFieldDTO;
import com.api.idealhome.models.dtos.FieldDTO;
import com.api.idealhome.models.dtos.IdealistaPropertyDTO;
import com.api.idealhome.models.dtos.NotionAddPropertyRequestDTO;
import com.api.idealhome.models.dtos.NotionPropertyType;
import com.api.idealhome.models.dtos.NotionUpdatePropertyRequestDTO;
import com.api.idealhome.models.dtos.PageableResponseDTO;
import com.api.idealhome.models.dtos.ParentDTO;
import com.api.idealhome.models.dtos.ParkingSpaceDTO;
import com.api.idealhome.models.dtos.RowDTO;
import com.api.idealhome.models.dtos.RowFieldsDTO;
import com.api.idealhome.models.dtos.TelegramRequestDTO;
import com.api.idealhome.models.dtos.TextContentDTO;
import com.api.idealhome.models.dtos.TextFieldDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.api.idealhome.models.dtos.NotionPropertyType.CHECKBOX;
import static com.api.idealhome.models.dtos.NotionPropertyType.DATE;
import static com.api.idealhome.models.dtos.NotionPropertyType.NUMBER;
import static com.api.idealhome.models.dtos.NotionPropertyType.TEXT;
import static com.api.idealhome.models.dtos.NotionPropertyType.TITLE;
import static com.api.idealhome.models.dtos.NotionPropertyType.URL;

@Slf4j
@RequiredArgsConstructor
@Service
public class CronRequestTaskServiceImpl {

    private final IdealistaClient idealistaClient;
    private final IdealistaConfigs idealistaConfigs;
    private final NotionClient notionClient;
    private final NotionConfigs notionConfigs;
    private final TelegramClient telegramClient;
    private final TelegramConfigs telegramConfigs;

    private static final List<String> MUNICIPIO_NOT_INTERESTED_IN = List.of("penafiel", "arcozelo", "serzedo",
            "são felix", "avintes", "póvoa de varzim");

    @Scheduled(cron = "0 0 13 * * ?", zone = "Europe/Lisbon")
    public void deleteOldNotionPropertiesAlreadySeen() {
        List<RowDTO> notionProperties = new ArrayList<>();
        getNotionProperties(notionProperties);

        List<RowDTO> notionPropertiesToDelete = notionProperties.stream()
                .filter(property ->
                        ChronoUnit.DAYS.between(LocalDate.parse(property.getProperties().getDataDeCriacao().getDate().getStart()), LocalDate.now()) > 2 &&
                                Boolean.TRUE.equals(property.getProperties().getVisto().getCheckbox()) &&
                                Boolean.FALSE.equals(property.getProperties().getInteresse().getCheckbox()))
                .toList();

        List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
        NotionUpdatePropertyRequestDTO notionUpdatePropertyRequestDTO = NotionUpdatePropertyRequestDTO.builder().archived(false).inTrash(true).build();
        for (RowDTO rowDTO : notionPropertiesToDelete) {
            completableFutureList.add(CompletableFuture.runAsync(() ->
                    notionClient.updateProperty(notionConfigs.getKey(), notionConfigs.getVersion(), notionUpdatePropertyRequestDTO, rowDTO.getId())));
        }

        try {
            CompletableFuture.allOf(completableFutureList.toArray(CompletableFuture[]::new)).get();
            log.info("Deleted {} properties to from Notion.", notionPropertiesToDelete.size());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(cron = "0 0 12 * * ?", zone = "Europe/Lisbon")
    public void findNewPropertiesAndAddToNotion() {
        List<IdealistaPropertyDTO> foundIdealistaProperties = new ArrayList<>();
        List<RowDTO> notionProperties = new ArrayList<>();

        findIdealistaProperties(foundIdealistaProperties);
        getNotionProperties(notionProperties);

        //Filter properties on Porto District, has more than one room, and do not exist already in notion
        List<IdealistaPropertyDTO> newPropertiesToAdd = foundIdealistaProperties.stream().filter(property ->
                "Porto".equalsIgnoreCase(property.getProvince()) && property.getRooms() > 1 && !MUNICIPIO_NOT_INTERESTED_IN.contains(property.getMunicipality().toLowerCase()) &&
                        notionProperties.stream().noneMatch(rowDTO ->
                                rowDTO.getProperties().getId().getTitle().getFirst().getText().getContent().contains(property.getPropertyCode()))).toList();

        log.info("{} properties to be added into Notion.", newPropertiesToAdd.size());
        addNewPropertiesInNotionAndSendTelegramNotification(newPropertiesToAdd);
        log.info("Added {} properties into Notion.", newPropertiesToAdd.size());
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

    private void getNotionProperties(List<RowDTO> notionPropertyIds) {
        notionClient.getPropertiesDataBase(notionConfigs.getKey(), notionConfigs.getGrantType(), notionConfigs.getScope(), notionConfigs.getVersion(), notionConfigs.getDataBaseId())
                .getResults().forEach(notionPropertyIds::add);
    }

    private void addNewPropertiesInNotionAndSendTelegramNotification(List<IdealistaPropertyDTO> newPropertiesToAdd) {
        List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();

        for (IdealistaPropertyDTO idealistaPropertyDTO : newPropertiesToAdd) {
            completableFutureList.add(CompletableFuture.runAsync(() -> {
                boolean isNewDevelopment = idealistaPropertyDTO.getNewDevelopment();
                NotionAddPropertyRequestDTO notionRequest = NotionAddPropertyRequestDTO.builder()
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
                                .dataDeCriacao(buildFieldDTO(DATE, LocalDate.now(ZoneId.of("Europe/Lisbon")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
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
        stringValue = Optional.ofNullable(stringValue).orElse("");
        return switch (fieldType) {
            case TITLE ->
                    FieldDTO.builder().title(List.of(TextFieldDTO.builder().text(TextContentDTO.builder().content(stringValue).build()).build())).build();
            case NUMBER -> FieldDTO.builder().number(Double.parseDouble(stringValue)).build();
            case URL -> FieldDTO.builder().url(stringValue).build();
            case CHECKBOX -> FieldDTO.builder().checkbox(Boolean.parseBoolean(stringValue)).build();
            case TEXT ->
                    FieldDTO.builder().richText(List.of(TextFieldDTO.builder().text(TextContentDTO.builder().content(stringValue).build()).build())).build();
            case DATE -> FieldDTO.builder().date(DateFieldDTO.builder().start(stringValue).build()).build();
        };
    }

    private void sendTelegramMessage(IdealistaPropertyDTO idealistaPropertyDTO) {
        String telegramMessage = "*✨🆕 Novo Imóvel à Venda\\!🆕✨*\n\n" +
                "*🏡 Imóvel ID:* " + idealistaPropertyDTO.getPropertyCode() + "\n" +
                "*🏗️ Nova Construção:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(idealistaPropertyDTO.getNewDevelopment())) + "\n" +
                "*💰 Preço:* " + escapeMarkdownV2(String.valueOf(idealistaPropertyDTO.getPrice())) + "€\n" +
                "*📍 Morada:* " + escapeMarkdownV2(idealistaPropertyDTO.getAddress() + ", " + idealistaPropertyDTO.getMunicipality()) + "\n" +
                "*📐 Área Bruta:* " + escapeMarkdownV2(idealistaPropertyDTO.getSize() + "m²") + "\n" +
                "*🛏️ Quartos:* " + idealistaPropertyDTO.getRooms() + "\n" +
                "*🏢 Andar:* " + escapeMarkdownV2(idealistaPropertyDTO.getFloor()) + "\n" +
                "*🛗 Elevador:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(idealistaPropertyDTO.getHasLift())) + "\n" +
                "*🚗 Estacionamento:* " + escapeMarkdownV2(booleanValueToStringYesOrNo(Optional.ofNullable(idealistaPropertyDTO.getParkingSpace()).map(ParkingSpaceDTO::getHasParkingSpace).orElse(false))) + "\n" +
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
        return Optional.ofNullable(text).orElse("")
                .replace("_", "\\_")
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
}
