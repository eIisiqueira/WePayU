package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;

public class EmpregadoHorista extends Empregado {

    private static final BigDecimal LIMITE_HORAS_NORMAIS =
            new BigDecimal("8");

    private static final BigDecimal ADICIONAL_HORA_EXTRA =
            new BigDecimal("1.5");

    private final List<CartaoPonto> cartoes = new ArrayList<>();

    public EmpregadoHorista(
            String id,
            String nome,
            String endereco,
            BigDecimal salario
    ) throws EmpregadoInvalidoException, SalarioInvalidoException {

        super(id, nome, endereco, salario);
    }

    public EmpregadoHorista(Empregado origem, BigDecimal salario)
            throws SalarioInvalidoException {

        super(origem, salario);
    }

    @Override
    public String getTipo() {
        return "horista";
    }

    @Override
    public boolean deveReceberEm(LocalDate dataPagamento) {
        return dataPagamento.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    @Override
    public LocalDate inicioPeriodoPagamento(LocalDate dataPagamento) {
        return dataPagamento.minusDays(6);
    }

    /**
     * Considera como início do contrato a data do primeiro cartão de ponto.
     *
     * @return data do primeiro cartão ou {@code null} se ainda não houver cartões
     */
    @Override
    protected LocalDate dataInicioContrato() {
        LocalDate primeiraData = null;

        for (CartaoPonto cartao : cartoes) {
            if (primeiraData == null
                    || cartao.getData().isBefore(primeiraData)) {

                primeiraData = cartao.getData();
            }
        }

        return primeiraData;
    }

    @Override
    protected Contracheque calcularDadosFolha(LocalDate dataPagamento) {
        LocalDate inicio = inicioPeriodoPagamento(dataPagamento);
        LocalDate fimExclusivo = dataPagamento.plusDays(1);

        BigDecimal horasNormais =
                getHorasNormaisTrabalhadas(inicio, fimExclusivo);

        BigDecimal horasExtras =
                getHorasExtrasTrabalhadas(inicio, fimExclusivo);

        BigDecimal salarioBruto = horasNormais
                .multiply(getSalario())
                .add(horasExtras
                        .multiply(getSalario())
                        .multiply(ADICIONAL_HORA_EXTRA));

        return new Contracheque(
                this,
                horasNormais,
                horasExtras,
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
        secoes.adicionarHorista(contracheque);
    }

    @Override
    public void lancaCartao(LocalDate data, BigDecimal horas)
            throws HorasInvalidasException {

        cartoes.add(new CartaoPonto(data, horas));
    }

    /**
     * Soma as horas normais dos cartões do intervalo, considerando no
     * máximo oito horas normais por cartão.
     */
    @Override
    public BigDecimal getHorasNormaisTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartaoPonto cartao : cartoes) {
            if (estaNoIntervalo(cartao.getData(), dataInicial, dataFinal)) {
                total = total.add(
                        cartao.getHoras().min(LIMITE_HORAS_NORMAIS)
                );
            }
        }

        return total;
    }

    /**
     * Soma apenas as horas que excedem as oito horas normais de cada cartão.
     */
    @Override
    public BigDecimal getHorasExtrasTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartaoPonto cartao : cartoes) {
            if (estaNoIntervalo(cartao.getData(), dataInicial, dataFinal)
                    && cartao.getHoras()
                    .compareTo(LIMITE_HORAS_NORMAIS) > 0) {

                total = total.add(
                        cartao.getHoras().subtract(LIMITE_HORAS_NORMAIS)
                );
            }
        }

        return total;
    }
}
