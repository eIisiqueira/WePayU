package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Pagamento num dia da semana, a cada N semanas, contadas a partir do
 * início do sistema (01/01/2005). O primeiro pagamento ocorre na N-ésima
 * ocorrência do dia da semana a partir dessa data.
 */
public class AgendaSemanal extends AgendaPagamento {

    private static final LocalDate INICIO = LocalDate.of(2005, 1, 1);
    private static final BigDecimal MESES_NO_ANO = new BigDecimal("12");
    private static final BigDecimal SEMANAS_NO_ANO = new BigDecimal("52");

    private final int intervaloSemanas;
    private final LocalDate primeiroPagamento;

    AgendaSemanal(String descricao, int intervaloSemanas, int diaSemana) {
        super(descricao);
        this.intervaloSemanas = intervaloSemanas;
        this.primeiroPagamento = INICIO
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(diaSemana)))
                .plusWeeks(intervaloSemanas - 1);
    }

    @Override
    public boolean deveReceberEm(LocalDate data) {
        if (data.isBefore(primeiroPagamento)) {
            return false;
        }

        long dias = ChronoUnit.DAYS.between(primeiroPagamento, data);

        return dias % (7L * intervaloSemanas) == 0;
    }

    @Override
    public LocalDate inicioPeriodo(LocalDate dataPagamento) {
        return dataPagamento.minusDays(7L * intervaloSemanas - 1);
    }

    @Override
    public BigDecimal valorProporcional(BigDecimal valorMensal) {
        return valorMensal
                .multiply(MESES_NO_ANO)
                .multiply(BigDecimal.valueOf(intervaloSemanas))
                .divide(SEMANAS_NO_ANO, 2, RoundingMode.DOWN);
    }
}
