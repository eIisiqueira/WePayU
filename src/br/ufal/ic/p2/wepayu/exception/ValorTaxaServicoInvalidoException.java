package br.ufal.ic.p2.wepayu.exception;

public class ValorTaxaServicoInvalidoException extends Exception {

    public ValorTaxaServicoInvalidoException() {
        super("Valor deve ser positivo.");
    }
}
