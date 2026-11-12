package ar.utn.donatrack.incentivos.dtos.response;

import java.util.UUID;

public record PuestoRankingResponse(int puesto, UUID donanteId, int misionesCompletadas) {}
