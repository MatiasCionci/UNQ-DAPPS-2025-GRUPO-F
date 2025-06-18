package com.dappstp.dappstp.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // Para ignorar campos que no mapeamos
public class TeamDto {
    private int id;
    private String name;
     private String shortName;
    private String tla; // Three Letter Abbreviation
    private String crest; // URL del escudo

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getTla() { return tla; }
    public void setTla(String tla) { this.tla = tla; }

    public String getCrest() { return crest; }
    public void setCrest(String crest) { this.crest = crest; }
}