package br.ufal.ic.p2.wepayu.exception;

public class HorasInvalidasException extends Exception {

    public HorasInvalidasException() {
        super("Horas devem ser positivas.");
    }
}
