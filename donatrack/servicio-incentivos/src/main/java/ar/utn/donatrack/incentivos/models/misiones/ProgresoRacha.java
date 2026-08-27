package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ProgresoRacha {

    public int mesesConsecutivosDonando(Donante donante, LocalDate fechaReferencia) {
        YearMonth cursor = YearMonth.from(fechaReferencia);
        int meses = 0;

        while (donacionesEnPeriodo(donante, cursor) > 0) {
            meses++;
            cursor = cursor.minusMonths(1);
        }

        return meses;
    }

    public boolean pasoUnMesCompletoSinDonaciones(Donante donante, LocalDate fechaReferencia) {
        List<DonacionRegistrada> donaciones = donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .toList();

        if (donaciones.isEmpty()) {
            return false;
        }

        YearMonth mesAnterior = YearMonth.from(fechaReferencia).minusMonths(1);
        YearMonth ultimoMesConDonacion = donaciones.stream()
                .map(donacion -> YearMonth.from(donacion.getFecha()))
                .max(Comparator.naturalOrder())
                .orElse(mesAnterior);

        return ultimoMesConDonacion.isBefore(mesAnterior);
    }

    private int donacionesEnPeriodo(Donante donante, YearMonth periodo) {
        return (int) donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .filter(donacion -> YearMonth.from(donacion.getFecha()).equals(periodo))
                .count();
    }
}
