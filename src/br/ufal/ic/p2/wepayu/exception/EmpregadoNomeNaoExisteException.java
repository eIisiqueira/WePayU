package br.ufal.ic.p2.wepayu.exception;

public class EmpregadoNomeNaoExisteException extends Exception {

    public EmpregadoNomeNaoExisteException() {
        super("Nao ha empregado com esse nome.");
    }
}
