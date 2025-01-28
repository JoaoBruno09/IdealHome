package com.api.idealhome.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TokenResponseDTO {
    @JsonProperty("access_token")
    private String accessToken;
}
