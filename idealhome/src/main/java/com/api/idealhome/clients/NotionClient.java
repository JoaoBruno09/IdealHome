package com.api.idealhome.clients;

import com.api.idealhome.models.dtos.NotionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "notionAPI", url = "${notion.url}")
public interface NotionClient {

    @PostMapping(value = "/databases/{databaseId}/query", headers = "")
    NotionResponseDTO getPropertiesDataBase(@RequestHeader("Authorization") String token,
                                            @RequestHeader("grant_type") String grantType,
                                            @RequestHeader("scope") String scope,
                                            @RequestHeader("Notion-Version") String notionVersion,
                                            @PathVariable("databaseId") String databaseId);
}
