package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dappstp.dappstp.service.impl.PlayerServiceImpl; // Add this import

import com.dappstp.dappstp.model.Player;
import java.util.List;

public class PlayerServiceTest {
    
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerServiceImpl();
    }



}
