package br.ufal.ic.p2.wepayu.Exception;

public class EmpregadoNaoComissionadoException extends Exception {

    public EmpregadoNaoComissionadoException() {
        super("Empregado nao eh comissionado.");
    }
}