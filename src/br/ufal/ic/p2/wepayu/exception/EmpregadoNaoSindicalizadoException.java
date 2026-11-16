package br.ufal.ic.p2.wepayu.exception;

public class EmpregadoNaoSindicalizadoException extends Exception {

    public EmpregadoNaoSindicalizadoException() {
        super("Empregado nao eh sindicalizado.");
    }
}
