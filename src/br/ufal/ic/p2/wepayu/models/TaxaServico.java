package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import br.ufal.ic.p2.wepayu.exception.ValorTaxaServicoInvalidoException;

/**
 * Representa uma taxa de serviço lançada pelo sindicato para um membro.
 */
public class TaxaServico implements Serializable {

    private final LocalDate data;
    private final BigDecimal valor;

    public TaxaServico(LocalDate data, BigDecimal valor)
            throws ValorTaxaServicoInvalidoException {

        if (valor == null || valor.signum() <= 0) {
            throw new ValorTaxaServicoInvalidoException();
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
