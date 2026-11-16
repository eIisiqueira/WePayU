package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.exception.IdentificacaoSindicatoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.TaxaSindicalInvalidaException;

/**
 * Associação de um empregado ao sindicato: identificação, taxa sindical
 * diária, taxas de serviço lançadas e cálculo das deduções na folha.
 */
public class MembroSindicato implements Serializable {

    private final String identificacao;
    private final BigDecimal taxaSindical;
    private final List<TaxaServico> taxasServico = new ArrayList<>();

    public MembroSindicato(String identificacao, BigDecimal taxaSindical)
            throws IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException {

        if (identificacao == null || identificacao.isEmpty()) {
            throw new IdentificacaoSindicatoInvalidaException();
        }

        if (taxaSindical == null) {
            throw new TaxaSindicalInvalidaException(
                    "Taxa sindical nao pode ser nula."
            );
        }

        if (taxaSindical.signum() < 0) {
            throw new TaxaSindicalInvalidaException(
                    "Taxa sindical deve ser nao-negativa."
            );
        }

        this.identificacao = identificacao;
        this.taxaSindical = taxaSindical;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public BigDecimal getTaxaSindical() {
        return taxaSindical;
    }

    public boolean possuiIdentificacao(String identificacao) {
        return this.identificacao.equals(identificacao);
    }

    public void lancaTaxaServico(LocalDate data, BigDecimal valor) {

        taxasServico.add(new TaxaServico(data, valor));
    }

    /**
     * Soma as taxas de serviço do período (início inclusivo, fim exclusivo).
     */
    public BigDecimal getTaxasServico(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (TaxaServico taxa : taxasServico) {
            if (Empregado.estaNoIntervalo(
                    taxa.getData(), dataInicial, dataFinal)) {

                total = total.add(taxa.getValor());
            }
        }

        return total;
    }

    /**
     * Calcula o desconto sindical aplicado ao empregado na data informada.
     *
     * Reconstitui as deduções de cada pagamento desde o início do contrato,
     * de modo que o saldo que não pôde ser descontado (por não haver
     * salário suficiente) seja levado para os pagamentos seguintes.
     *
     * @param empregado empregado associado a esta filiação
     * @param dataPagamento data da folha
     * @param salarioBrutoAtual salário bruto calculado para essa data
     * @return valor efetivamente descontado nessa data
     */
    BigDecimal calcularDescontos(
            Empregado empregado,
            LocalDate dataPagamento,
            BigDecimal salarioBrutoAtual
    ) {
        LocalDate inicioContrato = empregado.dataInicioContrato();

        if (inicioContrato == null || dataPagamento.isBefore(inicioContrato)) {
            return BigDecimal.ZERO;
        }

        BigDecimal saldoPendente = BigDecimal.ZERO;
        LocalDate inicioPeriodo = inicioContrato;

        for (LocalDate data = inicioContrato;
             !data.isAfter(dataPagamento);
             data = data.plusDays(1)) {

            if (!empregado.deveReceberEm(data)) {
                continue;
            }

            long dias = ChronoUnit.DAYS.between(inicioPeriodo, data) + 1;

            saldoPendente = saldoPendente
                    .add(taxaSindical.multiply(BigDecimal.valueOf(dias)))
                    .add(getTaxasServico(inicioPeriodo, data.plusDays(1)));

            BigDecimal salarioBruto = data.equals(dataPagamento)
                    ? salarioBrutoAtual
                    : empregado.calcularDadosFolha(data).getSalarioBruto();

            BigDecimal descontoAplicado = saldoPendente.min(salarioBruto);

            if (data.equals(dataPagamento)) {
                return descontoAplicado;
            }

            saldoPendente = saldoPendente.subtract(descontoAplicado);
            inicioPeriodo = data.plusDays(1);
        }

        return BigDecimal.ZERO;
    }
}
