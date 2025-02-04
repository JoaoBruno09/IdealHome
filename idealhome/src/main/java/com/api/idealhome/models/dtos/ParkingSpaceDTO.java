package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ParkingSpaceDTO {
    private Boolean hasParkingSpace;
    private Boolean isParkingSpaceIncludedInPrice;
}
