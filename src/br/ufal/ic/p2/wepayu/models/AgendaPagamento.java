package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agenda de pagamento de um empregado.
 *
 * Define em quais datas o empregado recebe, qual período cada pagamento
 * cobre e qual parcela do salário mensal corresponde a esse período.
 * As variações (semanal e mensal) são resolvidas por polimorfismo.
 */
public abstract class AgendaPagamento implements Serializable {

    private final String descricao;

    protected AgendaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** @return {@code true} se há pagamento na data informada */
    public abstract boolean deveReceberEm(LocalDate data);

    /** @return primeiro dia do período coberto pelo pagamento da data */
    public abstract LocalDate inicioPeriodo(LocalDate dataPagamento);

    /**
     * Converte um valor mensal na parcela correspondente a um pagamento
     * desta agenda.
     *
     * @param valorMensal salário mensal
     * @return valor proporcional ao período, truncado em duas casas
     */
    public abstract BigDecimal valorProporcional(BigDecimal valorMensal);

    /** Agenda padrão de horistas: toda sexta-feira. */
    public static AgendaPagamento semanalPadrao() {
        return new AgendaSemanal("semanal 5", 1, 5);
    }

    /** Agenda padrão de assalariados: último dia útil do mês. */
    public static AgendaPagamento mensalPadrao() {
        return new AgendaMensal("mensal $", AgendaMensal.ULTIMO_DIA);
    }

    /** Agenda padrão de comissionados: sexta-feira, a cada duas semanas. */
    public static AgendaPagamento bissemanalPadrao() {
        return new AgendaSemanal("semanal 2 5", 2, 5);
    }
}
