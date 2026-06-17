//TODO package y import

@Repository
public class RankingMensualRepository implements RankingMensualRepositoryInterface {

    private final List<RankingMensual> historial = new ArrayList<>();

    @Override
    public void guardar(RankingMensual ranking) {
        historial.add(ranking);
    }

    @Override
    public Optional<RankingMensual> buscarPorPeriodo(int mes, int anio) { //Si no lo encuentra devuelve vacio por eco opicional
        return historial.stream()
                .filter(r -> r.getMes() == mes && r.getAnio() == anio)
                .findFirst();
    }

    @Override
    public List<RankingMensual> obtenerHistorial() {
        return historial;
    }
}
