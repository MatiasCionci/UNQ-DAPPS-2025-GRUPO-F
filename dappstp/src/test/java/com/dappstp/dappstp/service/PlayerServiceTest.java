package com.dappstp.dappstp.service;

import org.junit.jupiter.api.BeforeEach;

import com.dappstp.dappstp.service.impl.PlayerServiceImpl; // Add this import

public class PlayerServiceTest {
    
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerServiceImpl();
    }



}
