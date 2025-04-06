package com.api.idealhome.clients;

import com.api.idealhome.models.dtos.NotionAddPropertyRequestDTO;
import com.api.idealhome.models.dtos.NotionResponseDTO;
import com.api.idealhome.models.dtos.NotionUpdatePropertyRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "notionAPI", url = "${notion.url}")
public interface NotionClient {

    @PostMapping(value = "/databases/{databaseId}/query")
    NotionResponseDTO getPropertiesDataBase(@RequestHeader("Authorization") String token,
                                            @RequestHeader("grant_type") String grantType,
                                            @RequestHeader("scope") String scope,
                                            @RequestHeader("Notion-Version") String notionVersion,
                                            @PathVariable("databaseId") String databaseId);

    @PostMapping(value = "/pages")
    void addNewProperty(@RequestHeader("Authorization") String token,
                        @RequestHeader("Notion-Version") String notionVersion,
                        @RequestBody NotionAddPropertyRequestDTO notionAddPropertyRequestDTO);

    @PatchMapping(value = "/pages/{pageId}")
    void updateProperty(@RequestHeader("Authorization") String token,
                        @RequestHeader("Notion-Version") String notionVersion,
                        @RequestBody NotionUpdatePropertyRequestDTO notionUpdatePropertyRequestDTO,
                        @PathVariable("pageId") String pageId);
}
