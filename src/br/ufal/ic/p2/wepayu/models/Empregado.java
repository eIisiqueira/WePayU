package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import br.ufal.ic.p2.wepayu.exception.AtributoNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoRecebeEmBancoException;

/**
 * Classe base abstrata da hierarquia de empregados do sistema.
 *
 * Concentra os dados comuns aos diferentes tipos de empregado e define
 * operações especializadas polimorficamente pelas subclasses. A associação
 * sindical e o método de pagamento são delegados a {@link MembroSindicato}
 * e {@link MetodoPagamento}.
 */

public abstract class Empregado implements Serializable {

    private static final LocalDate INICIO_CONTRATO_PADRAO =
            LocalDate.of(2005, 1, 1);

    private String id;
    private String nome;
    private String endereco;
    private BigDecimal salario;
    private MembroSindicato sindicato;
    private MetodoPagamento metodoPagamento;

    public Empregado(String id, String nome, String endereco, BigDecimal salario) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.salario = salario;
        this.metodoPagamento = new PagamentoEmMaos();
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
        return sindicato != null;
    }

    public Optional<MembroSindicato> getSindicato() {
        return Optional.ofNullable(sindicato);
    }

    public MembroSindicato getMembroSindicato()
            throws EmpregadoNaoSindicalizadoException {

        return getSindicato()
                .orElseThrow(EmpregadoNaoSindicalizadoException::new);
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
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

    public void alterarMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = Objects.requireNonNull(metodoPagamento);
    }

    public void sindicalizar(MembroSindicato sindicato) {
        this.sindicato = Objects.requireNonNull(sindicato);
    }

    public void dessindicalizar() {
        this.sindicato = null;
    }

    /**
     * Copia para outro empregado o estado comum que deve ser preservado
     * durante uma mudança de tipo: filiação sindical e método de pagamento.
     *
     * @param destino empregado que receberá o estado comum
     */
    public void copiarEstadoComumPara(Empregado destino) {
        destino.sindicato = this.sindicato;
        destino.metodoPagamento = this.metodoPagamento;
    }

    public abstract String getTipo();

    /**
     * Informa se o empregado deve receber pagamento na data informada,
     * de acordo com a agenda definida por seu tipo.
     *
     * @param dataPagamento data a ser verificada
     * @return {@code true} se o empregado pertence à folha dessa data
     */
    public abstract boolean deveReceberEm(LocalDate dataPagamento);

    /**
     * Determina a data inicial do período considerado no pagamento.
     *
     * @param dataPagamento data da folha
     * @return início do período de pagamento
     */
    public abstract LocalDate inicioPeriodoPagamento(LocalDate dataPagamento);

    protected abstract Contracheque calcularDadosFolha(LocalDate dataPagamento);

    public abstract String getSecaoFolha();

    public abstract String formatarLinhaFolha(Contracheque contracheque);

    /**
     * Calcula o contracheque do empregado para a data informada,
     * combinando o cálculo específico do tipo com os descontos sindicais.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque calculado
     */
    public Contracheque calcularContracheque(LocalDate dataPagamento) {
        Contracheque dados = calcularDadosFolha(dataPagamento);
        BigDecimal bruto = dados.getSalarioBruto();

        BigDecimal descontos = sindicato == null
                ? BigDecimal.ZERO
                : sindicato.calcularDescontos(this, dataPagamento, bruto);

        return dados.comDeducoes(descontos, bruto.subtract(descontos));
    }

    /**
     * Informa a data a partir da qual as deduções sindicais podem ser
     * acumuladas. Assalariados e comissionados usam a data padrão do
     * contrato; horistas especializam este comportamento com base no
     * primeiro cartão de ponto.
     *
     * @return data de início do contrato para fins de folha
     */
    protected LocalDate dataInicioContrato() {
        return INICIO_CONTRATO_PADRAO;
    }

    /**
     * Indica se uma data pertence ao intervalo, com início inclusivo
     * e fim exclusivo, regra usada pelos lançamentos de todos os tipos.
     */
    protected static boolean estaNoIntervalo(
            LocalDate data,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        return !data.isBefore(dataInicial) && data.isBefore(dataFinal);
    }

    /**
     * Produz a descrição textual do método de pagamento configurado
     * para o empregado.
     *
     * @return descrição utilizada no relatório da folha
     */
    public String descricaoPagamento() {
        return metodoPagamento.descricao(endereco);
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
                return metodoPagamento.getTipo();

            case "banco":
                return metodoPagamento.getBanco();

            case "agencia":
                return metodoPagamento.getAgencia();

            case "contaCorrente":
                return metodoPagamento.getContaCorrente();

            case "sindicalizado":
                return String.valueOf(isSindicalizado());

            case "idSindicato":
                return getMembroSindicato().getIdentificacao();

            case "taxaSindical":
                return formatarValor(getMembroSindicato().getTaxaSindical());

            default:
                throw new AtributoNaoExisteException();
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
