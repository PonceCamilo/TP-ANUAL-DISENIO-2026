package ar.utn.donatrack.incentivos.models.Donante;

import ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.Donante.Insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Donante {
    private UUID id;
    private CategoriaDonante categoria;
    private Mision misionActual;
    private int progresoMisionActual;
    private List<InsigniaObtenida> insigniasObtenidas = new ArrayList<>();

    public void addInsignia(InsigniaObtenida insignia) {
        this.insigniasObtenidas.add(insignia);
    }
}