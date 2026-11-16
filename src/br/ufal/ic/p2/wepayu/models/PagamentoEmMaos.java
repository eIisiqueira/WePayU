package br.ufal.ic.p2.wepayu.models;

public class PagamentoEmMaos extends MetodoPagamento {

    @Override
    public String getTipo() {
        return "emMaos";
    }

    @Override
    public String descricao(String endereco) {
        return "Em maos";
    }
}
