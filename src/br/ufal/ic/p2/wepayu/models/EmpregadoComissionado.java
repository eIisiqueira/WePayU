package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;

/**
 * Representa um empregado comissionado, com salário base,
 * taxa de comissão e resultados de vendas associados.
 *
 * Sua remuneração combina a parcela fixa com a comissão
 * sobre as vendas do período. A taxa de comissão nunca pode
 * ser nula ou negativa.
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
            BigDecimal comissao
    ) throws EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        super(id, nome, endereco, salario);
        this.comissao = validarComissao(comissao);
    }

    /**
     * Converte outro empregado em comissionado, mantendo o salário atual.
     */
    public EmpregadoComissionado(Empregado origem, BigDecimal comissao)
            throws SalarioInvalidoException, ComissaoInvalidaException {

        super(origem, origem.getSalario());
        this.comissao = validarComissao(comissao);
    }

    @Override
    public BigDecimal getComissao() {
        return comissao;
    }

    @Override
    public void alterarComissao(BigDecimal comissao)
            throws ComissaoInvalidaException {

        this.comissao = validarComissao(comissao);
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

        long dias = ChronoUnit.DAYS.between(
                PRIMEIRA_DATA_PAGAMENTO,
                dataPagamento
        );

        return dias % 14 == 0;
    }

    @Override
    public LocalDate inicioPeriodoPagamento(LocalDate dataPagamento) {
        return dataPagamento.minusDays(13);
    }

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

        BigDecimal salarioBruto = fixo.add(valorComissao);

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
    protected void registrarEm(
            SecoesFolha secoes,
            Contracheque contracheque
    ) {
        secoes.adicionarComissionado(contracheque);
    }

    @Override
    public void lancaVenda(LocalDate data, BigDecimal valor)
            throws ValorVendaInvalidoException {

        vendas.add(new ResultadoVenda(data, valor));
    }

    /**
     * Soma as vendas do período (início inclusivo, fim exclusivo).
     */
    @Override
    public BigDecimal getVendasRealizadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (ResultadoVenda venda : vendas) {
            if (estaNoIntervalo(venda.getData(), dataInicial, dataFinal)) {
                total = total.add(venda.getValor());
            }
        }

        return total;
    }

    private static BigDecimal validarComissao(BigDecimal comissao)
            throws ComissaoInvalidaException {

        if (comissao == null) {
            throw new ComissaoInvalidaException("Comissao nao pode ser nula.");
        }

        if (comissao.signum() < 0) {
            throw new ComissaoInvalidaException(
                    "Comissao deve ser nao-negativa."
            );
        }

        return comissao;
    }
}
