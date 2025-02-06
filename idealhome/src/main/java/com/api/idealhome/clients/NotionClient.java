package com.api.idealhome.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "notionAPI", url = "${notion.url}")
public interface NotionClient {

    @PostMapping(value = "/databases/${notionAPI.databaseId}/query", headers = "")
    void getPropertiesDataBase();
}
