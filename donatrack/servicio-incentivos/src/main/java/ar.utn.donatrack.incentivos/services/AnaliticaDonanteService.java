// TODO

@Service
@RequiredArgsConstructor
public class AnaliticaDonanteService {

    private final DonacionesQueryPort donacionesQueryPort;

    public MetricasDonante calcularMetricas(UUID donanteId) {
        List<DonacionResumen> donaciones = donacionesQueryPort.obtenerDonacionesDe(donanteId);

        int totalHistorico = donaciones.size();

        long organizacionesAyudadas = donaciones.stream()
                .map(DonacionResumen::entidadBeneficiariaId) //ver biern estas entidades
                .distinct() //elimina los elementos duplicados
                .count(); //es como un length

        List<EvolucionPeriodo> evolucion = donaciones.stream()
                .collect(Collectors.groupingBy( //{[Año 2023, Mes 5] = 14 donaciones, [Año 2024, Mes 1] = 8 donaciones}
                        d -> Map.entry(d.fecha().getYear(), d.fecha().getMonthValue()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> EvolucionPeriodo.builder()
                        .anio(entry.getKey().getKey())
                        .mes(entry.getKey().getValue())
                        .cantidadDonaciones(entry.getValue().intValue())
                        .build())
                .sorted(Comparator.comparing(EvolucionPeriodo::getAnio) //Ordena cronologicamente primero compara anio y despues mes
                        .thenComparing(EvolucionPeriodo::getMes))
                .toList();

        return MetricasDonante.builder() //TODO CHECKEAR DONACIONES
                .donanteId(donanteId)
                .totalDonacionesHistoricas(totalHistorico)
                .organizacionesAyudadas((int) organizacionesAyudadas)
                .evolucionPorPeriodo(evolucion)
                .build();
    }
}