package br.ufal.ic.p2.wepayu.Exception;

public class ValorSindicalizadoInvalidoException extends Exception {

    public ValorSindicalizadoInvalidoException() {
        super("Valor deve ser true ou false.");
    }
}