package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pagamento num dia fixo do mês (1 a 28) ou no último dia do mês.
 */
public class AgendaMensal extends AgendaPagamento {

    static final int ULTIMO_DIA = 0;

    private final int dia;

    AgendaMensal(String descricao, int dia) {
        super(descricao);
        this.dia = dia;
    }

    @Override
    public boolean deveReceberEm(LocalDate data) {
        return data.getDayOfMonth() == diaDePagamento(data);
    }

    @Override
    public LocalDate inicioPeriodo(LocalDate dataPagamento) {
        if (dia == ULTIMO_DIA) {
            return dataPagamento.withDayOfMonth(1);
        }

        return dataPagamento.minusMonths(1).plusDays(1);
    }

    @Override
    public BigDecimal valorProporcional(BigDecimal valorMensal) {
        return valorMensal;
    }

    private int diaDePagamento(LocalDate data) {
        return dia == ULTIMO_DIA ? data.lengthOfMonth() : dia;
    }
}
