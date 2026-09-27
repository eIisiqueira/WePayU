package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CartaoPonto implements Serializable {

    private final LocalDate data;
    private final BigDecimal horas;

    public CartaoPonto(LocalDate data, BigDecimal horas) {
        this.data = data;
        this.horas = horas;
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getHoras() {
        return horas;
    }
}