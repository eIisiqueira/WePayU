package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import java.math.RoundingMode;
import java.io.Serializable;
import java.time.LocalDate;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoHoristaException;

public abstract class Empregado implements Serializable {

    private String id;
    private String nome;
    private String endereco;
    private BigDecimal salario;
    private boolean sindicalizado;

    public Empregado(String id, String nome, String endereco, BigDecimal salario) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.salario = salario;
        this.sindicalizado = false;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public boolean isSindicalizado() {
        return sindicalizado;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public void setSindicalizado(boolean sindicalizado) {
        this.sindicalizado = sindicalizado;
    }

    public abstract String getTipo();

    public String getAtributo(String atributo)
            throws AtributoNaoExisteException {

        switch (atributo) {

            case "nome":
                return getNome();

            case "endereco":
                return getEndereco();

            case "tipo":
                return getTipo();

            case "salario":
                return formatarValor(getSalario());

            case "sindicalizado":
                return String.valueOf(isSindicalizado());

            default:
                throw new AtributoNaoExisteException();
        }
    }

    protected String formatarValor(BigDecimal valor) {
        return valor
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace(".", ",");
    }

    public void lancaCartao(LocalDate data, BigDecimal horas)
            throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

    public BigDecimal getHorasNormaisTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

    public BigDecimal getHorasExtrasTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws EmpregadoNaoHoristaException {

        throw new EmpregadoNaoHoristaException();
    }

}
