package com.dappstp.dappstp.service;

import java.util.List;

import com.dappstp.dappstp.model.Team;

public interface TeamService {
    List<Team> findAll();

    Team findById(Long id);

    Team create(Team team);

    Team update(Long id, Team team);
    
    void delete(Long id);
}
