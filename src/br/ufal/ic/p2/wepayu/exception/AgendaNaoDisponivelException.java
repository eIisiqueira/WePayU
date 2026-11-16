package br.ufal.ic.p2.wepayu.exception;

public class AgendaNaoDisponivelException extends Exception {

    public AgendaNaoDisponivelException() {
        super("Agenda de pagamento nao esta disponivel");
    }
}
