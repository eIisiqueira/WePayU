package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import br.ufal.ic.p2.wepayu.exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;

/**
 * Classe base abstrata da hierarquia de empregados do sistema.
 *
 * Mantém apenas os dados comuns a todos os empregados (identificação,
 * nome, endereço e salário), garantindo que nunca assumam valores
 * inválidos, e define as operações que cada tipo especializa por
 * polimorfismo. A associação sindical e o método de pagamento são
 * delegados a {@link MembroSindicato} e {@link MetodoPagamento}.
 */
public abstract class Empregado implements Serializable {

    private static final LocalDate INICIO_CONTRATO_PADRAO =
            LocalDate.of(2005, 1, 1);

    private final String id;
    private String nome;
    private String endereco;
    private BigDecimal salario;
    private MembroSindicato sindicato;
    private MetodoPagamento metodoPagamento;

    protected Empregado(
            String id,
            String nome,
            String endereco,
            BigDecimal salario
    ) throws EmpregadoInvalidoException, SalarioInvalidoException {

        this.id = Objects.requireNonNull(id);
        this.nome = validarNome(nome);
        this.endereco = validarEndereco(endereco);
        this.salario = validarSalario(salario);
        this.metodoPagamento = new PagamentoEmMaos();
    }

    /**
     * Cria um empregado de outro tipo a partir de um existente, preservando
     * identificação, nome, endereço, sindicato e método de pagamento.
     * Utilizado na mudança de tipo do empregado.
     *
     * @param origem empregado cuja representação está sendo substituída
     * @param salario salário do novo empregado
     * @throws SalarioInvalidoException se o salário for nulo ou negativo
     */
    protected Empregado(Empregado origem, BigDecimal salario)
            throws SalarioInvalidoException {

        this.id = origem.id;
        this.nome = origem.nome;
        this.endereco = origem.endereco;
        this.salario = validarSalario(salario);
        this.sindicato = origem.sindicato;
        this.metodoPagamento = origem.metodoPagamento;
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

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
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

    public void alterarNome(String nome)
            throws EmpregadoInvalidoException {

        this.nome = validarNome(nome);
    }

    public void alterarEndereco(String endereco)
            throws EmpregadoInvalidoException {

        this.endereco = validarEndereco(endereco);
    }

    public void alterarSalario(BigDecimal salario)
            throws SalarioInvalidoException {

        this.salario = validarSalario(salario);
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

    /**
     * Calcula os valores brutos do pagamento, específicos de cada tipo.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque ainda sem descontos
     */
    protected abstract Contracheque calcularDadosFolha(LocalDate dataPagamento);

    /**
     * Encaminha o contracheque à seção da folha correspondente ao tipo
     * do empregado, sem que a folha precise inspecionar o tipo.
     *
     * @param secoes destino que organiza os contracheques por seção
     * @param contracheque contracheque deste empregado
     */
    protected abstract void registrarEm(
            SecoesFolha secoes,
            Contracheque contracheque
    );

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
     * contrato; horistas especializam este comportamento.
     *
     * @return data de início do contrato para fins de folha
     */
    protected LocalDate dataInicioContrato() {
        return INICIO_CONTRATO_PADRAO;
    }

    public BigDecimal getComissao()
            throws EmpregadoNaoComissionadoException {

        throw new EmpregadoNaoComissionadoException();
    }

    public void alterarComissao(BigDecimal comissao)
            throws EmpregadoNaoComissionadoException,
            ComissaoInvalidaException {

        throw new EmpregadoNaoComissionadoException();
    }

    public void lancaVenda(LocalDate data, BigDecimal valor)
            throws EmpregadoNaoComissionadoException,
            ValorVendaInvalidoException {

        throw new EmpregadoNaoComissionadoException();
    }

    public BigDecimal getVendasRealizadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoComissionadoException {

        throw new EmpregadoNaoComissionadoException();
    }

    public void lancaCartao(LocalDate data, BigDecimal horas)
            throws EmpregadoNaoHoristaException,
            HorasInvalidasException {

        throw new EmpregadoNaoHoristaException();
    }

    public BigDecimal getHorasNormaisTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

    public BigDecimal getHorasExtrasTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
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

    private static String validarNome(String nome)
            throws EmpregadoInvalidoException {

        if (nome == null || nome.isEmpty()) {
            throw new EmpregadoInvalidoException("Nome nao pode ser nulo.");
        }

        return nome;
    }

    private static String validarEndereco(String endereco)
            throws EmpregadoInvalidoException {

        if (endereco == null || endereco.isEmpty()) {
            throw new EmpregadoInvalidoException(
                    "Endereco nao pode ser nulo."
            );
        }

        return endereco;
    }

    private static BigDecimal validarSalario(BigDecimal salario)
            throws SalarioInvalidoException {

        if (salario == null) {
            throw new SalarioInvalidoException("Salario nao pode ser nulo.");
        }

        if (salario.signum() < 0) {
            throw new SalarioInvalidoException(
                    "Salario deve ser nao-negativo."
            );
        }

        return salario;
    }
}
