package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class FieldDTO {
    private String type;
    private Integer number;
    private String url;
    private Boolean checkbox;
    private List<TextFieldDTO> title;
    @JsonProperty("rich_text")
    private List<TextFieldDTO> richText;
}