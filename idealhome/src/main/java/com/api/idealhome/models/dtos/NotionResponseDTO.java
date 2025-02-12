package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class NotionResponseDTO {
    private List<RowDTO> results;
}
