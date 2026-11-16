package br.ufal.ic.p2.wepayu.exception;

public class ValorVendaInvalidoException extends Exception {

    public ValorVendaInvalidoException() {
        super("Valor deve ser positivo.");
    }
}
