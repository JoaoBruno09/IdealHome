package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class NotionRequestDTO {
    private ParentDTO parent;
    private RowFieldsDTO properties;
}
