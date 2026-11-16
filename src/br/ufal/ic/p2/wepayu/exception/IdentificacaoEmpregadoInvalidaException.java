package br.ufal.ic.p2.wepayu.exception;

public class IdentificacaoEmpregadoInvalidaException extends Exception {

    public IdentificacaoEmpregadoInvalidaException() {
        super("Identificacao do empregado nao pode ser nula.");
    }
}
