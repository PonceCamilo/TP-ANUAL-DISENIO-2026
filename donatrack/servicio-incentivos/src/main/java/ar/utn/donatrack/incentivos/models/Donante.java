package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.ProgresoMision;
import jakarta.persistence.CascadeType;
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
import java.util.List;
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

    /**
     * Vuelve a crear el progreso de misión cuando Hibernate lo trae en null.
     *
     * POR QUÉ ES NECESARIO: ProgresoMision es un @Embedded cuya única columna es
     * la misión actual. Si esa columna está en NULL —un donante que todavía no
     * arrancó ninguna misión— Hibernate deja el embeddable ENTERO en null y pisa
     * el `= new ProgresoMision()` del campo, que solo corre al construir el
     * objeto en memoria.
     *
     * Sin esto, cualquier donante leído de la base sin misión actual tiraba
     * NullPointerException al subir de categoría, que es el flujo central del
     * servicio. Lo detectó MapeoIncentivosTest.subirCategoriaYGuardar.
     *
     * Es el mismo criterio de rehidratación que usa servicio-donaciones para el
     * patrón State y servicio-notificaciones para el medio.
     */
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
        this.insigniasObtenidas.add(insignia);
    }

    public void cambiarVisibilidadInsignia(UUID idInsignia, boolean visible) {
        insigniasObtenidas.stream()
                .filter(insignia -> insignia.getId().equals(idInsignia))
                .findFirst()
                .ifPresent(insignia -> insignia.setVisibilidad(visible));
    }

    public void cambiarMisionActual(Mision mision) {
        this.progresoMision.cambiarMisionActual(mision);
    }

    public int misionesCompletadasEnPeriodo(int mes, int anio) {
        return (int) insigniasObtenidas.stream()
                .filter(insignia -> insignia.getFechaObtencion() != null)
                .filter(insignia -> insignia.getFechaObtencion().getMonthValue() == mes)
                .filter(insignia -> insignia.getFechaObtencion().getYear() == anio)
                .count();
    }
}
