package ar.utn.donatrack.incentivos.clients;

import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import java.util.UUID;

public interface DonacionesClient {
    DonanteInfoDTO obtenerInfoDonante(UUID idDonante);
}

// Esto es momentaneo para que no tire errores pero hay que ver la conexion.