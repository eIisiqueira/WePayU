package br.ufal.ic.p2.wepayu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.ufal.ic.p2.wepayu.exception.ComandoAposEncerrarSistemaException;
import br.ufal.ic.p2.wepayu.exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.DataInvalidaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNomeNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoEmpregadoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoMembroInvalidaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoSindicatoDuplicadaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoSindicatoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.MembroNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.NaoHaComandoADesfazerException;
import br.ufal.ic.p2.wepayu.exception.NaoHaComandoARefazerException;
import br.ufal.ic.p2.wepayu.exception.PersistenciaException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.exception.TaxaSindicalInvalidaException;
import br.ufal.ic.p2.wepayu.exception.ValorTaxaServicoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;
import br.ufal.ic.p2.wepayu.models.Contracheque;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.MembroSindicato;
import br.ufal.ic.p2.wepayu.models.MetodoPagamento;

/**
 * Coordena as operações centrais do sistema de folha de pagamento.
 *
 * Mantém e localiza os empregados, gera identificadores, aplica as regras
 * que envolvem mais de um empregado (como a unicidade da identificação
 * sindical), registra cada alteração no histórico de undo/redo e cuida
 * da persistência. As regras de cada empregado ficam nos objetos de domínio.
 */
public class SistemaFolha {

    /** Cria um empregado a partir do identificador gerado pelo sistema. */
    @FunctionalInterface
    interface FabricaEmpregado {
        Empregado criar(String id) throws EmpregadoInvalidoException,
                SalarioInvalidoException, ComissaoInvalidaException;
    }

    /** Produz a nova representação de um empregado que mudou de tipo. */
    @FunctionalInterface
    interface ConversaoTipo {
        Empregado converter(Empregado atual)
                throws SalarioInvalidoException, ComissaoInvalidaException;
    }

    private static final String ARQUIVO_DADOS = "wepayu.dat";

    private final Map<String, Empregado> empregados = new LinkedHashMap<>();
    private int proximoId = 1;

    private final Deque<byte[]> pilhaUndo = new ArrayDeque<>();
    private final Deque<byte[]> pilhaRedo = new ArrayDeque<>();
    private boolean encerrado;

    public SistemaFolha() {
        carregar();
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
     * Cadastra um novo empregado. O identificador só é consumido se o
     * empregado for criado com sucesso.
     *
     * @param fabrica cria o empregado a partir do identificador
     * @return identificador do novo empregado
     */
    public String cadastrar(FabricaEmpregado fabrica)
            throws EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        Empregado empregado = fabrica.criar(String.valueOf(proximoId));

        byte[] estadoAnterior = criarSnapshot();
        empregados.put(empregado.getId(), empregado);
        proximoId++;
        confirmarTransacao(estadoAnterior);

        return empregado.getId();
    }

    public void removerEmpregado(String id)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        buscarEmpregado(id);

        byte[] estadoAnterior = criarSnapshot();
        empregados.remove(id);
        confirmarTransacao(estadoAnterior);
    }

    /**
     * Localiza um empregado pelo identificador.
     *
     * @param id identificador do empregado
     * @return empregado encontrado
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se não houver empregado com esse identificador
     */
    public Empregado buscarEmpregado(String id)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        if (id == null || id.isEmpty()) {
            throw new IdentificacaoEmpregadoInvalidaException();
        }

        Empregado empregado = empregados.get(id);

        if (empregado == null) {
            throw new EmpregadoNaoExisteException();
        }

        return empregado;
    }

    /**
     * Procura empregados cujo nome contenha o texto informado e retorna
     * o ID da correspondência indicada, na ordem de cadastro.
     *
     * @param nome trecho de nome utilizado na busca
     * @param indice posição da correspondência desejada, começando em 1
     * @return identificador do empregado encontrado
     * @throws EmpregadoNomeNaoExisteException se não houver correspondência
     *                                         na posição solicitada
     */
    public String buscarEmpregadoPorNome(String nome, int indice)
            throws EmpregadoNomeNaoExisteException {

        int contador = 0;

        for (Empregado empregado : empregados.values()) {
            if (empregado.getNome().contains(nome) && ++contador == indice) {
                return empregado.getId();
            }
        }

        throw new EmpregadoNomeNaoExisteException();
    }

    public void alterarNome(String emp, String nome)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoInvalidoException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.alterarNome(nome);
        confirmarTransacao(estadoAnterior);
    }

    public void alterarEndereco(String emp, String endereco)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoInvalidoException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.alterarEndereco(endereco);
        confirmarTransacao(estadoAnterior);
    }

    public void alterarSalario(String emp, BigDecimal salario)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            SalarioInvalidoException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.alterarSalario(salario);
        confirmarTransacao(estadoAnterior);
    }

    public void alterarComissao(String emp, BigDecimal comissao)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            ComissaoInvalidaException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.alterarComissao(comissao);
        confirmarTransacao(estadoAnterior);
    }

    public void alterarMetodoPagamento(String emp, MetodoPagamento metodo)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.alterarMetodoPagamento(metodo);
        confirmarTransacao(estadoAnterior);
    }

    /**
     * Substitui o empregado por outra subclasse, preservando seus dados
     * comuns. A nova instância é criada antes de qualquer alteração, de
     * modo que uma conversão inválida não modifica o sistema.
     *
     * @param emp identificador do empregado
     * @param conversao cria a representação do novo tipo
     */
    public void alterarTipo(String emp, ConversaoTipo conversao)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        Empregado novo = conversao.converter(buscarEmpregado(emp));

        byte[] estadoAnterior = criarSnapshot();
        empregados.put(emp, novo);
        confirmarTransacao(estadoAnterior);
    }

    /**
     * Associa o empregado ao sindicato, garantindo que a identificação
     * sindical não pertença a outro empregado.
     */
    public void sindicalizar(
            String emp,
            String idSindicato,
            BigDecimal taxaSindical
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException,
            IdentificacaoSindicatoDuplicadaException {

        Empregado empregado = buscarEmpregado(emp);
        MembroSindicato membro = new MembroSindicato(idSindicato, taxaSindical);

        for (Empregado outro : empregados.values()) {
            if (outro != empregado
                    && possuiIdentificacaoSindical(outro, idSindicato)) {

                throw new IdentificacaoSindicatoDuplicadaException();
            }
        }

        byte[] estadoAnterior = criarSnapshot();
        empregado.sindicalizar(membro);
        confirmarTransacao(estadoAnterior);
    }

    public void dessindicalizar(String emp)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.dessindicalizar();
        confirmarTransacao(estadoAnterior);
    }

    public void lancaTaxaServico(String membro, LocalDate data, BigDecimal valor)
            throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException,
            ValorTaxaServicoInvalidoException {

        MembroSindicato membroSindicato = buscarMembro(membro);

        byte[] estadoAnterior = criarSnapshot();
        membroSindicato.lancaTaxaServico(data, valor);
        confirmarTransacao(estadoAnterior);
    }

    public void lancaVenda(String emp, LocalDate data, BigDecimal valor)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            ValorVendaInvalidoException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.lancaVenda(data, valor);
        confirmarTransacao(estadoAnterior);
    }

    public void lancaCartao(String emp, LocalDate data, BigDecimal horas)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            HorasInvalidasException {

        Empregado empregado = buscarEmpregado(emp);

        byte[] estadoAnterior = criarSnapshot();
        empregado.lancaCartao(data, horas);
        confirmarTransacao(estadoAnterior);
    }

    public BigDecimal getTaxasServico(String emp, LocalDate inicio, LocalDate fim)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoSindicalizadoException,
            DataInvalidaException {

        MembroSindicato membro = buscarEmpregado(emp).getMembroSindicato();
        validarIntervalo(inicio, fim);

        return membro.getTaxasServico(inicio, fim);
    }

    public BigDecimal getVendasRealizadas(String emp, LocalDate inicio, LocalDate fim)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException {

        Empregado empregado = buscarEmpregado(emp);
        validarIntervalo(inicio, fim);

        return empregado.getVendasRealizadas(inicio, fim);
    }

    public BigDecimal getHorasNormaisTrabalhadas(String emp, LocalDate inicio, LocalDate fim)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        Empregado empregado = buscarEmpregado(emp);
        validarIntervalo(inicio, fim);

        return empregado.getHorasNormaisTrabalhadas(inicio, fim);
    }

    public BigDecimal getHorasExtrasTrabalhadas(String emp, LocalDate inicio, LocalDate fim)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        Empregado empregado = buscarEmpregado(emp);
        validarIntervalo(inicio, fim);

        return empregado.getHorasExtrasTrabalhadas(inicio, fim);
    }

    /**
     * Calcula o total bruto da folha na data informada, sem executá-la.
     *
     * @param data data da folha
     * @return soma dos salários brutos dos empregados que recebem na data
     */
    public BigDecimal totalFolha(LocalDate data) {
        return RelatorioFolha.somaBruto(calcularContracheques(data));
    }

    /**
     * Executa a folha da data informada, grava o relatório no arquivo
     * indicado e registra a operação no histórico. O arquivo gerado
     * não faz parte do undo.
     *
     * @param data data da folha
     * @param saida caminho do arquivo de saída
     */
    public void rodaFolha(LocalDate data, String saida) {
        byte[] estadoAnterior = criarSnapshot();

        new RelatorioFolha(data, calcularContracheques(data)).gravar(saida);

        confirmarTransacao(estadoAnterior);
    }

    /**
     * Desfaz a última transação registrada.
     *
     * @throws ComandoAposEncerrarSistemaException se o sistema já foi encerrado
     * @throws NaoHaComandoADesfazerException se não houver transação a desfazer
     */
    public void undo()
            throws ComandoAposEncerrarSistemaException,
            NaoHaComandoADesfazerException {

        verificarAtivo();

        if (pilhaUndo.isEmpty()) {
            throw new NaoHaComandoADesfazerException();
        }

        byte[] estadoAtual = criarSnapshot();
        restaurarSnapshot(pilhaUndo.peek());
        pilhaUndo.pop();
        pilhaRedo.push(estadoAtual);
    }

    /**
     * Refaz a última transação desfeita.
     *
     * @throws ComandoAposEncerrarSistemaException se o sistema já foi encerrado
     * @throws NaoHaComandoARefazerException se não houver transação a refazer
     */
    public void redo()
            throws ComandoAposEncerrarSistemaException,
            NaoHaComandoARefazerException {

        verificarAtivo();

        if (pilhaRedo.isEmpty()) {
            throw new NaoHaComandoARefazerException();
        }

        byte[] estadoAtual = criarSnapshot();
        restaurarSnapshot(pilhaRedo.peek());
        pilhaRedo.pop();
        pilhaUndo.push(estadoAtual);
    }

    /**
     * Reinicia o estado do sistema, removendo os empregados, restaurando a
     * sequência de identificadores e excluindo o arquivo de persistência.
     * A operação entra no histórico e pode ser desfeita.
     */
    public void zerar() {
        byte[] estadoAnterior = criarSnapshot();

        empregados.clear();
        proximoId = 1;

        File arquivo = new File(ARQUIVO_DADOS);

        if (arquivo.exists()) {
            arquivo.delete();
        }

        confirmarTransacao(estadoAnterior);
    }

    /**
     * Persiste o estado atual e encerra a sessão. Depois disso,
     * undo e redo não são mais permitidos.
     */
    public void encerrar() {
        salvar();
        encerrado = true;
    }

    /**
     * Persiste em arquivo os empregados e o próximo identificador.
     *
     * @throws PersistenciaException se ocorrer falha na gravação
     */
    public void salvar() {
        try (ObjectOutputStream saida = new ObjectOutputStream(
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

        try (ObjectInputStream entrada = new ObjectInputStream(
                new FileInputStream(arquivo))) {

            // Mesma ordem usada em salvar(): empregados e depois próximo ID.
            empregados.putAll((Map<String, Empregado>) entrada.readObject());
            proximoId = entrada.readInt();

        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException("Erro ao carregar dados.");
        }
    }

    private List<Contracheque> calcularContracheques(LocalDate data) {
        List<Contracheque> contracheques = new ArrayList<>();

        for (Empregado empregado : empregados.values()) {
            if (empregado.deveReceberEm(data)) {
                contracheques.add(empregado.calcularContracheque(data));
            }
        }

        contracheques.sort(Comparator.comparing(
                contracheque -> contracheque.getEmpregado().getNome()));

        return contracheques;
    }

    private MembroSindicato buscarMembro(String identificacao)
            throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException {

        if (identificacao == null || identificacao.isEmpty()) {
            throw new IdentificacaoMembroInvalidaException();
        }

        for (Empregado empregado : empregados.values()) {
            Optional<MembroSindicato> membro = empregado.getSindicato();

            if (membro.isPresent()
                    && membro.get().possuiIdentificacao(identificacao)) {

                return membro.get();
            }
        }

        throw new MembroNaoExisteException();
    }

    private static boolean possuiIdentificacaoSindical(
            Empregado empregado,
            String identificacao
    ) {
        return empregado.getSindicato()
                .map(membro -> membro.possuiIdentificacao(identificacao))
                .orElse(false);
    }

    private static void validarIntervalo(LocalDate inicio, LocalDate fim)
            throws DataInvalidaException {

        if (inicio.isAfter(fim)) {
            throw new DataInvalidaException(
                    "Data inicial nao pode ser posterior aa data final."
            );
        }
    }

    private void verificarAtivo() throws ComandoAposEncerrarSistemaException {
        if (encerrado) {
            throw new ComandoAposEncerrarSistemaException();
        }
    }

    private void confirmarTransacao(byte[] estadoAnterior) {
        pilhaUndo.push(estadoAnterior);
        pilhaRedo.clear();
    }

    private byte[] criarSnapshot() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream saida = new ObjectOutputStream(bytes)) {

            saida.writeObject(empregados);
            saida.writeInt(proximoId);
            saida.flush();

            return bytes.toByteArray();

        } catch (IOException e) {
            throw new PersistenciaException("Erro ao criar snapshot.");
        }
    }

    @SuppressWarnings("unchecked")
    private void restaurarSnapshot(byte[] snapshot) {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new ByteArrayInputStream(snapshot))) {

            Map<String, Empregado> restaurados =
                    (Map<String, Empregado>) entrada.readObject();
            int proximoIdRestaurado = entrada.readInt();

            empregados.clear();
            empregados.putAll(restaurados);
            proximoId = proximoIdRestaurado;

        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException("Erro ao restaurar snapshot.");
        }
    }
}
