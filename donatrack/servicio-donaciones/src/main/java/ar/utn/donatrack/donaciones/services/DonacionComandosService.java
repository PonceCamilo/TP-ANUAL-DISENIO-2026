package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Operaciones que cambian una donación ya registrada: estado, bienes,
 * asignación y baja (el registro está en SegmentadorDonacionesService).
 *
 * Cada una lee la donación, le pide el cambio al modelo y la guarda, en una
 * transacción. Lo que depende de cada cambio (la bandeja de candidatas, las
 * necesidades de la entidad, los avisos) lo hacen los oyentes de sus eventos.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class DonacionComandosService {

    private final RepositorioDonaciones repositorio;
    private final DonacionMapper mapper;
    private final DonacionesValidator validador;
    private final EntidadesBeneficiariasValidator entidadesValidator;

    /**
     * Cambio de estado manual. Lo que depende del cambio lo hacen los oyentes de
     * sus eventos (paquete eventos): si la donación dejó el depósito su ranking sale
     * de la bandeja, y si llegó a ENTREGADA se imputa a las necesidades de la
     * entidad y se avisa a incentivos.
     */
    public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
        Donacion donacion = validador.validarYObtenerDonacion(id);
        donacion.cambiarEstadoManualmente(dto.getEstado(), dto.getJustificacion());
        repositorio.guardar(donacion);
    }

    /** Corrige un bien puntual de la donación (ver Donacion.reemplazarBien para las reglas). */
    public DonacionResponseDTO modificarBien(UUID id, UUID idBien, BienRequestDTO dto) {
        Donacion donacion = validador.validarYObtenerDonacion(id);
        donacion.reemplazarBien(idBien, mapper.toBien(dto));
        repositorio.guardar(donacion);
        return mapper.toDTO(donacion);
    }

    /**
     * Endpoint anterior (PATCH /donaciones/{id}/bien): reemplazaba siempre el
     * primer bien. Ahora solo funciona si la donación tiene un único bien; con
     * varios hay que indicar cuál.
     */
    public void modificarUnicoBien(UUID id, BienRequestDTO dto) {
        Donacion donacion = validador.validarYObtenerDonacion(id);
        donacion.reemplazarUnicoBien(mapper.toBien(dto));
        repositorio.guardar(donacion);
    }

    public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
        Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
        donacion.exigirSinBienesVencidosAl(FechaHoraArgentina.hoy());
        EntidadBeneficiaria entidad = entidadesValidator.validarYObtenerEntidad(dto.getIdEntidadBeneficiaria());

        donacion.asignarA(entidad);
        // Al guardar, el repositorio también suma la recepción a la entidad, en la
        // misma transacción. Los oyentes sacan la donación de la bandeja y, después
        // de confirmar, avisan a la persona donante y a la entidad.
        repositorio.guardar(donacion);
    }

    /**
     * Baja lógica: la donación queda guardada con su historial y desaparece de las
     * consultas. Al guardarla se descuenta su recepción, y su ranking sale de la
     * bandeja por el evento de baja.
     */
    public void eliminar(UUID id) {
        Donacion donacion = validador.validarYObtenerDonacion(id);
        donacion.darDeBaja();
        repositorio.guardar(donacion);
    }
}
