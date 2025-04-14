package com.dappstp.dappstp.service;

import java.util.List;

import com.dappstp.dappstp.model.Player;

public interface PlayerService {

    List<Player> getPlayersByTeam(String teamName);
}
