package com.dappstp.dappstp.factory;

import com.dappstp.dappstp.model.Team;

public class TeamFactory {
    
    public static Team createPSG() {
        return new Team(
            304L,
            "Paris Saint-Germain",
            "https://es.whoscored.com/teams/304/fixtures/francia-paris-saint-germain"
        );
    }

    public static Team createArsenal() {
        return new Team(
            1L,
            "Arsenal",
            "https://www.whoscored.com/Teams/13"
        );
    }
}
