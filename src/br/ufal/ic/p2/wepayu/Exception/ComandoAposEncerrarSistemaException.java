package br.ufal.ic.p2.wepayu.Exception;

public class ComandoAposEncerrarSistemaException extends Exception {

    public ComandoAposEncerrarSistemaException() {
        super("Nao pode dar comandos depois de encerrarSistema.");
    }
}