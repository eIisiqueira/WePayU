package br.ufal.ic.p2.wepayu.models;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public void lancaCartao(LocalDate data, BigDecimal horas) {
        cartoes.add(new CartaoPonto(data, horas));
    }

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
                total = total.add(
                        cartao.getHoras()
                                .min(LIMITE_HORAS_NORMAIS)
                );
            }
        }

        return total;
    }

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
        return !data.isBefore(dataInicial)
                && data.isBefore(dataFinal);
    }
}