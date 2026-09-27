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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

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

    private void validarIntervalo(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws DataInvalidaException {

        if (dataInicial.isAfter(dataFinal)) {
            throw new DataInvalidaException(
                    "Data inicial nao pode ser posterior aa data final."
            );
        }

    public void removerEmpregado(String id)
            throws EmpregadoNaoExisteException {

        if (!empregados.containsKey(id)) {
            throw new EmpregadoNaoExisteException();
        }

        empregados.remove(id);

    }

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

    public void zerar() {
        empregados.clear();
        proximoId = 1;

        File arquivo = new File(ARQUIVO_DADOS);

        if (arquivo.exists()) {
            arquivo.delete();
        }
    }

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
}