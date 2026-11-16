package br.ufal.ic.p2.wepayu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converte valores numéricos para o formato textual esperado pela
 * interface de testes e pela folha de pagamento (vírgula decimal).
 */
final class Formatador {

    private Formatador() {
    }

    /** Valor monetário com duas casas decimais, ex.: {@code 2300,45}. */
    static String valor(BigDecimal valor) {
        return valor
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace(".", ",");
    }

    /** Número sem zeros à direita, ex.: {@code 1,5} ou {@code 16}. */
    static String numero(BigDecimal valor) {
        return valor
                .stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }
}
