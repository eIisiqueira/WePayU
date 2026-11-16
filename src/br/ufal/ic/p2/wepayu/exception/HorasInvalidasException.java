package br.ufal.ic.p2.wepayu.Exception;

public class HorasInvalidasException extends Exception {

    public HorasInvalidasException() {
        super("Horas devem ser positivas.");
    }
}