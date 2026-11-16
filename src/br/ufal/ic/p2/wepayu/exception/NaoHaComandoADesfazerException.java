package br.ufal.ic.p2.wepayu.exception;

public class NaoHaComandoADesfazerException extends Exception {

    public NaoHaComandoADesfazerException() {
        super("Nao ha comando a desfazer.");
    }
}
