package com.donatrack.donaciones.servicio;

import com.donatrack.donaciones.dominio.*;
import com.donatrack.donaciones.dominio.contacto.*;
import com.donatrack.donaciones.dto.request.*;
import com.donatrack.donaciones.dto.response.PersonaDonanteResponse;
import com.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import com.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import com.donatrack.donaciones.repositorio.PersonaDonanteRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PersonaDonanteServicio {

    private final PersonaDonanteRepositorio repositorio;

    public PersonaDonanteServicio(PersonaDonanteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // ─── Registro ──────────────────────────────────────────────────────────────

    public PersonaDonanteResponse registrarPersonaHumana(RegistroPersonaHumanaRequest request) {
        validarEmailNoRegistrado(request.mediosDeContacto());

        List<MedioDeContacto> medios = construirMediosDeContacto(request.mediosDeContacto());
        Direccion direccion = construirDireccion(request.direccion());

        PersonaHumana persona = new PersonaHumana(
                request.nombre(),
                request.apellido(),
                request.edad(),
                request.numeroDocumento(),
                request.genero(),
                direccion,
                medios
        );

        configurarMedioPrederminado(persona, request.mediosDeContacto(), medios);
        repositorio.guardar(persona);
        return PersonaDonanteResponse.desdePersonaHumana(persona);
    }

    public PersonaDonanteResponse registrarPersonaJuridica(RegistroPersonaJuridicaRequest request) {
        validarEmailNoRegistrado(request.mediosDeContacto());

        List<MedioDeContacto> medios = construirMediosDeContacto(request.mediosDeContacto());
        List<Representante> representantes = request.representantes().stream()
                .map(r -> new Representante(r.nombre(), r.apellido(), r.email(), r.telefono()))
                .toList();

        PersonaJuridica persona = new PersonaJuridica(
                request.razonSocial(),
                request.tipo(),
                request.rubro(),
                medios,
                representantes
        );

        configurarMedioPrederminado(persona, request.mediosDeContacto(), medios);
        repositorio.guardar(persona);
        return PersonaDonanteResponse.desdePersonaJuridica(persona);
    }

    // ─── Consultas ─────────────────────────────────────────────────────────────

    public PersonaDonanteResponse obtenerPorId(UUID id) {
        PersonaDonante persona = buscarOLanzarExcepcion(id);
        return PersonaDonanteResponse.desde(persona);
    }

    public List<PersonaDonanteResponse> listarActivos() {
        return repositorio.buscarActivos().stream()
                .map(PersonaDonanteResponse::desde)
                .toList();
    }

    public List<PersonaDonanteResponse> listarTodos() {
        return repositorio.buscarTodos().stream()
                .map(PersonaDonanteResponse::desde)
                .toList();
    }

    // ─── Actualización ─────────────────────────────────────────────────────────

    public PersonaDonanteResponse actualizarPersonaHumana(UUID id,
                                                          RegistroPersonaHumanaRequest request) {
        PersonaDonante persona = buscarOLanzarExcepcion(id);

        if (!(persona instanceof PersonaHumana ph)) {
            throw new IllegalArgumentException(
                    "La persona con ID " + id + " no es una persona humana.");
        }

        Direccion direccion = construirDireccion(request.direccion());
        ph.actualizarDatos(request.nombre(), request.apellido(),
                request.edad(), request.genero(), direccion);
        repositorio.guardar(ph);
        return PersonaDonanteResponse.desdePersonaHumana(ph);
    }

    public PersonaDonanteResponse actualizarPersonaJuridica(UUID id,
                                                            RegistroPersonaJuridicaRequest request) {
        PersonaDonante persona = buscarOLanzarExcepcion(id);

        if (!(persona instanceof PersonaJuridica pj)) {
            throw new IllegalArgumentException(
                    "La persona con ID " + id + " no es una persona jurídica.");
        }

        pj.actualizarDatos(request.razonSocial(), request.tipo(), request.rubro());
        repositorio.guardar(pj);
        return PersonaDonanteResponse.desdePersonaJuridica(pj);
    }

    // ─── Baja ──────────────────────────────────────────────────────────────────

    public void darDeBaja(UUID id) {
        PersonaDonante persona = buscarOLanzarExcepcion(id);
        persona.darDeBaja();
        repositorio.guardar(persona);
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private PersonaDonante buscarOLanzarExcepcion(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new PersonaDonanteNoEncontradaException(id));
    }

    private void validarEmailNoRegistrado(List<MedioDeContactoRequest> medios) {
        medios.stream()
                .filter(m -> m.tipo() == TipoMedioContacto.EMAIL)
                .forEach(m -> {
                    if (repositorio.existePorEmail(m.valor())) {
                        throw new EmailYaRegistradoException(m.valor());
                    }
                });
    }

    private List<MedioDeContacto> construirMediosDeContacto(
            List<MedioDeContactoRequest> requests) {
        return requests.stream()
                .map(this::construirMedio)
                .toList();
    }

    private MedioDeContacto construirMedio(MedioDeContactoRequest request) {
        return switch (request.tipo()) {
            case EMAIL    -> new Email(request.valor());
            case TELEFONO -> new Telefono(request.valor());
            case WHATSAPP -> new WhatsApp(request.valor());
        };
    }

    private Direccion construirDireccion(DireccionRequest req) {
        return new Direccion(req.calle(), req.numero(),
                req.localidad(), req.provincia(), req.codigoPostal());
    }

    /**
     * Si el cliente marcó algún medio como predeterminado, lo configura.
     * De lo contrario, el dominio ya asigna el email como predeterminado por defecto.
     */
    private void configurarMedioPrederminado(PersonaDonante persona,
                                              List<MedioDeContactoRequest> requests,
                                              List<MedioDeContacto> medios) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).predeterminado()) {
                persona.establecerMedioContactoPredeterminado(medios.get(i));
                return;
            }
        }
    }
}
