package br.ufal.ic.p2.wepayu.Exception;

public class IdentificacaoEmpregadoInvalidaException extends Exception {

    public IdentificacaoEmpregadoInvalidaException() {
        super("Identificacao do empregado nao pode ser nula.");
    }
}