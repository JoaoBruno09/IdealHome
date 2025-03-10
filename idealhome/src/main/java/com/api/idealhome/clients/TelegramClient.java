package com.api.idealhome.clients;

import com.api.idealhome.models.dtos.TelegramRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "telegramAPI", url = "${telegram.url}")
public interface TelegramClient {

    @PostMapping(value = "/bot{botKey}/sendMessage")
    void sendTelegramMessage(@PathVariable("botKey") String botKey,
                             @RequestBody TelegramRequestDTO telegramRequestDTO);
}
