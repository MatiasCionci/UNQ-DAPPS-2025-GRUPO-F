package com.dappstp.dappstp.model;
import java.util.List;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Prediction {
 @JsonProperty("winner")
    private String winner;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("reasons")
    private List<String> reasons;
    
    @JsonProperty("scorePrediction")
    private String scorePrediction;
}