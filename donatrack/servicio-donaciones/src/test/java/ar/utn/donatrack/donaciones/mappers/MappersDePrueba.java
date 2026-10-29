package ar.utn.donatrack.donaciones.mappers;

/** Los mappers reales, armados como los arma Spring, para los tests que no levantan el contexto. */
public final class MappersDePrueba {

    public static final DireccionMapper DIRECCIONES = new DireccionMapper();
    public static final ContactoMapper CONTACTOS = new ContactoMapper();
    public static final RepresentanteMapper REPRESENTANTES = new RepresentanteMapper(CONTACTOS);

    private MappersDePrueba() {
    }

    public static PersonaDonanteMapper personas() {
        return new PersonaDonanteMapper(DIRECCIONES, CONTACTOS, REPRESENTANTES);
    }

    public static EntidadBeneficiariaMapper entidades() {
        return new EntidadBeneficiariaMapper(DIRECCIONES, CONTACTOS, REPRESENTANTES);
    }
}
