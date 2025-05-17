package com.dappstp.dappstp.webservices.dto;

public class PredictionRequest {
    private String matchId;
    private String scrapedData;

    // Getters y Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getScrapedData() { return scrapedData; }
    public void setScrapedData(String scrapedData) { this.scrapedData = scrapedData; }
}