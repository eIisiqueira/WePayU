package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import br.ufal.ic.p2.wepayu.exception.DescricaoAgendaInvalidaException;

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

    /**
     * Interpreta a descrição textual de uma agenda.
     * Formatos aceitos: "semanal D", "semanal N D" (N de 1 a 52, D de 1 a 7)
     * e "mensal M" (M de 1 a 28, ou "$" para o último dia do mês).
     *
     * @param descricao descrição da agenda
     * @return agenda correspondente
     * @throws DescricaoAgendaInvalidaException se a descrição for inválida
     */
    public static AgendaPagamento deDescricao(String descricao)
            throws DescricaoAgendaInvalidaException {

        if (descricao == null) {
            throw new DescricaoAgendaInvalidaException();
        }

        String[] partes = descricao.trim().split("\\s+");

        if ("semanal".equals(partes[0]) && partes.length == 2) {
            return new AgendaSemanal(descricao, 1,
                    numeroEntre(partes[1], 1, 7));
        }

        if ("semanal".equals(partes[0]) && partes.length == 3) {
            return new AgendaSemanal(descricao,
                    numeroEntre(partes[1], 1, 52),
                    numeroEntre(partes[2], 1, 7));
        }

        if ("mensal".equals(partes[0]) && partes.length == 2) {
            int dia = "$".equals(partes[1])
                    ? AgendaMensal.ULTIMO_DIA
                    : numeroEntre(partes[1], 1, 28);

            return new AgendaMensal(descricao, dia);
        }

        throw new DescricaoAgendaInvalidaException();
    }

    private static int numeroEntre(String texto, int minimo, int maximo)
            throws DescricaoAgendaInvalidaException {

        try {
            int numero = Integer.parseInt(texto);

            if (numero < minimo || numero > maximo) {
                throw new DescricaoAgendaInvalidaException();
            }

            return numero;

        } catch (NumberFormatException e) {
            throw new DescricaoAgendaInvalidaException();
        }
    }
}
