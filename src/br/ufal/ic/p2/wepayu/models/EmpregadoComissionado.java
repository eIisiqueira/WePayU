package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um empregado comissionado, que possui salário base
 * e uma taxa de comissão associada.
 *
 * Especializa a consulta de atributos do empregado para disponibilizar
 * também o valor da comissão.
 */

public class EmpregadoComissionado extends Empregado {

    private BigDecimal comissao;
    private final List<ResultadoVenda> vendas = new ArrayList<>();

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
    @Override
    public void lancaVenda(LocalDate data, BigDecimal valor) {
        vendas.add(new ResultadoVenda(data, valor));
    }

    @Override
    public BigDecimal getVendasRealizadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (ResultadoVenda venda : vendas) {
            if (estaNoIntervalo(
                    venda.getData(),
                    dataInicial,
                    dataFinal
            )) {
                total = total.add(venda.getValor());
            }
        }

        return total;
    }

    private boolean estaNoIntervalo(
            LocalDate data,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        return !data.isBefore(dataInicial)
                && data.isBefore(dataFinal);
    }

}