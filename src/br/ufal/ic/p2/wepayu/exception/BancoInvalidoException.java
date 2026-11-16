package br.ufal.ic.p2.wepayu.exception;

public class BancoInvalidoException extends Exception {

    public BancoInvalidoException() {
        super("Banco nao pode ser nulo.");
    }
}
