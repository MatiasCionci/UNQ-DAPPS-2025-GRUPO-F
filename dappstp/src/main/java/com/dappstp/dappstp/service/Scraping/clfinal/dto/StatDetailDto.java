package com.dappstp.dappstp.service.scraping.clfinal.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatDetailDto {
    private String label;
    private String homeValue;
    private String awayValue;
}