package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;

/**
 * Representa um empregado comissionado, que possui salário base
 * e uma taxa de comissão associada.
 *
 * Especializa a consulta de atributos do empregado para disponibilizar
 * também o valor da comissão.
 */

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