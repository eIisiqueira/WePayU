package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Override
    public boolean deveReceberEm(LocalDate dataPagamento) {
        return dataPagamento.getDayOfMonth() == dataPagamento.lengthOfMonth();
    }

    @Override
    public LocalDate inicioPeriodoPagamento(LocalDate dataPagamento) {
        return dataPagamento.withDayOfMonth(1);
    }

    /**
     * Calcula os dados brutos da folha do empregado assalariado
     * com base em seu salário mensal.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque com os dados brutos do pagamento
     */
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
    public String getSecaoFolha() {
        return "ASSALARIADOS";
    }

    @Override
    public String formatarLinhaFolha(Contracheque contracheque) {
        return String.format(
                "%-48s %13s %9s %15s %s",
                getNome(),
                formatarValor(contracheque.getSalarioBruto()),
                formatarValor(contracheque.getDescontos()),
                formatarValor(contracheque.getSalarioLiquido()),
                descricaoPagamento()
        );
    }
}