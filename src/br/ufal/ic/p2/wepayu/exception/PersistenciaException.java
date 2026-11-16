package br.ufal.ic.p2.wepayu.Exception;

public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }
}