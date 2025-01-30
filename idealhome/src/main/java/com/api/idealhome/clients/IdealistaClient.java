package com.api.idealhome.clients;

import com.api.idealhome.models.dtos.PageableResponseDTO;
import com.api.idealhome.models.dtos.TokenResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(value = "idealistaAPI", url = "${idealista.url}")
public interface IdealistaClient {

    @PostMapping(value = "/oauth/token", headers = {"Content-Type=application/x-www-form-urlencoded"})
    TokenResponseDTO generateNewToken(@RequestHeader("Authorization") String authorization,
                                      @RequestParam("grant_type") String grantType,
                                      @RequestParam("scope") String scope);

    @PostMapping(value = "/${idealista.version}/pt/search", consumes = "multipart/form-data")
    PageableResponseDTO searchHomes(@RequestHeader("Authorization") String authorization,
                                    @RequestBody Map<String, String> filters);
}
