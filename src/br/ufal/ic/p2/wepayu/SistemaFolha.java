package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNomeNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.*;

import br.ufal.ic.p2.wepayu.Exception.PersistenciaException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoEmpregadoInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.Exception.DataInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.Exception.ValorVendaInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoMembroInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.MembroNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoSindicatoDuplicadaException;
import br.ufal.ic.p2.wepayu.Exception.ValorTaxaServicoInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Coordena as operações centrais do sistema de folha de pagamento.
 *
 * Mantém e localiza os empregados, controla a geração de identificadores
 * e a persistência dos dados, além de delegar comportamentos específicos
 * aos objetos de domínio correspondentes.
 */
public class SistemaFolha {

    private static final String ARQUIVO_DADOS = "wepayu.dat";

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("d/M/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Map<String, Empregado> empregados;
    private int proximoId;

    public SistemaFolha() {
        this.empregados = new LinkedHashMap<>();
        this.proximoId = 1;

        carregar();
    }

    public String gerarId() {
        return String.valueOf(proximoId++);
    }

    public void adicionarEmpregado(Empregado empregado) {
        empregados.put(empregado.getId(), empregado);
    }

    public Empregado buscarEmpregado(String id) {
        return empregados.get(id);
    }

    /**
     * Remove do conjunto de empregados o empregado correspondente ao ID informado.
     *
     * @param id identificador do empregado
     * @throws EmpregadoNaoExisteException se nenhum empregado possuir o ID informado
     */
    public void removerEmpregado(String id)
            throws EmpregadoNaoExisteException {

        if (!empregados.containsKey(id)) {
            throw new EmpregadoNaoExisteException();
        }

        empregados.remove(id);

    }

    public void alteraEmpregadoSindicalizado(
            String emp,
            boolean sindicalizado,
            String idSindicato,
            String taxaSindical
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            IdentificacaoSindicatoDuplicadaException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        if (!sindicalizado) {
            empregado.dessindicalizar();
            return;
        }

        BigDecimal valorTaxaSindical = new BigDecimal(
                taxaSindical.replace(",", ".")
        );

        for (Empregado outro : empregados.values()) {
            if (outro != empregado
                    && outro.isSindicalizado()
                    && idSindicato.equals(outro.getIdSindicato())) {
                throw new IdentificacaoSindicatoDuplicadaException();
            }
        }

        empregado.sindicalizar(
                idSindicato,
                valorTaxaSindical
        );
    }

    public void lancaTaxaServico(
            String membro,
            String data,
            String valor
    ) throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException,
            DataInvalidaException,
            ValorTaxaServicoInvalidoException {

        Empregado empregado = buscarMembroSindicatoObrigatorio(membro);

        LocalDate dataTaxa =
                converterData(data, "Data invalida.");

        BigDecimal valorTaxa =
                converterValorTaxaServico(valor);

        empregado.lancaTaxaServico(
                dataTaxa,
                valorTaxa
        );
    }

    public BigDecimal getTaxasServico(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoSindicalizadoException,
            DataInvalidaException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        if (!empregado.isSindicalizado()) {
            throw new EmpregadoNaoSindicalizadoException();
        }

        LocalDate inicio = converterData(
                dataInicial,
                "Data inicial invalida."
        );

        LocalDate fim = converterData(
                dataFinal,
                "Data final invalida."
        );

        validarIntervalo(inicio, fim);

        return empregado.getTaxasServico(
                inicio,
                fim
        );
    }


    /**
     * Localiza o empregado, converte os dados da venda e delega
     * o lançamento ao próprio empregado.
     *
     * @param emp identificador do empregado
     * @param data data da venda
     * @param valor valor da venda
     */
    public void lancaVenda(
            String emp,
            String data,
            String valor
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException,
            ValorVendaInvalidoException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        LocalDate dataVenda =
                converterData(data, "Data invalida.");

        BigDecimal valorVenda =
                converterValorVenda(valor);

        empregado.lancaVenda(
                dataVenda,
                valorVenda
        );
    }

    /**
     * Valida o período informado e delega ao empregado a obtenção
     * do valor total das vendas realizadas.
     *
     * @param emp identificador do empregado
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @return total das vendas realizadas no período
     */
    public BigDecimal getVendasRealizadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException {

        Empregado empregado =
                buscarEmpregadoObrigatorio(emp);

        LocalDate inicio =
                converterData(
                        dataInicial,
                        "Data inicial invalida."
                );

        LocalDate fim =
                converterData(
                        dataFinal,
                        "Data final invalida."
                );

        validarIntervalo(inicio, fim);

        return empregado.getVendasRealizadas(
                inicio,
                fim
        );
    }

    /**
     * Valida e converte os dados externos de um cartão de ponto e delega
     * seu lançamento ao empregado correspondente.
     *
     * @param emp identificador do empregado
     * @param data data do cartão no formato aceito pelo sistema
     * @param horas quantidade de horas trabalhadas
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não aceitar cartões de ponto
     * @throws DataInvalidaException se a data não puder ser interpretada
     * @throws HorasInvalidasException se a quantidade de horas não for válida ou positiva
     */
    public void lancaCartao(
            String emp,
            String data,
            String horas
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException,
            HorasInvalidasException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        LocalDate dataCartao =
                converterData(data, "Data invalida.");

        BigDecimal quantidadeHoras =
                converterHoras(horas);

        empregado.lancaCartao(
                dataCartao,
                quantidadeHoras
        );
    }

    /**
     * Obtém o total de horas normais trabalhadas pelo empregado
     * no intervalo informado.
     *
     * As datas recebidas são convertidas e o intervalo é validado antes
     * de o cálculo ser delegado ao empregado.
     *
     * @param emp identificador do empregado
     * @param dataInicial início do intervalo
     * @param dataFinal fim do intervalo
     * @return total de horas normais trabalhadas
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não for horista
     * @throws DataInvalidaException se alguma data ou o intervalo forem inválidos
     */
    public BigDecimal getHorasNormaisTrabalhadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        Empregado empregado =
                buscarEmpregadoObrigatorio(emp);

        LocalDate inicio =
                converterData(
                        dataInicial,
                        "Data inicial invalida."
                );

        LocalDate fim =
                converterData(
                        dataFinal,
                        "Data final invalida."
                );

        validarIntervalo(inicio, fim);

        return empregado.getHorasNormaisTrabalhadas(
                inicio,
                fim
        );
    }

    /**
     * Obtém o total de horas extras trabalhadas pelo empregado
     * no intervalo informado.
     *
     * As datas recebidas são convertidas e o intervalo é validado antes
     * de o cálculo ser delegado ao empregado.
     *
     * @param emp identificador do empregado
     * @param dataInicial início do intervalo
     * @param dataFinal fim do intervalo
     * @return total de horas extras trabalhadas
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não for horista
     * @throws DataInvalidaException se alguma data ou o intervalo forem inválidos
     */
    public BigDecimal getHorasExtrasTrabalhadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        Empregado empregado =
                buscarEmpregadoObrigatorio(emp);

        LocalDate inicio =
                converterData(
                        dataInicial,
                        "Data inicial invalida."
                );

        LocalDate fim =
                converterData(
                        dataFinal,
                        "Data final invalida."
                );

        validarIntervalo(inicio, fim);

        return empregado.getHorasExtrasTrabalhadas(
                inicio,
                fim
        );
    }

    private Empregado buscarMembroSindicatoObrigatorio(String membro)
            throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException {

        if (membro == null || membro.isEmpty()) {
            throw new IdentificacaoMembroInvalidaException();
        }

        for (Empregado empregado : empregados.values()) {
            if (empregado.isSindicalizado()
                    && membro.equals(empregado.getIdSindicato())) {
                return empregado;
            }
        }

        throw new MembroNaoExisteException();
    }

    private Empregado buscarEmpregadoObrigatorio(String id)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        if (id == null || id.isEmpty()) {
            throw new IdentificacaoEmpregadoInvalidaException();
        }

        Empregado empregado = buscarEmpregado(id);

        if (empregado == null) {
            throw new EmpregadoNaoExisteException();
        }

        return empregado;
    }

    private LocalDate converterData(
            String data,
            String mensagem
    ) throws DataInvalidaException {

        try {
            return LocalDate.parse(
                    data,
                    FORMATO_DATA
            );
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DataInvalidaException(mensagem);
        }
    }

    private BigDecimal converterHoras(String horas)
            throws HorasInvalidasException {

        BigDecimal valor;

        try {
            valor = new BigDecimal(
                    horas.replace(",", ".")
            );
        } catch (NumberFormatException | NullPointerException e) {
            throw new HorasInvalidasException();
        }

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HorasInvalidasException();
        }

        return valor;
    }

    private BigDecimal converterValorVenda(String valor)
            throws ValorVendaInvalidoException {

        BigDecimal valorConvertido;

        try {
            valorConvertido = new BigDecimal(
                    valor.replace(",", ".")
            );
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValorVendaInvalidoException();
        }

        if (valorConvertido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorVendaInvalidoException();
        }

        return valorConvertido;
    }

    private BigDecimal converterValorTaxaServico(String valor)
            throws ValorTaxaServicoInvalidoException {

        BigDecimal valorConvertido;

        try {
            valorConvertido = new BigDecimal(
                    valor.replace(",", ".")
            );
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValorTaxaServicoInvalidoException();
        }

        if (valorConvertido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorTaxaServicoInvalidoException();
        }

        return valorConvertido;
    }

    private void validarIntervalo(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws DataInvalidaException {

        if (dataInicial.isAfter(dataFinal)) {
            throw new DataInvalidaException(
                    "Data inicial nao pode ser posterior aa data final."
            );
        }

    }

    /**
     * Persiste em arquivo o estado atual dos empregados e o próximo
     * identificador que deverá ser gerado.
     *
     * @throws PersistenciaException se ocorrer uma falha durante a gravação dos dados
     */
    public void salvar() {

        try (ObjectOutputStream saida =
                     new ObjectOutputStream(
                             new FileOutputStream(ARQUIVO_DADOS))) {

            saida.writeObject(empregados);
            saida.writeInt(proximoId);

        } catch (IOException e) {
            throw new PersistenciaException("Erro ao salvar dados.");
        }
    }

    @SuppressWarnings("unchecked")
    private void carregar() {

        File arquivo = new File(ARQUIVO_DADOS);

        if (!arquivo.exists()) {
            return;
        }

        try (ObjectInputStream entrada =
                     new ObjectInputStream(
                             new FileInputStream(arquivo))) {

            // A leitura deve seguir a mesma ordem usada em salvar(): empregados e depois próximo ID.
            Map<String, Empregado> dados =
                    (Map<String, Empregado>) entrada.readObject();

            empregados.putAll(dados);

            proximoId = entrada.readInt();

        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException(
                    "Erro ao carregar dados."
            );
        }
    }


    /**
     * Reinicia o estado do sistema, removendo os empregados em memória,
     * restaurando a sequência de identificadores e excluindo o arquivo
     * de persistência existente.
     */
    public void zerar() {
        empregados.clear();
        proximoId = 1;

        File arquivo = new File(ARQUIVO_DADOS);

        if (arquivo.exists()) {
            arquivo.delete();
        }
    }

    /**
     * Procura empregados cujo nome contenha o texto informado e retorna
     * o ID da correspondência indicada.
     *
     * As correspondências seguem a ordem mantida pelo cadastro de empregados,
     * e o índice é contado a partir de 1.
     *
     * @param nome trecho de nome utilizado na busca
     * @param indice posição da correspondência desejada
     * @return identificador do empregado encontrado
     * @throws EmpregadoNomeNaoExisteException se não houver correspondência
     *                                         na posição solicitada
     */
    public String buscarEmpregadoPorNome(String nome, int indice)
            throws EmpregadoNomeNaoExisteException {

        int contador = 0;

        for (Empregado empregado : empregados.values()) {

            if (empregado.getNome().contains(nome)) {
                contador++;

                if (contador == indice) {
                    return empregado.getId();
                }
            }
        }

        throw new EmpregadoNomeNaoExisteException();
    }

    public void alterarNome(String emp, String nome)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);
        empregado.setNome(nome);
    }

    public void alterarEndereco(String emp, String endereco)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);
        empregado.setEndereco(endereco);
    }

    public void alterarSalario(String emp, BigDecimal salario)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);
        empregado.setSalario(salario);
    }

    public void alterarComissao(String emp, BigDecimal comissao)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);
        empregado.alterarComissao(comissao);
    }

}