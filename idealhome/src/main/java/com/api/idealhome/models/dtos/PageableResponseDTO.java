package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PageableResponseDTO {
    private int total;
    private int totalPages;
    private int actualPage;
    private boolean paginable;
    private List<String> summary;
    private List<PropertyDTO> elementList;
}
