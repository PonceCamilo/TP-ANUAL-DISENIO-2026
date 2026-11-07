package ar.utn.donatrack.logistica.interfaces.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;

/** Observer: cada listener decide qué hacer con un hecho de logística (p. ej. avisarle al broker de logística). */
public interface EntregaEventListener {
    void onEvento(EntregaEvento evento);
}
