package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;

public class EmpregadoAssalariado extends Empregado {

    public EmpregadoAssalariado(
            String id,
            String nome,
            String endereco,
            BigDecimal salario
    ) throws EmpregadoInvalidoException, SalarioInvalidoException {

        super(id, nome, endereco, salario);
    }

    /**
     * Converte outro empregado em assalariado, mantendo o salário atual.
     */
    public EmpregadoAssalariado(Empregado origem)
            throws SalarioInvalidoException {

        super(origem, origem.getSalario());
    }

    @Override
    public String getTipo() {
        return "assalariado";
    }

    @Override
    public boolean deveReceberEm(LocalDate dataPagamento) {
        return dataPagamento.getDayOfMonth() == dataPagamento.lengthOfMonth();
    }

    @Override
    public LocalDate inicioPeriodoPagamento(LocalDate dataPagamento) {
        return dataPagamento.withDayOfMonth(1);
    }

    @Override
    protected Contracheque calcularDadosFolha(LocalDate dataPagamento) {
        BigDecimal salarioBruto = getSalario();

        return new Contracheque(
                this,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                salarioBruto,
                BigDecimal.ZERO,
                salarioBruto
        );
    }

    @Override
    protected void registrarEm(
            SecoesFolha secoes,
            Contracheque contracheque
    ) {
        secoes.adicionarAssalariado(contracheque);
    }
}
