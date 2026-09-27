package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import java.math.RoundingMode;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoRecebeEmBancoException;

/**
 * Classe base abstrata da hierarquia de empregados do sistema.
 *
 * Concentra os dados e comportamentos comuns aos diferentes tipos de
 * empregado e define operações que podem ser especializadas
 * polimorficamente pelas subclasses.
 */

public abstract class Empregado implements Serializable {

    private static final LocalDate INICIO_CONTRATO_PADRAO =
            LocalDate.of(2005, 1, 1);

    private String id;
    private String nome;
    private String endereco;
    private BigDecimal salario;
    private boolean sindicalizado;
    private String idSindicato;
    private BigDecimal taxaSindical;
    private final List<TaxaServico> taxasServico = new ArrayList<>();
    private String metodoPagamento;
    private String banco;
    private String agencia;
    private String contaCorrente;

    public Empregado(String id, String nome, String endereco, BigDecimal salario) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.salario = salario;
        this.sindicalizado = false;
        this.metodoPagamento = "emMaos";
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public boolean isSindicalizado() {
        return sindicalizado;
    }

    public String getIdSindicato() {
        return idSindicato;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public void configurarPagamentoEmMaos() {
        this.metodoPagamento = "emMaos";
        this.banco = null;
        this.agencia = null;
        this.contaCorrente = null;
    }

    public void configurarPagamentoCorreios() {
        this.metodoPagamento = "correios";
        this.banco = null;
        this.agencia = null;
        this.contaCorrente = null;
    }

    public void configurarPagamentoBanco(
            String banco,
            String agencia,
            String contaCorrente
    ) {
        this.metodoPagamento = "banco";
        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    public void setSindicalizado(boolean sindicalizado) {
        this.sindicalizado = sindicalizado;
    }

    public void sindicalizar(String idSindicato, BigDecimal taxaSindical) {
        this.sindicalizado = true;
        this.idSindicato = idSindicato;
        this.taxaSindical = taxaSindical;
    }

    public void dessindicalizar() {
        this.sindicalizado = false;
        this.idSindicato = null;
        this.taxaSindical = null;
    }

    /**
     * Copia para outro empregado o estado comum que deve ser preservado
     * durante uma mudança de tipo.
     *
     * São preservadas as informações sindicais, taxas de serviço
     * e dados do método de pagamento.
     *
     * @param destino empregado que receberá o estado comum
     */
    public void copiarEstadoComumPara(Empregado destino) {
        destino.sindicalizado = this.sindicalizado;
        destino.idSindicato = this.idSindicato;
        destino.taxaSindical = this.taxaSindical;

        destino.taxasServico.addAll(this.taxasServico);

        destino.metodoPagamento = this.metodoPagamento;
        destino.banco = this.banco;
        destino.agencia = this.agencia;
        destino.contaCorrente = this.contaCorrente;
    }

    public void lancaTaxaServico(LocalDate data, BigDecimal valor) {
        taxasServico.add(new TaxaServico(data, valor));
    }

    /**
     * Soma as taxas de serviço registradas no período informado.
     * A data inicial é incluída e a data final é excluída.
     *
     * @param dataInicial início inclusivo do período
     * @param dataFinal fim exclusivo do período
     * @return total das taxas de serviço
     */
    public BigDecimal getTaxasServico(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (TaxaServico taxa : taxasServico) {
            LocalDate data = taxa.getData();

            if (!data.isBefore(dataInicial) && data.isBefore(dataFinal)) {
                total = total.add(taxa.getValor());
            }
        }

        return total;
    }

    public abstract String getTipo();

    public abstract boolean deveReceberEm(LocalDate dataPagamento);

    public abstract LocalDate inicioPeriodoPagamento(LocalDate dataPagamento);

    protected abstract Contracheque calcularDadosFolha(LocalDate dataPagamento);

    public abstract String getSecaoFolha();

    public abstract String formatarLinhaFolha(Contracheque contracheque);

    /**
     * Calcula o contracheque do empregado para a data informada,
     * combinando o cálculo específico do tipo com os descontos comuns.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque calculado
     */
    public Contracheque calcularContracheque(LocalDate dataPagamento) {
        Contracheque dados = calcularDadosFolha(dataPagamento);

        if (dados.getSalarioBruto().compareTo(BigDecimal.ZERO) == 0) {
            return dados.comDeducoes(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal descontos = BigDecimal.ZERO;

        if (sindicalizado) {
            int diasTaxaSindical = calcularDiasTaxaSindical(dataPagamento);

            descontos = descontos.add(
                    taxaSindical.multiply(
                            BigDecimal.valueOf(diasTaxaSindical)
                    )
            );

            descontos = descontos.add(
                    getTaxasServico(
                            inicioPeriodoPagamento(dataPagamento),
                            dataPagamento.plusDays(1)
                    )
            );
        }

        BigDecimal salarioLiquido =
                dados.getSalarioBruto().subtract(descontos);

        return dados.comDeducoes(
                descontos,
                salarioLiquido
        );
    }

    private int calcularDiasTaxaSindical(LocalDate dataPagamento) {
        LocalDate dataAnterior = dataPagamento.minusDays(1);

        // Retrocede até a última folha com salário bruto positivo para
        // acumular os dias de taxa sindical desde esse pagamento.
        while (!dataAnterior.isBefore(INICIO_CONTRATO_PADRAO)) {
            if (deveReceberEm(dataAnterior)) {
                Contracheque dadosAnteriores =
                        calcularDadosFolha(dataAnterior);

                if (dadosAnteriores.getSalarioBruto()
                        .compareTo(BigDecimal.ZERO) > 0) {

                    return (int) ChronoUnit.DAYS.between(
                            dataAnterior,
                            dataPagamento
                    );
                }
            }

            dataAnterior = dataAnterior.minusDays(1);
        }

        return (int) ChronoUnit.DAYS.between(
                INICIO_CONTRATO_PADRAO,
                dataPagamento
        ) + 1;
    }

    public String descricaoPagamento() {
        switch (metodoPagamento) {
            case "correios":
                return "Correios, " + endereco;

            case "banco":
                return banco
                        + ", Ag. " + agencia
                        + " CC " + contaCorrente;

            default:
                return "Em maos";
        }
    }

    public String getAtributo(String atributo)
            throws AtributoNaoExisteException,
            EmpregadoNaoComissionadoException,
            EmpregadoNaoSindicalizadoException,
            EmpregadoNaoRecebeEmBancoException {

        switch (atributo) {

            case "nome":
                return getNome();

            case "endereco":
                return getEndereco();

            case "tipo":
                return getTipo();

            case "salario":
                return formatarValor(getSalario());

            case "comissao":
                throw new EmpregadoNaoComissionadoException();

            case "metodoPagamento":
                return metodoPagamento;

            case "banco":
                validarPagamentoBanco();
                return banco;

            case "agencia":
                validarPagamentoBanco();
                return agencia;

            case "contaCorrente":
                validarPagamentoBanco();
                return contaCorrente;

            case "sindicalizado":
                return String.valueOf(isSindicalizado());

            case "idSindicato":
                validarSindicalizado();
                return idSindicato;

            case "taxaSindical":
                validarSindicalizado();
                return formatarValor(taxaSindical);

            default:
                throw new AtributoNaoExisteException();
        }
    }

    private void validarPagamentoBanco()
            throws EmpregadoNaoRecebeEmBancoException {

        if (!"banco".equals(metodoPagamento)) {
            throw new EmpregadoNaoRecebeEmBancoException();
        }
    }

    private void validarSindicalizado()
            throws EmpregadoNaoSindicalizadoException {

        if (!sindicalizado) {
            throw new EmpregadoNaoSindicalizadoException();
        }
    }

    public static String formatarValor(BigDecimal valor) {
        return valor
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace(".", ",");
    }

    public static String formatarNumero(BigDecimal valor) {
        return valor
                .stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }

    public void alterarComissao(BigDecimal comissao)
            throws EmpregadoNaoComissionadoException {

        throw new EmpregadoNaoComissionadoException();
    }

    /**
     * Define a operação polimórfica de lançamento de venda.
     * A implementação padrão rejeita a operação para empregados
     * que não são comissionados.
     *
     * @param data data da venda
     * @param valor valor da venda
     * @throws EmpregadoNaoComissionadoException se o empregado não suportar vendas
     */
    public void lancaVenda(LocalDate data, BigDecimal valor)
            throws EmpregadoNaoComissionadoException {

        throw new EmpregadoNaoComissionadoException();
    }

    /**
     * Define a consulta polimórfica das vendas realizadas em um período.
     * A implementação padrão rejeita a consulta para empregados
     * que não são comissionados.
     *
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @return total das vendas realizadas
     * @throws EmpregadoNaoComissionadoException se o empregado não suportar vendas
     */
    public BigDecimal getVendasRealizadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoComissionadoException {

        throw new EmpregadoNaoComissionadoException();
    }

    /**
     * Define a operação polimórfica de lançamento de cartão de ponto.
     *
     * Na implementação base, a operação é rejeitada. Subclasses que
     * suportam cartões de ponto devem sobrescrever este comportamento.
     *
     * @param data data do cartão de ponto
     * @param horas quantidade de horas trabalhadas
     * @throws EmpregadoNaoHoristaException quando o tipo de empregado
     *                                      não suporta cartão de ponto
     */
    public void lancaCartao(LocalDate data, BigDecimal horas)
            throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

    /**
     * Define a consulta polimórfica das horas normais trabalhadas
     * em determinado intervalo.
     *
     * Na implementação base, a consulta é rejeitada. Subclasses que
     * controlam horas trabalhadas devem sobrescrever este comportamento.
     *
     * @param dataInicial início do intervalo
     * @param dataFinal fim do intervalo
     * @return total de horas normais trabalhadas
     * @throws EmpregadoNaoHoristaException quando o tipo de empregado
     *                                      não suporta essa consulta
     */
    public BigDecimal getHorasNormaisTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

    /**
     * Define a consulta polimórfica das horas extras trabalhadas
     * em determinado intervalo.
     *
     * Na implementação base, a consulta é rejeitada. Subclasses que
     * controlam horas trabalhadas devem sobrescrever este comportamento.
     *
     * @param dataInicial início do intervalo
     * @param dataFinal fim do intervalo
     * @return total de horas extras trabalhadas
     * @throws EmpregadoNaoHoristaException quando o tipo de empregado
     *                                      não suporta essa consulta
     */
    public BigDecimal getHorasExtrasTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

}
