package com.api.idealhome;

import com.api.idealhome.services.CronRequestTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test")
@RequiredArgsConstructor
public class TestController {

    private final CronRequestTaskService cronRequestTaskService;

    @GetMapping
    public void getTestData() {
        cronRequestTaskService.executeRequest();
    }
}