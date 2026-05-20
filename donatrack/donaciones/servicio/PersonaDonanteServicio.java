package com.donatrack.donaciones.servicio;

import com.donatrack.donaciones.dominio.PersonaDonante;
import com.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import com.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import com.donatrack.donaciones.notificacion.ObservadorDeRegistro;
import com.donatrack.donaciones.repositorio.PersonaDonanteRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PersonaDonanteServicio {

    private final PersonaDonanteRepositorio repositorio;
    private final List<ObservadorDeRegistro> observadores;

    public PersonaDonanteServicio(PersonaDonanteRepositorio repositorio,
                                  List<ObservadorDeRegistro> observadores) {
        this.repositorio = repositorio;
        this.observadores = observadores;
    }

    public PersonaDonante registrar(PersonaDonante donante) {
        if (repositorio.existePorEmail(
                donante.getMedioContactoPredeterminado().getValor())) {
            throw new EmailYaRegistradoException(
                donante.getMedioContactoPredeterminado().getValor());
        }
        repositorio.guardar(donante);
        observadores.forEach(obs -> obs.alRegistrarDonante(donante));
        return donante;
    }

    public PersonaDonante buscarPorId(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new PersonaDonanteNoEncontradaException(id));
    }

    public void darDeBaja(UUID id) {
        PersonaDonante donante = buscarPorId(id);
        donante.darDeBaja();
        repositorio.guardar(donante);
    }

    public void reactivar(UUID id) {
        PersonaDonante donante = buscarPorId(id);
        donante.reactivar();
        repositorio.guardar(donante);
    }

    public List<PersonaDonante> listarActivos() {
        return repositorio.buscarActivos();
    }

    public List<PersonaDonante> listarTodos() {
        return repositorio.buscarTodos();
    }
}