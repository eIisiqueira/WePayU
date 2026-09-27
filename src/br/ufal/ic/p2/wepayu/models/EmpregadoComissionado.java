package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoRecebeEmBancoException;

/**
 * Representa um empregado comissionado, com salário base,
 * taxa de comissão e resultados de vendas associados.
 *
 * Sua remuneração combina a parcela fixa com a comissão
 * sobre as vendas do período.
 */

public class EmpregadoComissionado extends Empregado {

    private static final LocalDate PRIMEIRA_DATA_PAGAMENTO =
            LocalDate.of(2005, 1, 14);

    private BigDecimal comissao;
    private final List<ResultadoVenda> vendas = new ArrayList<>();

    public EmpregadoComissionado(
            String id,
            String nome,
            String endereco,
            BigDecimal salario,
            BigDecimal comissao) {

        super(id, nome, endereco, salario);
        this.comissao = comissao;
    }

    public BigDecimal getValorComissao() {
        return comissao;
    }

    @Override
    public void alterarComissao(BigDecimal comissao) {
        this.comissao = comissao;
    }

    @Override
    public String getTipo() {
        return "comissionado";
    }

    @Override
    public boolean deveReceberEm(LocalDate dataPagamento) {
        if (dataPagamento.isBefore(PRIMEIRA_DATA_PAGAMENTO)) {
            return false;
        }

        long diasDesdePrimeiroPagamento =
                ChronoUnit.DAYS.between(
                        PRIMEIRA_DATA_PAGAMENTO,
                        dataPagamento
                );

        return diasDesdePrimeiroPagamento % 14 == 0;
    }

    @Override
    public LocalDate inicioPeriodoPagamento(LocalDate dataPagamento) {
        return dataPagamento.minusDays(13);
    }

    /**
     * Calcula os dados brutos da folha do empregado comissionado,
     * combinando a parcela fixa com a comissão sobre as vendas do período.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque com os dados brutos do pagamento
     */
    @Override
    protected Contracheque calcularDadosFolha(LocalDate dataPagamento) {
        LocalDate inicio = inicioPeriodoPagamento(dataPagamento);
        LocalDate fimExclusivo = dataPagamento.plusDays(1);

        BigDecimal fixo = getSalario()
                .multiply(new BigDecimal("12"))
                .divide(new BigDecimal("26"), 2, RoundingMode.DOWN);

        BigDecimal totalVendas =
                getVendasRealizadas(inicio, fimExclusivo);

        BigDecimal valorComissao = totalVendas
                .multiply(comissao)
                .setScale(2, RoundingMode.DOWN);

        BigDecimal salarioBruto =
                fixo.add(valorComissao);

        return new Contracheque(
                this,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                fixo,
                totalVendas,
                valorComissao,
                salarioBruto,
                BigDecimal.ZERO,
                salarioBruto
        );
    }

    @Override
    public String getSecaoFolha() {
        return "COMISSIONADOS";
    }

    @Override
    public String formatarLinhaFolha(Contracheque contracheque) {
        return String.format(
                "%-21s %8s %8s %8s %13s %9s %15s %s",
                getNome(),
                formatarValor(contracheque.getFixo()),
                formatarValor(contracheque.getVendas()),
                formatarValor(contracheque.getComissao()),
                formatarValor(contracheque.getSalarioBruto()),
                formatarValor(contracheque.getDescontos()),
                formatarValor(contracheque.getSalarioLiquido()),
                descricaoPagamento()
        );
    }

    @Override
    public String getAtributo(String atributo)
            throws AtributoNaoExisteException,
            EmpregadoNaoComissionadoException,
            EmpregadoNaoSindicalizadoException,
            EmpregadoNaoRecebeEmBancoException {

        if (atributo.equals("comissao")) {
            return formatarValor(comissao);
        }
        return super.getAtributo(atributo);
    }
    @Override
    public void lancaVenda(LocalDate data, BigDecimal valor) {
        vendas.add(new ResultadoVenda(data, valor));
    }

    /**
     * Soma os valores das vendas realizadas no período informado.
     * A data inicial é incluída e a data final é excluída.
     *
     * @param dataInicial início inclusivo do período
     * @param dataFinal fim exclusivo do período
     * @return total vendido no período
     */
    @Override
    public BigDecimal getVendasRealizadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (ResultadoVenda venda : vendas) {
            if (estaNoIntervalo(
                    venda.getData(),
                    dataInicial,
                    dataFinal
            )) {
                total = total.add(venda.getValor());
            }
        }

        return total;
    }

    private boolean estaNoIntervalo(
            LocalDate data,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        return !data.isBefore(dataInicial)
                && data.isBefore(dataFinal);
    }

}