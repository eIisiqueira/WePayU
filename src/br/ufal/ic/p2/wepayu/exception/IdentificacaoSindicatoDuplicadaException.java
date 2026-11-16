package br.ufal.ic.p2.wepayu.Exception;

public class IdentificacaoSindicatoDuplicadaException extends Exception {

    public IdentificacaoSindicatoDuplicadaException() {
        super("Ha outro empregado com esta identificacao de sindicato");
    }
}