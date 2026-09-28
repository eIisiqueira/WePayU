package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.EmpregadoAssalariado;
import br.ufal.ic.p2.wepayu.models.EmpregadoHorista;
import br.ufal.ic.p2.wepayu.models.EmpregadoComissionado;
import br.ufal.ic.p2.wepayu.models.Contracheque;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNomeNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
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
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoSindicatoInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.TaxaSindicalInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.MetodoPagamentoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.BancoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.AgenciaInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.ContaCorrenteInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.NaoHaComandoADesfazerException;
import br.ufal.ic.p2.wepayu.Exception.NaoHaComandoARefazerException;


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

    private static final String QUEBRA_LINHA = "\n";

    private static final String SEPARADOR = "=".repeat(127);

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("d/M/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Map<String, Empregado> empregados;
    private int proximoId;

    private final Deque<byte[]> pilhaUndo;
    private final Deque<byte[]> pilhaRedo;

    public SistemaFolha() {
        this.empregados = new LinkedHashMap<>();
        this.proximoId = 1;

        this.pilhaUndo = new ArrayDeque<>();
        this.pilhaRedo = new ArrayDeque<>();

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
     * Retorna a quantidade de empregados mantidos pelo sistema.
     *
     * @return número de empregados cadastrados
     */
    public int getNumeroDeEmpregados() {
        return empregados.size();
    }

    /**
     * Cria uma cópia profunda do estado de negócio atual do sistema.
     *
     * O snapshot contém os empregados cadastrados e o próximo identificador
     * disponível, sem utilizar o arquivo de persistência do sistema.
     *
     * @return representação serializada do estado atual
     * @throws PersistenciaException se não for possível criar o snapshot
     */
    private byte[] criarSnapshot() {

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream saida = new ObjectOutputStream(bytes)) {

            saida.writeObject(empregados);
            saida.writeInt(proximoId);
            saida.flush();

            return bytes.toByteArray();

        } catch (IOException e) {
            throw new PersistenciaException(
                    "Erro ao criar snapshot."
            );
        }
    }

    /**
     * Restaura o estado de negócio armazenado em um snapshot.
     *
     * Os empregados atuais são substituídos pelos empregados presentes
     * no snapshot e o próximo identificador também é restaurado.
     *
     * @param snapshot estado anteriormente capturado
     * @throws PersistenciaException se não for possível restaurar o snapshot
     */
    @SuppressWarnings("unchecked")
    private void restaurarSnapshot(byte[] snapshot) {

        try (ByteArrayInputStream bytes =
                     new ByteArrayInputStream(snapshot);
             ObjectInputStream entrada =
                     new ObjectInputStream(bytes)) {

            Map<String, Empregado> empregadosRestaurados =
                    (Map<String, Empregado>) entrada.readObject();

            int proximoIdRestaurado = entrada.readInt();

            empregados.clear();
            empregados.putAll(empregadosRestaurados);

            proximoId = proximoIdRestaurado;

        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException(
                    "Erro ao restaurar snapshot."
            );
        }
    }

    /**
     * Captura o estado atual para uma possível transação.
     *
     * @return snapshot profundo do estado de negócio atual
     */
    byte[] capturarEstadoParaTransacao() {
        return criarSnapshot();
    }

    /**
     * Registra uma transação concluída com sucesso.
     *
     * O estado anterior é armazenado na pilha de undo e qualquer
     * histórico de redo é descartado.
     *
     * @param estadoAnterior estado existente antes da transação
     */
    void confirmarTransacao(byte[] estadoAnterior) {
        pilhaUndo.push(estadoAnterior);
        pilhaRedo.clear();
    }

    /**
     * Desfaz a última transação registrada no sistema.
     *
     * O estado atual é armazenado para permitir um possível redo e,
     * em seguida, o estado anterior é restaurado.
     *
     * @throws NaoHaComandoADesfazerException se não existir transação
     *                                        disponível para desfazer
     */
    public void undo() throws NaoHaComandoADesfazerException {

        if (pilhaUndo.isEmpty()) {
            throw new NaoHaComandoADesfazerException();
        }

        byte[] estadoAtual = criarSnapshot();
        byte[] estadoAnterior = pilhaUndo.peek();

        restaurarSnapshot(estadoAnterior);

        pilhaUndo.pop();
        pilhaRedo.push(estadoAtual);
    }

    /**
     * Refaz a última transação anteriormente desfeita.
     *
     * O estado atual é armazenado na pilha de undo e o estado
     * futuro correspondente é restaurado.
     *
     * @throws NaoHaComandoARefazerException se não existir transação
     *                                       disponível para refazer
     */
    public void redo() throws NaoHaComandoARefazerException {

        if (pilhaRedo.isEmpty()) {
            throw new NaoHaComandoARefazerException();
        }

        byte[] estadoAtual = criarSnapshot();
        byte[] estadoFuturo = pilhaRedo.peek();

        restaurarSnapshot(estadoFuturo);

        pilhaRedo.pop();
        pilhaUndo.push(estadoAtual);
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

    /**
     * Altera a situação sindical do empregado.
     *
     * Ao sindicalizar, valida a identificação sindical e a taxa informada,
     * impedindo também o uso de uma identificação já associada a outro
     * empregado. Ao dessindicalizar, remove os dados sindicais atuais.
     *
     * @param emp identificador do empregado
     * @param sindicalizado nova situação sindical
     * @param idSindicato identificação do empregado no sindicato
     * @param taxaSindical taxa sindical
     */
    public void alteraEmpregadoSindicalizado(
            String emp,
            boolean sindicalizado,
            String idSindicato,
            String taxaSindical
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            IdentificacaoSindicatoDuplicadaException,
            IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        if (!sindicalizado) {
            empregado.dessindicalizar();
            return;
        }

        if (idSindicato == null || idSindicato.isEmpty()) {
            throw new IdentificacaoSindicatoInvalidaException();
        }

        if (taxaSindical == null || taxaSindical.isEmpty()) {
            throw new TaxaSindicalInvalidaException(
                    "Taxa sindical nao pode ser nula."
            );
        }

        BigDecimal valorTaxaSindical;

        try {
            valorTaxaSindical = new BigDecimal(
                    taxaSindical.replace(",", ".")
            );
        } catch (NumberFormatException e) {
            throw new TaxaSindicalInvalidaException(
                    "Taxa sindical deve ser numerica."
            );
        }

        if (valorTaxaSindical.compareTo(BigDecimal.ZERO) < 0) {
            throw new TaxaSindicalInvalidaException(
                    "Taxa sindical deve ser nao-negativa."
            );
        }

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

    /**
     * Localiza o empregado pela identificação sindical, converte os dados
     * recebidos e delega o registro da taxa de serviço ao empregado.
     *
     * @param membro identificação do empregado no sindicato
     * @param data data da taxa de serviço
     * @param valor valor da taxa de serviço
     */
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

    /**
     * Consulta o total das taxas de serviço do empregado sindicalizado
     * no período informado.
     *
     * @param emp identificador do empregado
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @return soma das taxas de serviço no período
     */
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

    /**
     * Calcula o total bruto da folha na data informada,
     * somando os contracheques dos empregados que devem receber.
     *
     * @param data data da folha
     * @return total bruto da folha
     * @throws DataInvalidaException se a data informada for inválida
     */
    public BigDecimal totalFolha(String data)
            throws DataInvalidaException {

        LocalDate dataFolha = converterData(
                data,
                "Data invalida."
        );

        BigDecimal total = BigDecimal.ZERO;

        for (Contracheque contracheque :
                calcularContracheques(dataFolha)) {

            total = total.add(
                    contracheque.getSalarioBruto()
            );
        }

        return total;
    }

    /**
     * Calcula os contracheques da data informada, monta o relatório
     * da folha e grava o resultado no arquivo indicado.
     *
     * @param data data da folha
     * @param saida caminho do arquivo de saída
     * @throws DataInvalidaException se a data informada for inválida
     */
    public void rodaFolha(String data, String saida)
            throws DataInvalidaException {

        LocalDate dataFolha = converterData(
                data,
                "Data invalida."
        );

        List<Contracheque> contracheques =
                calcularContracheques(dataFolha);

        String conteudo = montarRelatorio(
                dataFolha,
                contracheques
        );

        try {
            Files.write(
                    Paths.get(saida),
                    conteudo.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException | RuntimeException e) {
            throw new PersistenciaException(
                    "Erro ao gerar folha."
            );
        }
    }

    private List<Contracheque> calcularContracheques(
            LocalDate dataFolha
    ) {
        List<Contracheque> contracheques =
                new ArrayList<>();

        for (Empregado empregado : empregados.values()) {
            if (empregado.deveReceberEm(dataFolha)) {
                contracheques.add(
                        empregado.calcularContracheque(dataFolha)
                );
            }
        }

        contracheques.sort(
                Comparator.comparing(
                        contracheque ->
                                contracheque
                                        .getEmpregado()
                                        .getNome()
                )
        );

        return contracheques;
    }

    private String montarRelatorio(
            LocalDate dataFolha,
            List<Contracheque> contracheques
    ) {
        List<Contracheque> horistas =
                filtrarSecao(contracheques, "HORISTAS");

        List<Contracheque> assalariados =
                filtrarSecao(contracheques, "ASSALARIADOS");

        List<Contracheque> comissionados =
                filtrarSecao(contracheques, "COMISSIONADOS");

        StringBuilder relatorio = new StringBuilder();

        String dataFormatada = dataFolha.toString();
        String cabecalho =
                "FOLHA DE PAGAMENTO DO DIA " + dataFormatada;

        relatorio.append(cabecalho).append(QUEBRA_LINHA);
        relatorio.append("=".repeat(cabecalho.length()))
                .append(QUEBRA_LINHA);
        relatorio.append(QUEBRA_LINHA);

        relatorio.append(montarSecaoHoristas(horistas));
        relatorio.append(montarSecaoAssalariados(assalariados));
        relatorio.append(montarSecaoComissionados(comissionados));

        BigDecimal totalFolha = BigDecimal.ZERO;

        for (Contracheque contracheque : contracheques) {
            totalFolha = totalFolha.add(
                    contracheque.getSalarioBruto()
            );
        }

        relatorio.append("TOTAL FOLHA: ")
                .append(Empregado.formatarValor(totalFolha))
                .append(QUEBRA_LINHA);

        return relatorio.toString();
    }

    private List<Contracheque> filtrarSecao(
            List<Contracheque> contracheques,
            String secao
    ) {
        List<Contracheque> resultado = new ArrayList<>();

        for (Contracheque contracheque : contracheques) {
            if (secao.equals(
                    contracheque
                            .getEmpregado()
                            .getSecaoFolha()
            )) {
                resultado.add(contracheque);
            }
        }

        return resultado;
    }

    private String montarSecaoHoristas(
            List<Contracheque> contracheques
    ) {
        StringBuilder secao = new StringBuilder();

        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(linhaTituloSecao("HORISTAS"))
                .append(QUEBRA_LINHA);
        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(
                "Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo"
        ).append(QUEBRA_LINHA);
        secao.append(
                "==================================== ===== ===== ============= ========= =============== ======================================"
        ).append(QUEBRA_LINHA);

        BigDecimal totalHoras = BigDecimal.ZERO;
        BigDecimal totalExtras = BigDecimal.ZERO;
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescontos = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;

        for (Contracheque contracheque : contracheques) {
            secao.append(
                    contracheque
                            .getEmpregado()
                            .formatarLinhaFolha(contracheque)
            ).append(QUEBRA_LINHA);

            totalHoras = totalHoras.add(
                    contracheque.getHorasNormais()
            );
            totalExtras = totalExtras.add(
                    contracheque.getHorasExtras()
            );
            totalBruto = totalBruto.add(
                    contracheque.getSalarioBruto()
            );
            totalDescontos = totalDescontos.add(
                    contracheque.getDescontos()
            );
            totalLiquido = totalLiquido.add(
                    contracheque.getSalarioLiquido()
            );
        }

        secao.append(QUEBRA_LINHA);
        secao.append(String.format(
                "%-36s %5s %5s %13s %9s %15s",
                "TOTAL HORISTAS",
                Empregado.formatarNumero(totalHoras),
                Empregado.formatarNumero(totalExtras),
                Empregado.formatarValor(totalBruto),
                Empregado.formatarValor(totalDescontos),
                Empregado.formatarValor(totalLiquido)
        )).append(QUEBRA_LINHA);
        secao.append(QUEBRA_LINHA);

        return secao.toString();
    }

    private String montarSecaoAssalariados(
            List<Contracheque> contracheques
    ) {
        StringBuilder secao = new StringBuilder();

        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(linhaTituloSecao("ASSALARIADOS"))
                .append(QUEBRA_LINHA);
        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(
                "Nome                                             Salario Bruto Descontos Salario Liquido Metodo"
        ).append(QUEBRA_LINHA);
        secao.append(
                "================================================ ============= ========= =============== ======================================"
        ).append(QUEBRA_LINHA);

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescontos = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;

        for (Contracheque contracheque : contracheques) {
            secao.append(
                    contracheque
                            .getEmpregado()
                            .formatarLinhaFolha(contracheque)
            ).append(QUEBRA_LINHA);

            totalBruto = totalBruto.add(
                    contracheque.getSalarioBruto()
            );
            totalDescontos = totalDescontos.add(
                    contracheque.getDescontos()
            );
            totalLiquido = totalLiquido.add(
                    contracheque.getSalarioLiquido()
            );
        }

        secao.append(QUEBRA_LINHA);
        secao.append(String.format(
                "%-48s %13s %9s %15s",
                "TOTAL ASSALARIADOS",
                Empregado.formatarValor(totalBruto),
                Empregado.formatarValor(totalDescontos),
                Empregado.formatarValor(totalLiquido)
        )).append(QUEBRA_LINHA);
        secao.append(QUEBRA_LINHA);

        return secao.toString();
    }

    private String montarSecaoComissionados(
            List<Contracheque> contracheques
    ) {
        StringBuilder secao = new StringBuilder();

        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(linhaTituloSecao("COMISSIONADOS"))
                .append(QUEBRA_LINHA);
        secao.append(SEPARADOR).append(QUEBRA_LINHA);
        secao.append(
                "Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo"
        ).append(QUEBRA_LINHA);
        secao.append(
                "===================== ======== ======== ======== ============= ========= =============== ======================================"
        ).append(QUEBRA_LINHA);

        BigDecimal totalFixo = BigDecimal.ZERO;
        BigDecimal totalVendas = BigDecimal.ZERO;
        BigDecimal totalComissao = BigDecimal.ZERO;
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescontos = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;

        for (Contracheque contracheque : contracheques) {
            secao.append(
                    contracheque
                            .getEmpregado()
                            .formatarLinhaFolha(contracheque)
            ).append(QUEBRA_LINHA);

            totalFixo = totalFixo.add(contracheque.getFixo());
            totalVendas = totalVendas.add(contracheque.getVendas());
            totalComissao = totalComissao.add(
                    contracheque.getComissao()
            );
            totalBruto = totalBruto.add(
                    contracheque.getSalarioBruto()
            );
            totalDescontos = totalDescontos.add(
                    contracheque.getDescontos()
            );
            totalLiquido = totalLiquido.add(
                    contracheque.getSalarioLiquido()
            );
        }

        secao.append(QUEBRA_LINHA);
        secao.append(String.format(
                "%-21s %8s %8s %8s %13s %9s %15s",
                "TOTAL COMISSIONADOS",
                Empregado.formatarValor(totalFixo),
                Empregado.formatarValor(totalVendas),
                Empregado.formatarValor(totalComissao),
                Empregado.formatarValor(totalBruto),
                Empregado.formatarValor(totalDescontos),
                Empregado.formatarValor(totalLiquido)
        )).append(QUEBRA_LINHA);
        secao.append(QUEBRA_LINHA);

        return secao.toString();
    }

    private String linhaTituloSecao(String titulo) {
        String inicio = "=".repeat(21) + " " + titulo + " ";

        return inicio + "=".repeat(127 - inicio.length());
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

    public void alterarMetodoPagamento(
            String emp,
            String metodoPagamento
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            MetodoPagamentoInvalidoException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        switch (metodoPagamento) {

            case "emMaos":
                empregado.configurarPagamentoEmMaos();
                return;

            case "correios":
                empregado.configurarPagamentoCorreios();
                return;

            case "banco":
                throw new MetodoPagamentoInvalidoException();

            default:
                throw new MetodoPagamentoInvalidoException();
        }
    }

    public void alterarMetodoPagamentoBanco(
            String emp,
            String banco,
            String agencia,
            String contaCorrente
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            BancoInvalidoException,
            AgenciaInvalidaException,
            ContaCorrenteInvalidaException {

        Empregado empregado = buscarEmpregadoObrigatorio(emp);

        if (banco == null || banco.isEmpty()) {
            throw new BancoInvalidoException();
        }

        if (agencia == null || agencia.isEmpty()) {
            throw new AgenciaInvalidaException();
        }

        if (contaCorrente == null || contaCorrente.isEmpty()) {
            throw new ContaCorrenteInvalidaException();
        }

        empregado.configurarPagamentoBanco(
                banco,
                agencia,
                contaCorrente
        );
    }

    /**
     * Altera o empregado para o tipo assalariado, preservando os
     * dados comuns da representação anterior.
     *
     * @param emp identificador do empregado
     */
    public void alterarTipoAssalariado(String emp)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado atual = buscarEmpregadoObrigatorio(emp);

        Empregado novoEmpregado = new EmpregadoAssalariado(
                atual.getId(),
                atual.getNome(),
                atual.getEndereco(),
                atual.getSalario()
        );

        atual.copiarEstadoComumPara(novoEmpregado);

        empregados.put(emp, novoEmpregado);
    }

    /**
     * Altera o empregado para o tipo comissionado, preservando os
     * dados comuns e associando a comissão informada.
     *
     * @param emp identificador do empregado
     * @param comissao taxa de comissão do novo empregado comissionado
     */
    public void alterarTipoComissionado(
            String emp,
            BigDecimal comissao
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado atual = buscarEmpregadoObrigatorio(emp);

        Empregado novoEmpregado = new EmpregadoComissionado(
                atual.getId(),
                atual.getNome(),
                atual.getEndereco(),
                atual.getSalario(),
                comissao
        );

        atual.copiarEstadoComumPara(novoEmpregado);

        empregados.put(emp, novoEmpregado);
    }

    /**
     * Altera o empregado para o tipo horista, preservando os dados
     * comuns e utilizando o novo salário informado.
     *
     * @param emp identificador do empregado
     * @param salario salário do novo empregado horista
     */
    public void alterarTipoHorista(
            String emp,
            BigDecimal salario
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado atual = buscarEmpregadoObrigatorio(emp);

        Empregado novoEmpregado = new EmpregadoHorista(
                atual.getId(),
                atual.getNome(),
                atual.getEndereco(),
                salario
        );

        atual.copiarEstadoComumPara(novoEmpregado);

        empregados.put(emp, novoEmpregado);
    }

}