package br.ufal.ic.p2.wepayu.exception;

public class AgenciaInvalidaException extends Exception {

    public AgenciaInvalidaException() {
        super("Agencia nao pode ser nulo.");
    }
}
