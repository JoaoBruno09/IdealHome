package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class ParentDTO {
    @JsonProperty("database_id")
    private String databaseId;
}
