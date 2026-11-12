package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import ar.utn.donatrack.donaciones.salida.Destinos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Avisos al Servicio de Autenticación (Entrega 6, requisito 20): las personas
 * importadas por CSV necesitan un usuario para entrar.
 *
 * Donaciones no crea cuentas ni maneja contraseñas: informa qué personas importó
 * (su id, que pasa a ser el perfilId de la cuenta, su email, su nombre y su tipo)
 * y Autenticación decide cómo darles acceso (por ejemplo, un enlace de activación
 * que manda Notificaciones). El aviso va por la bandeja de salida, en la misma
 * transacción que guarda las personas: si el lote no se guarda, no se avisa.
 *
 * Contrato propuesto (EnviadorAutenticacion): POST /api/autenticacion/cuentas/importadas
 * con {"donantes":[{"donanteId","email","nombre","tipoDonante"}]}. Tiene que ser
 * idempotente por email: reimportar un archivo vuelve a avisar por las mismas
 * personas.
 *
 * Mientras donaciones.cuentas.avisar-importados esté apagado no se encola nada.
 */
@Component
@RequiredArgsConstructor
public class AutenticacionClient {

    private final BandejaDeSalida salida;
    private final DonacionesProperties propiedades;

    public boolean avisaImportados() {
        return propiedades.cuentas().avisarImportados();
    }

    /** Encola un aviso cada personasPorAviso personas. Sin personas, o apagado, no hace nada. */
    public void notificarImportados(List<DonanteImportado> donantes) {
        if (!avisaImportados() || donantes.isEmpty()) {
            return;
        }
        int porAviso = propiedades.cuentas().personasPorAviso();
        for (int desde = 0; desde < donantes.size(); desde += porAviso) {
            List<DonanteImportado> tanda = List.copyOf(donantes.subList(desde, Math.min(desde + porAviso, donantes.size())));
            salida.encolar(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS, new CuentasImportadas(tanda));
        }
    }

    /** Una persona importada: su id en Donaciones es el perfilId que tiene que llevar su cuenta. */
    public record DonanteImportado(UUID donanteId, String email, String nombre, String tipoDonante) {}

    /** Cuerpo del aviso. */
    public record CuentasImportadas(List<DonanteImportado> donantes) {}
}
