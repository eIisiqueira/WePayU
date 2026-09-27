package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class EmpregadoHorista extends Empregado {

    private static final BigDecimal LIMITE_HORAS_NORMAIS =
            new BigDecimal("8");

    private final List<CartaoPonto> cartoes = new ArrayList<>();

    public EmpregadoHorista(
            String id,
            String nome,
            String endereco,
            BigDecimal salario
    ) {
        super(id, nome, endereco, salario);
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
     * Calcula os dados brutos da folha do empregado horista,
     * considerando horas normais e horas extras do período.
     *
     * @param dataPagamento data do pagamento
     * @return contracheque com os dados brutos do período
     */
    @Override
    protected Contracheque calcularDadosFolha(LocalDate dataPagamento) {
        LocalDate inicio = inicioPeriodoPagamento(dataPagamento);
        LocalDate fimExclusivo = dataPagamento.plusDays(1);

        BigDecimal horasNormais =
                getHorasNormaisTrabalhadas(inicio, fimExclusivo);

        BigDecimal horasExtras =
                getHorasExtrasTrabalhadas(inicio, fimExclusivo);

        BigDecimal parteNormal =
                horasNormais.multiply(getSalario());

        BigDecimal parteExtra =
                horasExtras
                        .multiply(getSalario())
                        .multiply(new BigDecimal("1.5"));

        BigDecimal salarioBruto =
                parteNormal.add(parteExtra);

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
    public String getSecaoFolha() {
        return "HORISTAS";
    }

    @Override
    public String formatarLinhaFolha(Contracheque contracheque) {
        return String.format(
                "%-36s %5s %5s %13s %9s %15s %s",
                getNome(),
                formatarNumero(contracheque.getHorasNormais()),
                formatarNumero(contracheque.getHorasExtras()),
                formatarValor(contracheque.getSalarioBruto()),
                formatarValor(contracheque.getDescontos()),
                formatarValor(contracheque.getSalarioLiquido()),
                descricaoPagamento()
        );
    }

    /**
     * Registra um cartão de ponto para este empregado horista.
     *
     * A data e a quantidade de horas já chegam validadas pela camada
     * responsável por coordenar a operação.
     *
     * @param data data do cartão de ponto
     * @param horas quantidade de horas trabalhadas
     */
    @Override
    public void lancaCartao(LocalDate data, BigDecimal horas) {
        cartoes.add(new CartaoPonto(data, horas));
    }

    /**
     * Soma as horas normais registradas nos cartões de ponto pertencentes
     * ao intervalo informado.
     *
     * Em cada dia são consideradas no máximo oito horas como normais.
     *
     * @param dataInicial início inclusivo do intervalo
     * @param dataFinal fim exclusivo do intervalo
     * @return total de horas normais trabalhadas
     */
    @Override
    public BigDecimal getHorasNormaisTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartaoPonto cartao : cartoes) {
            if (estaNoIntervalo(
                    cartao.getData(),
                    dataInicial,
                    dataFinal
            )) {
                // Em cada cartão, apenas as primeiras 8 horas contam como horas normais.
                total = total.add(
                        cartao.getHoras()
                                .min(LIMITE_HORAS_NORMAIS)
                );
            }
        }

        return total;
    }

    /**
     * Soma, nos cartões de ponto pertencentes ao intervalo informado,
     * somente as horas trabalhadas além das oito horas normais de cada dia.
     *
     * @param dataInicial início inclusivo do intervalo
     * @param dataFinal fim exclusivo do intervalo
     * @return total de horas extras trabalhadas
     */
    @Override
    public BigDecimal getHorasExtrasTrabalhadas(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartaoPonto cartao : cartoes) {
            if (estaNoIntervalo(
                    cartao.getData(),
                    dataInicial,
                    dataFinal
            )
                    && cartao.getHoras()
                    .compareTo(LIMITE_HORAS_NORMAIS) > 0) {

                total = total.add(
                        cartao.getHoras()
                                .subtract(LIMITE_HORAS_NORMAIS)
                );
            }
        }

        return total;
    }

    private boolean estaNoIntervalo(
            LocalDate data,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        // Os testes consideram o início do intervalo inclusivo e o fim exclusivo.
        return !data.isBefore(dataInicial)
                && data.isBefore(dataFinal);
    }
}