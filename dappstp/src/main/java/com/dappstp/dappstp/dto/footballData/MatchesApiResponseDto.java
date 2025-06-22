package com.dappstp.dappstp.dto.footballData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchesApiResponseDto {

    private List<MatchDto> matches;

    // Getters y Setters
    public List<MatchDto> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchDto> matches) {
        this.matches = matches;
    }
}