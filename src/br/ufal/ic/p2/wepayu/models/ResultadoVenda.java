package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;

public class ResultadoVenda implements Serializable {

    private final LocalDate data;
    private final BigDecimal valor;

    public ResultadoVenda(LocalDate data, BigDecimal valor)
            throws ValorVendaInvalidoException {

        if (valor == null || valor.signum() <= 0) {
            throw new ValorVendaInvalidoException();
        }

        this.data = Objects.requireNonNull(data);
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
