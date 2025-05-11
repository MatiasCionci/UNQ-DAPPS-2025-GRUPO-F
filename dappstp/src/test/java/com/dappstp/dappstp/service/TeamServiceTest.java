package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dappstp.dappstp.factory.TeamFactory;
import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.repository.TeamRepository;
import com.dappstp.dappstp.service.impl.TeamServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepo;

    @InjectMocks
    private TeamServiceImpl teamService;

    private Team psg;
    private Team arsenal;

    @BeforeEach
    void setUp() {
        psg     = TeamFactory.createPSG();
        arsenal = TeamFactory.createArsenal();
    }

    @Test
    void testFindAll() {
        when(teamRepo.findAll()).thenReturn(List.of(psg, arsenal));

        List<Team> result = teamService.findAll();

        assertEquals(2, result.size(), "Should return two teams");
    }

    @Test
    void testFindById_Success() {
        when(teamRepo.findById(psg.getId())).thenReturn(Optional.of(psg));

        Team result = teamService.findById(psg.getId());

        assertEquals("Paris Saint-Germain", result.getName(), "Name should match factory PSG");
    }

    @Test
    void testFindById_NotFound() {
        when(teamRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> teamService.findById(999L),
            "Should throw EntityNotFoundException when ID does not exist"
        );
    }

    @Test
    void testCreate() {
        when(teamRepo.save(arsenal)).thenReturn(arsenal);

        Team result = teamService.create(arsenal);

        assertEquals("Arsenal", result.getName(), "Created team name should match");
    }

    @Test
    void testUpdate() {
        Team updated = new Team(null, "PSG Updated", "http://updated.url");
        when(teamRepo.findById(psg.getId())).thenReturn(Optional.of(psg));
        when(teamRepo.save(psg)).thenReturn(psg);

        Team result = teamService.update(psg.getId(), updated);

        assertEquals("PSG Updated", result.getName(), "Updated team name should be applied");
    }

    @Test
    void testDelete() {
        when(teamRepo.findById(psg.getId())).thenReturn(Optional.of(psg));

        teamService.delete(psg.getId());

        verify(teamRepo).delete(psg);
    }
}
