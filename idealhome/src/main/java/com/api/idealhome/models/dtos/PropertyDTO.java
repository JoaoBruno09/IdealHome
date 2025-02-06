package com.api.idealhome.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PropertyDTO {
    private String propertyCode;
    private String floor;
    private Double price;
    private Double size;
    private Integer rooms;
    private Integer bathrooms;
    private String address;
    private String province;
    private String municipality;
    private String url;
    private String status;
    private Boolean newDevelopment;
    private Boolean hasLift;
    private ParkingSpaceDTO parkingSpace;
    private Boolean newDevelopmentFinished;
    private Double priceByArea;
}
