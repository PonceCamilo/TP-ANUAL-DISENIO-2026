package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
public class ProgresoRacha {

    public int mesesConsecutivosDonando(Donante donante, LocalDate fechaReferencia) {
        return mesesConsecutivosDonando(mesesDeDonaciones(donante), fechaReferencia);
    }

    int mesesConsecutivosDonando(Set<YearMonth> meses, LocalDate fechaReferencia) {
        YearMonth cursor = meses.stream()
                .max(Comparator.naturalOrder())
                .orElse(YearMonth.from(fechaReferencia));
        if (cursor.isBefore(YearMonth.from(fechaReferencia).minusMonths(1))) {
            return 0;
        }
        int consecutivos = 0;

        while (meses.contains(cursor)) {
            consecutivos++;
            cursor = cursor.minusMonths(1);
        }

        return consecutivos;
    }

    public boolean pasoUnMesCompletoSinDonaciones(Donante donante, LocalDate fechaReferencia) {
        return pasoUnMesCompletoSinDonaciones(mesesDeDonaciones(donante), fechaReferencia);
    }

    boolean pasoUnMesCompletoSinDonaciones(Set<YearMonth> meses, LocalDate fechaReferencia) {
        YearMonth mesAnterior = YearMonth.from(fechaReferencia).minusMonths(1);
        YearMonth ultimoMesConDonacion = meses.stream()
                .max(Comparator.naturalOrder())
                .orElse(mesAnterior);

        return ultimoMesConDonacion.isBefore(mesAnterior);
    }

    private Set<YearMonth> mesesDeDonaciones(Donante donante) {
        return donante.getDonaciones().stream()
                .map(donacion -> YearMonth.from(donacion.getFecha()))
                .collect(Collectors.toSet());
    }
}
