package com.dappstp.dappstp.factory;

import com.dappstp.dappstp.model.Player; 

public class PlayerFactory {

    public static Player crearJugadorTop() {
        return new Player("Messi", "Inter Miami", 30, 25, 18, 9.2);
    }

    public static Player crearJugadorPromesa() {
        return new Player("Lamine Yamal", "Barcelona", 10, 4, 3, 7.0);
    }

    public static Player crearJugadorConEstadisticasBajas() {
        return new Player("Stefan Ortega", "Manchester City", 2, 0, 0, 5.0);
    }
}
