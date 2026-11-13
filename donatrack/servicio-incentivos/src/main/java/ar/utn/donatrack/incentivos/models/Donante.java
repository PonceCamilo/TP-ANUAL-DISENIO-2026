package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.ProgresoMision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "donante")
public class Donante {
    @Id
    private UUID id;

    @ManyToOne
    private CategoriaDonante categoriaActual;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InsigniaObtenida> insigniasObtenidas = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DonacionRegistrada> donaciones = new ArrayList<>();

    @Embedded
    private ProgresoMision progresoMision = new ProgresoMision();

    // Entrega 6: cómo se la nombra en el podio (nombre e inicial del apellido, o
    // razón social). Lo manda Donaciones en cada aviso.
    @Column(name = "nombre_visible", length = 150)
    private String nombreVisible;

    @PostLoad
    private void rehidratarProgresoMision() {
        if (this.progresoMision == null) {
            this.progresoMision = new ProgresoMision();
        }
    }

    public CategoriaDonante getCategoria() {
        return categoriaActual;
    }

    public void setCategoria(CategoriaDonante categoria) {
        this.categoriaActual = categoria;
    }

    public void registrarDonacion(DonacionRegistrada donacion) {
        this.donaciones.add(donacion);
    }

    public void completarMision(Mision mision) {
        agregarInsignia(mision.otorgarInsignia());
    }

    public boolean completoMision(Mision mision) {
        return mision != null && tieneInsignia(mision.getInsignia());
    }

    /** La misma insignia (mismo id o mismo nombre) no se otorga dos veces. */
    public boolean tieneInsignia(Insignia insignia) {
        if (insignia == null) {
            return false;
        }
        return insigniasObtenidas.stream().anyMatch(obtenida -> mismaInsignia(obtenida.getInsignia(), insignia));
    }

    public boolean subirCategoria() {
        CategoriaDonante siguiente = this.categoriaActual.siguienteCategoria();
        if (siguiente != null) {
            this.categoriaActual = siguiente;
            cambiarMisionActual(siguiente.primeraMision());
            return true;
        }
        return false;
    }

    public void agregarInsignia(InsigniaObtenida insignia) {
        if (insignia == null || tieneInsignia(insignia.getInsignia())) {
            return;
        }
        this.insigniasObtenidas.add(insignia);
    }

    /**
     * Una fila por nombre. Si el perfil ya tiene copias de la misma insignia,
     * el panel y el perfil público muestran una sola.
     */
    public List<InsigniaObtenida> insigniasSinRepetir() {
        Map<String, InsigniaObtenida> unicas = new LinkedHashMap<>();
        for (InsigniaObtenida obtenida : insigniasObtenidas) {
            String clave = claveDe(obtenida);
            InsigniaObtenida previa = unicas.get(clave);
            if (previa == null || (!previa.isVisibilidad() && obtenida.isVisibilidad())) {
                unicas.put(clave, obtenida);
            }
        }
        return new ArrayList<>(unicas.values());
    }

    public void cambiarVisibilidadInsignia(Long idInsignia, boolean visible) {
        String nombre = insigniasObtenidas.stream()
                .filter(insignia -> Objects.equals(insignia.getId(), idInsignia))
                .map(this::nombreDe)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        insigniasObtenidas.stream()
                .filter(insignia -> Objects.equals(insignia.getId(), idInsignia)
                        || (nombre != null && nombre.equalsIgnoreCase(nombreDe(insignia))))
                .forEach(insignia -> insignia.setVisibilidad(visible));
    }

    private String claveDe(InsigniaObtenida obtenida) {
        String nombre = nombreDe(obtenida);
        if (nombre != null) {
            return nombre.toLowerCase(Locale.ROOT);
        }
        return "id:" + obtenida.getId();
    }

    private String nombreDe(InsigniaObtenida obtenida) {
        Insignia insignia = obtenida.getInsignia();
        if (insignia == null || insignia.getNombre() == null || insignia.getNombre().isBlank()) {
            return null;
        }
        return insignia.getNombre().strip();
    }

    private boolean mismaInsignia(Insignia guardada, Insignia otra) {
        if (guardada == null || otra == null) {
            return false;
        }
        if (guardada == otra) {
            return true;
        }
        // El nombre manda: dos filas de "Semilla de Solidaridad" son la misma insignia
        // aunque el cargador les haya dado ids distintos. Ids iguales con otro nombre no.
        return guardada.getNombre() != null && guardada.getNombre().equalsIgnoreCase(otra.getNombre());
    }

    public void cambiarMisionActual(Mision mision) {
        this.progresoMision.cambiarMisionActual(mision);
    }

    public int misionesCompletadasEnPeriodo(int mes, int anio) {
        return (int) insigniasObtenidas.stream()
                .filter(insignia -> insignia.getFechaObtencion() != null)
                .filter(insignia -> insignia.getFechaObtencion().getMonthValue() == mes)
                .filter(insignia -> insignia.getFechaObtencion().getYear() == anio)
                .map(this::claveDe)
                .distinct()
                .count();
    }
}
