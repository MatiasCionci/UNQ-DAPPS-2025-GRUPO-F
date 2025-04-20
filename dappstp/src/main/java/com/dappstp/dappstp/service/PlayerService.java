package com.dappstp.dappstp.service;

import com.dappstp.dappstp.model.Player; 
import java.util.List; 

public interface PlayerService {

    /**
     * Obtiene una lista de jugadores filtrados por el nombre del equipo.
     *
     * @param teamName El nombre del equipo por el cual filtrar.
     * @return Una lista de objetos Player que pertenecen al equipo especificado.
     *         Devuelve una lista vacía si no se encuentran jugadores para ese equipo
     *         o si teamName es nulo o vacío.
     */
    List<Player> getPlayersByTeam(String teamName); 

}

