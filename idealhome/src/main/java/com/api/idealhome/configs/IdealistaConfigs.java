package com.api.idealhome.configs;

import com.api.idealhome.models.CustomFilters;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "idealista")
public class IdealistaConfigs {
    private String url;
    private String secret;
    private String key;
    private String grantType;
    private String scope;
    private String version;
    private Map<String, String> filters;
    private CustomFilters customFilters;
}
