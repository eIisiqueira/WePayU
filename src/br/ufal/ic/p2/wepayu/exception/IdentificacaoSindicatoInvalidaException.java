package br.ufal.ic.p2.wepayu.exception;

public class IdentificacaoSindicatoInvalidaException extends Exception {

    public IdentificacaoSindicatoInvalidaException() {
        super("Identificacao do sindicato nao pode ser nula.");
    }
}
