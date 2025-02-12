package com.api.idealhome.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notion")
public class NotionConfigs {
    private String url;
    private String key;
    private String version;
    private String grantType;
    private String scope;
    private String dataBaseId;
}
