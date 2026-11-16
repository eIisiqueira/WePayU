package br.ufal.ic.p2.wepayu.Exception;

public class ValorTaxaServicoInvalidoException extends Exception {

    public ValorTaxaServicoInvalidoException() {
        super("Valor deve ser positivo.");
    }
}