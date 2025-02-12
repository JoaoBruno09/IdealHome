package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TextFieldDTO {
    @JsonProperty("plain_text")
    private String plainText;
}
