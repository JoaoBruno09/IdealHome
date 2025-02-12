package com.api.idealhome.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RowFieldsDTO {
    @JsonProperty("Morada")
    private FieldDTO morada;
    @JsonProperty("Andar")
    private FieldDTO andar;
    @JsonProperty("Municipio")
    private FieldDTO municipio;
    @JsonProperty("Preço")
    private FieldDTO preco;
    @JsonProperty("URL")
    private FieldDTO url;
    @JsonProperty("Quartos")
    private FieldDTO quartos;
    @JsonProperty("Preço por Área")
    private FieldDTO precoArea;
    @JsonProperty("Área Bruta")
    private FieldDTO areaBruta;
    @JsonProperty("Novo")
    private FieldDTO novo;
    @JsonProperty("Terminado")
    private FieldDTO terminado;
    @JsonProperty("Interesse")
    private FieldDTO interesse;
    @JsonProperty("Estado")
    private FieldDTO estado;
    @JsonProperty("ID")
    private FieldDTO id;
}
