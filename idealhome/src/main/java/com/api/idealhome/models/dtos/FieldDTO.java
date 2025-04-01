package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDTO {
    private String type;
    private Double number;
    private String url;
    private Boolean checkbox;
    private List<TextFieldDTO> title;
    @JsonProperty("rich_text")
    private List<TextFieldDTO> richText;
    private DateFieldDTO date;
}