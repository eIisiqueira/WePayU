package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;

public class EmpregadoAssalariado extends Empregado {

    public EmpregadoAssalariado(
            String id,
            String nome,
            String endereco,
            BigDecimal salario) {

        super(id, nome, endereco, salario);
    }

    @Override
    public String getTipo() {
        return "assalariado";
    }
}