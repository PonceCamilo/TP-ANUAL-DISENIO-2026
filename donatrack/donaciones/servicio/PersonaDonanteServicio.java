package com.donatrack.donaciones.servicio;

import com.donatrack.donaciones.dominio.*;
import com.donatrack.donaciones.dominio.contacto.*;
import com.donatrack.donaciones.dominio.contacto.factory.MedioDeContactoFactory;
import com.donatrack.donaciones.dto.request.*;
import com.donatrack.donaciones.dto.response.PersonaDonanteResponse;
import com.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import com.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import com.donatrack.donaciones.repositorio.PersonaDonanteRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: TEMPLATE METHOD (aplicado dentro de este servicio)
// ───────────────────────────────────────────────────────────────────────────────
// El flujo de registro siempre sigue los mismos pasos:
//   1. validar email no registrado
//   2. construir los medios de contacto        (FACTORY METHOD)
//   3. construir la persona con sus datos       (BUILDER)
//   4. configurar el medio predeterminado
//   5. guardar en el repositorio
//   6. notificar a los observers               (OBSERVER)
//   7. devolver la respuesta
//
// Los métodos privados registrarPersonaHumana/Juridica siguen ese template.
// Si se agregan más tipos, el esqueleto del flujo no cambia.
// ═══════════════════════════════════════════════════════════════════════════════
 
@Service
public class PersonaDonanteServicio {
 
    private final PersonaDonanteRepositorio repositorio;
    private final List<ObservadorDeRegistro> observadores;          // ← OBSERVER: lista de suscriptores
 
    // Spring inyecta todos los ObservadorDeRegistro como @Component.
    // Agregar un nuevo observer = crear la clase, nada más toca acá.
    public PersonaDonanteServicio(PersonaDonanteRepositorio repositorio,
                                   List<ObservadorDeRegistro> observadores) {
        this.repositorio = repositorio;
        this.observadores = observadores;
    }
 
    // ─── TEMPLATE METHOD: flujo de registro de persona humana ────────────────
    public PersonaDonanteResponse registrarPersonaHumana(RegistroPersonaHumanaRequest request) {
 
        // Paso 1: validar
        validarEmailNoRegistrado(request.mediosDeContacto());
 
        // Paso 2: construir medios con FACTORY METHOD
        List<MedioDeContacto> medios = construirMediosDeContacto(request.mediosDeContacto());
 
        // Paso 3: construir persona con BUILDER
        // en lugar de new PersonaHumana(7 params) usamos el builder
        PersonaHumana persona = new PersonaHumanaBuilder()           // ← BUILDER
                .nombre(request.nombre())
                .apellido(request.apellido())
                .edad(request.edad())
                .numeroDocumento(request.numeroDocumento())
                .genero(request.genero())
                .direccion(construirDireccion(request.direccion()))
                .medios(medios)
                .build();
 
        // Paso 4: medio predeterminado
        configurarMedioPredeterminado(persona, request.mediosDeContacto(), medios);
 
        // Paso 5: persistir
        repositorio.guardar(persona);
 
        // Paso 6: notificar observers
        notificarRegistro(persona);                                  // ← OBSERVER: dispara evento
 
        // Paso 7: respuesta
        return PersonaDonanteResponse.desdePersonaHumana(persona);
    }
 
    // ─── TEMPLATE METHOD: flujo de registro de persona jurídica ─────────────
    public PersonaDonanteResponse registrarPersonaJuridica(RegistroPersonaJuridicaRequest request) {
 
        // Paso 1: validar
        validarEmailNoRegistrado(request.mediosDeContacto());
 
        // Paso 2: construir medios con FACTORY METHOD
        List<MedioDeContacto> medios = construirMediosDeContacto(request.mediosDeContacto());
 
        // Paso 3: construir representantes
        List<Representante> representantes = request.representantes().stream()
                .map(r -> new Representante(r.nombre(), r.apellido(), r.email(), r.telefono()))
                .toList();
 
        // Paso 3b: construir persona jurídica con BUILDER
        // LÍNEA CLAVE: el builder acumula datos con nombre explícito, sin confundir parámetros
        PersonaJuridica persona = new PersonaJuridicaBuilder()       // ← BUILDER
                .razonSocial(request.razonSocial())
                .tipo(request.tipo())
                .rubro(request.rubro())
                .medios(medios)
                .representantes(representantes)
                .build();
 
        // Paso 4: medio predeterminado
        configurarMedioPredeterminado(persona, request.mediosDeContacto(), medios);
 
        // Paso 5: persistir
        repositorio.guardar(persona);
 
        // Paso 6: notificar observers
        notificarRegistro(persona);                                  // ← OBSERVER: dispara evento
 
        // Paso 7: respuesta
        return PersonaDonanteResponse.desdePersonaJuridica(persona);
    }
 
    // ─── Consultas ────────────────────────────────────────────────────────────
 
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
 
    // ─── Actualización ────────────────────────────────────────────────────────
 
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
 
    // ─── Baja ─────────────────────────────────────────────────────────────────
 
    public void darDeBaja(UUID id) {
        PersonaDonante persona = buscarOLanzarExcepcion(id);
        persona.darDeBaja();
        repositorio.guardar(persona);
    }
 
    // ─── Helpers privados ─────────────────────────────────────────────────────
 
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
                // LÍNEA CLAVE: ya no hay switch acá, lo delega todo al Factory
                .map(r -> MedioDeContactoFactory.crear(r.tipo(), r.valor()))  // ← FACTORY METHOD
                .toList();
    }
 
    private Direccion construirDireccion(DireccionRequest req) {
        return new Direccion(req.calle(), req.numero(),
                req.localidad(), req.provincia(), req.codigoPostal());
    }
 
    private void configurarMedioPredeterminado(PersonaDonante persona,
                                               List<MedioDeContactoRequest> requests,
                                               List<MedioDeContacto> medios) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).predeterminado()) {
                persona.establecerMedioContactoPredeterminado(medios.get(i));
                return;
            }
        }
    }
 
    // el sujeto (servicio) notifica a todos sus observers
    private void notificarRegistro(PersonaDonante donante) {        // ← OBSERVER: método de disparo
        observadores.forEach(obs -> obs.alRegistrarDonante(donante));
    }
}
