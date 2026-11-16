package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;

public class CartaoPonto implements Serializable {

    private final LocalDate data;
    private final BigDecimal horas;

    public CartaoPonto(LocalDate data, BigDecimal horas)
            throws HorasInvalidasException {

        if (horas == null || horas.signum() <= 0) {
            throw new HorasInvalidasException();
        }

        this.data = Objects.requireNonNull(data);
        this.horas = horas;
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getHoras() {
        return horas;
    }
}
