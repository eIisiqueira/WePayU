package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;

import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoRecebeEmBancoException;

/**
 * Forma pela qual o empregado recebe o pagamento. Cada subclasse define
 * seu identificador e sua descrição; apenas o depósito bancário possui
 * dados de conta.
 */
public abstract class MetodoPagamento implements Serializable {

    /**
     * @return identificador do método (emMaos, correios ou banco)
     */
    public abstract String getTipo();

    /**
     * @param endereco endereço do empregado, usado no envio pelos correios
     * @return descrição do método exibida na folha de pagamento
     */
    public abstract String descricao(String endereco);

    public String getBanco() throws EmpregadoNaoRecebeEmBancoException {
        throw new EmpregadoNaoRecebeEmBancoException();
    }

    public String getAgencia() throws EmpregadoNaoRecebeEmBancoException {
        throw new EmpregadoNaoRecebeEmBancoException();
    }

    public String getContaCorrente()
            throws EmpregadoNaoRecebeEmBancoException {

        throw new EmpregadoNaoRecebeEmBancoException();
    }
}
