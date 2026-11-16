package br.ufal.ic.p2.wepayu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.function.Supplier;

import br.ufal.ic.p2.wepayu.exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.DataInvalidaException;
import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.exception.MetodoPagamentoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.exception.TaxaSindicalInvalidaException;
import br.ufal.ic.p2.wepayu.exception.ValorSindicalizadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorTaxaServicoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;
import br.ufal.ic.p2.wepayu.models.MetodoPagamento;
import br.ufal.ic.p2.wepayu.models.PagamentoCorreios;
import br.ufal.ic.p2.wepayu.models.PagamentoEmMaos;

/**
 * Interpreta os textos recebidos pela {@link Facade} e os converte para
 * os tipos usados pelo domínio.
 *
 * Aqui só se verifica se o texto pode ser interpretado (formato de número,
 * data ou token). Regras de negócio, como valores positivos ou
 * não-negativos, são verificadas pelos próprios objetos de domínio.
 * Um texto vazio é convertido em {@code null}, que o domínio rejeita
 * com a mensagem de "nao pode ser nulo".
 */
final class ConversorEntrada {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("d/M/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private ConversorEntrada() {
    }

    static LocalDate data(String texto) throws DataInvalidaException {
        return data(texto, "Data invalida.");
    }

    static LocalDate dataInicial(String texto) throws DataInvalidaException {
        return data(texto, "Data inicial invalida.");
    }

    static LocalDate dataFinal(String texto) throws DataInvalidaException {
        return data(texto, "Data final invalida.");
    }

    static BigDecimal salario(String texto) throws SalarioInvalidoException {
        return numero(texto,
                () -> new SalarioInvalidoException("Salario deve ser numerico."));
    }

    static BigDecimal comissao(String texto) throws ComissaoInvalidaException {
        return numero(texto,
                () -> new ComissaoInvalidaException("Comissao deve ser numerica."));
    }

    static BigDecimal taxaSindical(String texto)
            throws TaxaSindicalInvalidaException {

        return numero(texto, () -> new TaxaSindicalInvalidaException(
                "Taxa sindical deve ser numerica."));
    }

    static BigDecimal horas(String texto) throws HorasInvalidasException {
        return numero(texto, HorasInvalidasException::new);
    }

    static BigDecimal valorVenda(String texto)
            throws ValorVendaInvalidoException {

        return numero(texto, ValorVendaInvalidoException::new);
    }

    static BigDecimal valorTaxaServico(String texto)
            throws ValorTaxaServicoInvalidoException {

        return numero(texto, ValorTaxaServicoInvalidoException::new);
    }

    static boolean sindicalizado(String texto)
            throws ValorSindicalizadoInvalidoException {

        if (!"true".equals(texto) && !"false".equals(texto)) {
            throw new ValorSindicalizadoInvalidoException();
        }

        return Boolean.parseBoolean(texto);
    }

    /**
     * Converte os tokens de método de pagamento que não exigem dados
     * adicionais. O depósito bancário usa a forma com banco, agência e conta.
     */
    static MetodoPagamento metodoPagamento(String texto)
            throws MetodoPagamentoInvalidoException {

        if ("emMaos".equals(texto)) {
            return new PagamentoEmMaos();
        }

        if ("correios".equals(texto)) {
            return new PagamentoCorreios();
        }

        throw new MetodoPagamentoInvalidoException();
    }

    private static LocalDate data(String texto, String mensagem)
            throws DataInvalidaException {

        try {
            return LocalDate.parse(texto, FORMATO_DATA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DataInvalidaException(mensagem);
        }
    }

    private static <E extends Exception> BigDecimal numero(
            String texto,
            Supplier<E> erroFormato
    ) throws E {

        if (texto == null || texto.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            throw erroFormato.get();
        }
    }
}
