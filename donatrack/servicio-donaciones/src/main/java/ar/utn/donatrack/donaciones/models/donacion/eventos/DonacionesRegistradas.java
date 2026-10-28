package ar.utn.donatrack.donaciones.models.donacion.eventos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Una persona donante registró una carga (que pudo segmentarse en varias
 * donaciones). Lo publica el caso de uso de registro: no es el evento de una
 * donación sino de la carga entera.
 */
public record DonacionesRegistradas(UUID idDonante, int cantidadBienes, List<String> categorias,
                                    LocalDateTime cuando) {

    public DonacionesRegistradas {
        categorias = List.copyOf(categorias);
    }
}
