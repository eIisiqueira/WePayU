package br.ufal.ic.p2.wepayu.Exception;

public class IdentificacaoMembroInvalidaException extends Exception {

    public IdentificacaoMembroInvalidaException() {
        super("Identificacao do membro nao pode ser nula.");
    }
}