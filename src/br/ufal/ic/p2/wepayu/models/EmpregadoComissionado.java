package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;


public class EmpregadoComissionado extends Empregado {

    private BigDecimal comissao;

    public EmpregadoComissionado(
            String id,
            String nome,
            String endereco,
            BigDecimal salario,
            BigDecimal comissao) {

        super(id, nome, endereco, salario);
        this.comissao = comissao;
    }

    public BigDecimal getValorComissao() {
        return comissao;
    }

    @Override
    public String getTipo() {
        return "comissionado";
    }

    @Override
    public String getAtributo(String atributo)
            throws AtributoNaoExisteException {

        if (atributo.equals("comissao")) {
            return formatarValor(comissao);
        }

        return super.getAtributo(atributo);
    }

}