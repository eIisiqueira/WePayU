package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;

public final class Contracheque {

    private final Empregado empregado;
    private final BigDecimal horasNormais;
    private final BigDecimal horasExtras;
    private final BigDecimal fixo;
    private final BigDecimal vendas;
    private final BigDecimal comissao;
    private final BigDecimal salarioBruto;
    private final BigDecimal descontos;
    private final BigDecimal salarioLiquido;

    public Contracheque(
            Empregado empregado,
            BigDecimal horasNormais,
            BigDecimal horasExtras,
            BigDecimal fixo,
            BigDecimal vendas,
            BigDecimal comissao,
            BigDecimal salarioBruto,
            BigDecimal descontos,
            BigDecimal salarioLiquido
    ) {
        this.empregado = empregado;
        this.horasNormais = horasNormais;
        this.horasExtras = horasExtras;
        this.fixo = fixo;
        this.vendas = vendas;
        this.comissao = comissao;
        this.salarioBruto = salarioBruto;
        this.descontos = descontos;
        this.salarioLiquido = salarioLiquido;
    }

    public Empregado getEmpregado() {
        return empregado;
    }

    public BigDecimal getHorasNormais() {
        return horasNormais;
    }

    public BigDecimal getHorasExtras() {
        return horasExtras;
    }

    public BigDecimal getFixo() {
        return fixo;
    }

    public BigDecimal getVendas() {
        return vendas;
    }

    public BigDecimal getComissao() {
        return comissao;
    }

    public BigDecimal getSalarioBruto() {
        return salarioBruto;
    }

    public BigDecimal getDescontos() {
        return descontos;
    }

    public BigDecimal getSalarioLiquido() {
        return salarioLiquido;
    }

    public Contracheque comDeducoes(
            BigDecimal descontos,
            BigDecimal salarioLiquido
    ) {
        return new Contracheque(
                empregado,
                horasNormais,
                horasExtras,
                fixo,
                vendas,
                comissao,
                salarioBruto,
                descontos,
                salarioLiquido
        );
    }
}