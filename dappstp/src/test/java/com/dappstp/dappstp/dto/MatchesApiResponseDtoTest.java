package com.dappstp.dappstp.dto;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.footballData.MatchDto;
import com.dappstp.dappstp.dto.footballData.MatchesApiResponseDto;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatchesApiResponseDtoTest {

    @Test
    void testGettersAndSetters() {
        MatchesApiResponseDto apiResponse = new MatchesApiResponseDto();
        assertNull(apiResponse.getMatches(), "Matches list should be null initially");

        List<MatchDto> matchesList = new ArrayList<>();
        MatchDto match1 = new MatchDto();
        match1.setId(1);
        match1.setStatus("FINISHED");

        MatchDto match2 = new MatchDto();
        match2.setId(2);
        match2.setStatus("SCHEDULED");

        matchesList.add(match1);
        matchesList.add(match2);

        apiResponse.setMatches(matchesList);

        assertNotNull(apiResponse.getMatches(), "Matches list should not be null after setting");
        assertEquals(2, apiResponse.getMatches().size(), "Matches list size should be 2");
        assertSame(matchesList, apiResponse.getMatches(), "The retrieved list should be the same instance as the set list");
        assertEquals(match1, apiResponse.getMatches().get(0), "First match should be match1");
        assertEquals(match2, apiResponse.getMatches().get(1), "Second match should be match2");
    }

    @Test
    void testSetMatchesWithNull() {
        MatchesApiResponseDto apiResponse = new MatchesApiResponseDto();
        apiResponse.setMatches(null);
        assertNull(apiResponse.getMatches(), "Matches list should be null after setting to null");
    }

    @Test
    void testSetMatchesWithEmptyList() {
        MatchesApiResponseDto apiResponse = new MatchesApiResponseDto();
        apiResponse.setMatches(new ArrayList<>());
        assertTrue(apiResponse.getMatches().isEmpty(), "Matches list should be empty after setting to an empty list");
    }
}