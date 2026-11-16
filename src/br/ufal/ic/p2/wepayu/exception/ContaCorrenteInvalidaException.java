package br.ufal.ic.p2.wepayu.exception;

public class ContaCorrenteInvalidaException extends Exception {

    public ContaCorrenteInvalidaException() {
        super("Conta corrente nao pode ser nulo.");
    }
}
