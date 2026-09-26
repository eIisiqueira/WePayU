package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNomeNaoExisteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.*;
import br.ufal.ic.p2.wepayu.Exception.PersistenciaException;

public class SistemaFolha {

    private static final String ARQUIVO_DADOS = "wepayu.dat";

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