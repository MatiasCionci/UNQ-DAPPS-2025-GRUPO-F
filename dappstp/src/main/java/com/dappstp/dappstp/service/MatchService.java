package com.dappstp.dappstp.service;

import java.util.List;

import com.dappstp.dappstp.model.UpcomingMatch;

public interface MatchService {
    /**
     * Scrapea los próximos partidos de un equipo, sincroniza la base de datos
     * (inserta nuevos, actualiza cambios, marca jugados) y devuelve la lista resultante.
     *
     * @param teamId el ID del equipo a procesar
     * @return lista de UpcomingMatch ya sincronizados en BD
     */
    List<UpcomingMatch> getAndPersistUpcomingMatches(Long teamId);
}
