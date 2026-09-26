package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;

import java.util.LinkedHashMap;
import java.util.Map;

public class SistemaFolha {

    private final Map<String, Empregado> empregados;
    private int proximoId;

    public SistemaFolha() {
        this.empregados = new LinkedHashMap<>();
        this.proximoId = 1;
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

    public void zerar() {
        empregados.clear();
        proximoId = 1;
    }
}