package com.api.idealhome.clients;

import com.api.idealhome.models.TokenResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="idealistaAPI", url = "${idealista.url}")
public interface IdealistaClient {

    @PostMapping(value = "/oauth/token", headers = {"Content-Type=application/x-www-form-urlencoded"})
    TokenResponseDTO generateNewToken(@RequestHeader("Authorization") String authorization,
                                      @RequestParam("grant_type") String grantType,
                                      @RequestParam("scope") String scope);
}
