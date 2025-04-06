package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class NotionUpdatePropertyRequestDTO {
    private Boolean archived;
    @JsonProperty("in_trash")
    private Boolean inTrash;
}
