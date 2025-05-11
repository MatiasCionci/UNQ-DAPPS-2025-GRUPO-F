package com.dappstp.dappstp.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.repository.TeamRepository;
import com.dappstp.dappstp.service.TeamService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepo;

    public TeamServiceImpl(TeamRepository teamRepo) {
        this.teamRepo = teamRepo;
    }

    @Override
    public List<Team> findAll() {
        return teamRepo.findAll();
    }

    @Override
    public Team findById(Long id) {
        return teamRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Team no existe"));
    }

    @Override
    public Team create(Team team) {
        return teamRepo.save(team);
    }

    @Override
    public Team update(Long id, Team team) {
        Team existing = findById(id);
        existing.setName(team.getName());
        existing.setWhoscoredUrl(team.getWhoscoredUrl());
        return teamRepo.save(existing);
    }

    @Override
    public void delete(Long id) {
        Team existing = findById(id);
        teamRepo.delete(existing);
    }
}

