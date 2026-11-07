package ar.utn.donatrack.donaciones.services.logistica;

import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aviso de logística: el chofer inició el recorrido. Las donaciones de la ruta
 * pasan a EN_TRASLADO y se avisa a sus donantes y entidades, con el link al mapa
 * de seguimiento en tiempo real.
 *
 * Regla arquitectónica (Entrega 3): el Servicio de Logística NO llama
 * directamente a notificaciones ni a incentivos; deja disponible la
 * información y es el Servicio de Donaciones quien orquesta los avisos.
 *
 * Cada aviso es una transacción y es idempotente: un aviso repetido responde
 * bien sin cambiar nada ni volver a notificar.
 * Si una donación de la ruta no puede pasar, no pasa ninguna.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class ManejadorInicioDeRuta {

    private final RepositorioDonaciones donaciones;
    private final ParticipantesDeDonaciones participantes;
    private final Notificador notificador;

    public void procesar(InicioRutaCallbackDTO dto) {
        log.info("[LOGISTICA] Inicio de ruta {}. Donaciones involucradas: {}", dto.idRuta(), dto.idsDonaciones().size());

        List<Donacion> deLaRuta = obtenerTodasOFallar(dto.idsDonaciones());
        ParticipantesDeDonaciones.Lote lote = participantes.de(deLaRuta);

        for (Donacion donacion : deLaRuta) {
            if (!donacion.iniciarTraslado("Chofer inició el recorrido. Ruta: " + dto.idRuta())) {
                log.info("[LOGISTICA] Inicio de ruta repetido para la donación {}: sin cambios.", donacion.getId());
                continue;
            }
            donaciones.guardar(donacion);
            notificador.inicioDeRuta(lote.donanteDe(donacion), lote.entidadDe(donacion), dto.urlMapaInteractivo());
        }
    }

    /** Las donaciones de la ruta, en el orden del aviso y sin repetir; 404 si falta alguna. */
    private List<Donacion> obtenerTodasOFallar(List<UUID> ids) {
        Set<UUID> pedidas = new LinkedHashSet<>(ids);
        Map<UUID, Donacion> encontradas = donaciones.obtenerPorIds(pedidas).stream()
                .collect(Collectors.toMap(Donacion::getId, Function.identity()));
        List<Donacion> resultado = new ArrayList<>();
        for (UUID id : pedidas) {
            Donacion donacion = encontradas.get(id);
            if (donacion == null) {
                throw new DonacionNoEncontradaException(id);
            }
            resultado.add(donacion);
        }
        return resultado;
    }
}
