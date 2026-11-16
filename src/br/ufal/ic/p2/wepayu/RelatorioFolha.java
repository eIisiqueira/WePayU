package br.ufal.ic.p2.wepayu;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.ufal.ic.p2.wepayu.exception.PersistenciaException;
import br.ufal.ic.p2.wepayu.models.Contracheque;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.SecoesFolha;

/**
 * Monta o texto da folha de pagamento de uma data a partir dos
 * contracheques calculados e o grava em arquivo.
 *
 * Concentra toda a formatação do relatório, que antes ficava espalhada
 * nas classes de empregado. Cada contracheque se registra na seção
 * correta por polimorfismo ({@link SecoesFolha}).
 */
class RelatorioFolha implements SecoesFolha {

    private static final String QUEBRA_LINHA = "\n";
    private static final int LARGURA = 127;
    private static final String SEPARADOR = "=".repeat(LARGURA);

    private final LocalDate data;
    private final List<Contracheque> todos;
    private final List<Contracheque> horistas = new ArrayList<>();
    private final List<Contracheque> assalariados = new ArrayList<>();
    private final List<Contracheque> comissionados = new ArrayList<>();

    RelatorioFolha(LocalDate data, List<Contracheque> contracheques) {
        this.data = data;
        this.todos = contracheques;

        for (Contracheque contracheque : contracheques) {
            contracheque.registrarEm(this);
        }
    }

    @Override
    public void adicionarHorista(Contracheque contracheque) {
        horistas.add(contracheque);
    }

    @Override
    public void adicionarAssalariado(Contracheque contracheque) {
        assalariados.add(contracheque);
    }

    @Override
    public void adicionarComissionado(Contracheque contracheque) {
        comissionados.add(contracheque);
    }

    void gravar(String caminho) {
        try {
            Files.write(
                    Paths.get(caminho),
                    gerar().getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException | RuntimeException e) {
            throw new PersistenciaException("Erro ao gerar folha.");
        }
    }

    String gerar() {
        StringBuilder relatorio = new StringBuilder();

        String cabecalho = "FOLHA DE PAGAMENTO DO DIA " + data;
        linha(relatorio, cabecalho);
        linha(relatorio, "=".repeat(cabecalho.length()));
        linha(relatorio, "");

        secaoHoristas(relatorio);
        secaoAssalariados(relatorio);
        secaoComissionados(relatorio);

        linha(relatorio, "TOTAL FOLHA: " + Formatador.valor(somaBruto(todos)));

        return relatorio.toString();
    }

    private void secaoHoristas(StringBuilder saida) {
        titulo(saida, "HORISTAS",
                "Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo",
                "==================================== ===== ===== ============= ========= =============== ======================================");

        BigDecimal horas = BigDecimal.ZERO;
        BigDecimal extras = BigDecimal.ZERO;

        for (Contracheque c : horistas) {
            linha(saida, String.format("%-36s %5s %5s %13s %9s %15s %s",
                    c.getEmpregado().getNome(),
                    Formatador.numero(c.getHorasNormais()),
                    Formatador.numero(c.getHorasExtras()),
                    Formatador.valor(c.getSalarioBruto()),
                    Formatador.valor(c.getDescontos()),
                    Formatador.valor(c.getSalarioLiquido()),
                    metodo(c)));

            horas = horas.add(c.getHorasNormais());
            extras = extras.add(c.getHorasExtras());
        }

        linha(saida, "");
        linha(saida, String.format("%-36s %5s %5s %13s %9s %15s",
                "TOTAL HORISTAS",
                Formatador.numero(horas),
                Formatador.numero(extras),
                Formatador.valor(somaBruto(horistas)),
                Formatador.valor(somaDescontos(horistas)),
                Formatador.valor(somaLiquido(horistas))));
        linha(saida, "");
    }

    private void secaoAssalariados(StringBuilder saida) {
        titulo(saida, "ASSALARIADOS",
                "Nome                                             Salario Bruto Descontos Salario Liquido Metodo",
                "================================================ ============= ========= =============== ======================================");

        for (Contracheque c : assalariados) {
            linha(saida, String.format("%-48s %13s %9s %15s %s",
                    c.getEmpregado().getNome(),
                    Formatador.valor(c.getSalarioBruto()),
                    Formatador.valor(c.getDescontos()),
                    Formatador.valor(c.getSalarioLiquido()),
                    metodo(c)));
        }

        linha(saida, "");
        linha(saida, String.format("%-48s %13s %9s %15s",
                "TOTAL ASSALARIADOS",
                Formatador.valor(somaBruto(assalariados)),
                Formatador.valor(somaDescontos(assalariados)),
                Formatador.valor(somaLiquido(assalariados))));
        linha(saida, "");
    }

    private void secaoComissionados(StringBuilder saida) {
        titulo(saida, "COMISSIONADOS",
                "Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo",
                "===================== ======== ======== ======== ============= ========= =============== ======================================");

        BigDecimal fixo = BigDecimal.ZERO;
        BigDecimal vendas = BigDecimal.ZERO;
        BigDecimal comissao = BigDecimal.ZERO;

        for (Contracheque c : comissionados) {
            linha(saida, String.format("%-21s %8s %8s %8s %13s %9s %15s %s",
                    c.getEmpregado().getNome(),
                    Formatador.valor(c.getFixo()),
                    Formatador.valor(c.getVendas()),
                    Formatador.valor(c.getComissao()),
                    Formatador.valor(c.getSalarioBruto()),
                    Formatador.valor(c.getDescontos()),
                    Formatador.valor(c.getSalarioLiquido()),
                    metodo(c)));

            fixo = fixo.add(c.getFixo());
            vendas = vendas.add(c.getVendas());
            comissao = comissao.add(c.getComissao());
        }

        linha(saida, "");
        linha(saida, String.format("%-21s %8s %8s %8s %13s %9s %15s",
                "TOTAL COMISSIONADOS",
                Formatador.valor(fixo),
                Formatador.valor(vendas),
                Formatador.valor(comissao),
                Formatador.valor(somaBruto(comissionados)),
                Formatador.valor(somaDescontos(comissionados)),
                Formatador.valor(somaLiquido(comissionados))));
        linha(saida, "");
    }

    private void titulo(
            StringBuilder saida,
            String nome,
            String colunas,
            String sublinhado
    ) {
        String inicio = "=".repeat(21) + " " + nome + " ";

        linha(saida, SEPARADOR);
        linha(saida, inicio + "=".repeat(LARGURA - inicio.length()));
        linha(saida, SEPARADOR);
        linha(saida, colunas);
        linha(saida, sublinhado);
    }

    private static String metodo(Contracheque contracheque) {
        Empregado empregado = contracheque.getEmpregado();

        return empregado.getMetodoPagamento()
                .descricao(empregado.getEndereco());
    }

    static BigDecimal somaBruto(List<Contracheque> contracheques) {
        BigDecimal total = BigDecimal.ZERO;

        for (Contracheque c : contracheques) {
            total = total.add(c.getSalarioBruto());
        }

        return total;
    }

    private static BigDecimal somaDescontos(List<Contracheque> contracheques) {
        BigDecimal total = BigDecimal.ZERO;

        for (Contracheque c : contracheques) {
            total = total.add(c.getDescontos());
        }

        return total;
    }

    private static BigDecimal somaLiquido(List<Contracheque> contracheques) {
        BigDecimal total = BigDecimal.ZERO;

        for (Contracheque c : contracheques) {
            total = total.add(c.getSalarioLiquido());
        }

        return total;
    }

    private static void linha(StringBuilder saida, String texto) {
        saida.append(texto).append(QUEBRA_LINHA);
    }
}
