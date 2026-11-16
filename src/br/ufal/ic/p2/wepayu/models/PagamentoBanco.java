package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.exception.AgenciaInvalidaException;
import br.ufal.ic.p2.wepayu.exception.BancoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ContaCorrenteInvalidaException;

/**
 * Depósito em conta bancária. Só pode ser criado com banco, agência
 * e conta corrente preenchidos.
 */
public class PagamentoBanco extends MetodoPagamento {

    private final String banco;
    private final String agencia;
    private final String contaCorrente;

    public PagamentoBanco(String banco, String agencia, String contaCorrente)
            throws BancoInvalidoException,
            AgenciaInvalidaException,
            ContaCorrenteInvalidaException {

        if (banco == null || banco.isEmpty()) {
            throw new BancoInvalidoException();
        }

        if (agencia == null || agencia.isEmpty()) {
            throw new AgenciaInvalidaException();
        }

        if (contaCorrente == null || contaCorrente.isEmpty()) {
            throw new ContaCorrenteInvalidaException();
        }

        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    @Override
    public String getTipo() {
        return "banco";
    }

    @Override
    public String descricao(String endereco) {
        return banco + ", Ag. " + agencia + " CC " + contaCorrente;
    }

    @Override
    public String getBanco() {
        return banco;
    }

    @Override
    public String getAgencia() {
        return agencia;
    }

    @Override
    public String getContaCorrente() {
        return contaCorrente;
    }
}
