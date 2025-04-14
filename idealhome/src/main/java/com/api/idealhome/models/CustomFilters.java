package com.api.idealhome.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class CustomFilters {
    private int minimumRooms;
    private String province;
    private List<String> notInterestedMunicipality;
}
