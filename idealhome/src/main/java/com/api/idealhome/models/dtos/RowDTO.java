package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RowDTO {
    private String id;
    private RowFieldsDTO properties;
}
