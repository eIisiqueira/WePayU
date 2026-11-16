package br.ufal.ic.p2.wepayu;

import java.math.BigDecimal;

import br.ufal.ic.p2.wepayu.exception.AgenciaInvalidaException;
import br.ufal.ic.p2.wepayu.exception.AtributoNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.BancoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ComandoAposEncerrarSistemaException;
import br.ufal.ic.p2.wepayu.exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.ContaCorrenteInvalidaException;
import br.ufal.ic.p2.wepayu.exception.DataInvalidaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoRecebeEmBancoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.exception.EmpregadoNomeNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoEmpregadoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoMembroInvalidaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoSindicatoDuplicadaException;
import br.ufal.ic.p2.wepayu.exception.IdentificacaoSindicatoInvalidaException;
import br.ufal.ic.p2.wepayu.exception.MembroNaoExisteException;
import br.ufal.ic.p2.wepayu.exception.MetodoPagamentoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.NaoHaComandoADesfazerException;
import br.ufal.ic.p2.wepayu.exception.NaoHaComandoARefazerException;
import br.ufal.ic.p2.wepayu.exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.exception.TaxaSindicalInvalidaException;
import br.ufal.ic.p2.wepayu.exception.ValorSindicalizadoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorTaxaServicoInvalidoException;
import br.ufal.ic.p2.wepayu.exception.ValorVendaInvalidoException;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.EmpregadoAssalariado;
import br.ufal.ic.p2.wepayu.models.EmpregadoComissionado;
import br.ufal.ic.p2.wepayu.models.EmpregadoHorista;
import br.ufal.ic.p2.wepayu.models.PagamentoBanco;

/**
 * Ponto de entrada utilizado pelos testes de aceitação (EasyAccept).
 *
 * Apenas interpreta os parâmetros textuais (via {@link ConversorEntrada}),
 * delega a operação ao {@link SistemaFolha} e formata o retorno
 * (via {@link Formatador}). Regras de negócio, validações de domínio
 * e controle de undo/redo ficam fora desta classe.
 */
public class Facade {

    private final SistemaFolha sistema = new SistemaFolha();

    public int getNumeroDeEmpregados() {
        return sistema.getNumeroDeEmpregados();
    }

    public void undo() throws ComandoAposEncerrarSistemaException,
            NaoHaComandoADesfazerException {

        sistema.undo();
    }

    public void redo() throws ComandoAposEncerrarSistemaException,
            NaoHaComandoARefazerException {

        sistema.redo();
    }

    /**
     * Reinicia o estado do sistema. A operação pode ser desfeita.
     */
    public void zerarSistema() {
        sistema.zerar();
    }

    public void encerrarSistema() {
        sistema.encerrar();
    }

    public String totalFolha(String data) throws DataInvalidaException {
        return Formatador.valor(
                sistema.totalFolha(ConversorEntrada.data(data))
        );
    }

    public void rodaFolha(String data, String saida)
            throws DataInvalidaException {

        sistema.rodaFolha(ConversorEntrada.data(data), saida);
    }

    /**
     * Cria um empregado horista ou assalariado.
     *
     * @param nome nome do empregado
     * @param endereco endereço do empregado
     * @param tipo "horista" ou "assalariado"
     * @param salario salário no formato textual dos testes
     * @return identificador gerado para o novo empregado
     * @throws EmpregadoInvalidoException se o tipo for inválido ou não aplicável
     *                                    a esta forma, ou se nome/endereço forem vazios
     * @throws SalarioInvalidoException se o salário for vazio, não numérico ou negativo
     */
    public String criarEmpregado(
            String nome,
            String endereco,
            String tipo,
            String salario
    ) throws EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        validarTipo(tipo);

        if ("comissionado".equals(tipo)) {
            throw new EmpregadoInvalidoException("Tipo nao aplicavel.");
        }

        BigDecimal valorSalario = ConversorEntrada.salario(salario);

        if ("horista".equals(tipo)) {
            return sistema.cadastrar(id ->
                    new EmpregadoHorista(id, nome, endereco, valorSalario));
        }

        return sistema.cadastrar(id ->
                new EmpregadoAssalariado(id, nome, endereco, valorSalario));
    }

    /**
     * Cria um empregado comissionado.
     *
     * @param nome nome do empregado
     * @param endereco endereço do empregado
     * @param tipo deve ser "comissionado"
     * @param salario salário base no formato textual dos testes
     * @param comissao taxa de comissão no formato textual dos testes
     * @return identificador gerado para o novo empregado
     * @throws EmpregadoInvalidoException se o tipo for inválido ou não aplicável
     *                                    a esta forma, ou se nome/endereço forem vazios
     * @throws SalarioInvalidoException se o salário for inválido
     * @throws ComissaoInvalidaException se a comissão for inválida
     */
    public String criarEmpregado(
            String nome,
            String endereco,
            String tipo,
            String salario,
            String comissao
    ) throws EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        validarTipo(tipo);

        if (!"comissionado".equals(tipo)) {
            throw new EmpregadoInvalidoException("Tipo nao aplicavel.");
        }

        BigDecimal valorSalario = ConversorEntrada.salario(salario);
        BigDecimal valorComissao = ConversorEntrada.comissao(comissao);

        return sistema.cadastrar(id -> new EmpregadoComissionado(
                id, nome, endereco, valorSalario, valorComissao));
    }

    /**
     * Consulta um atributo de um empregado, no formato textual esperado
     * pelos testes. Atributos que dependem do tipo, do sindicato ou do
     * método de pagamento são resolvidos polimorficamente pelos objetos
     * de domínio, que rejeitam a consulta quando ela não se aplica.
     *
     * @param emp identificador do empregado
     * @param atributo nome do atributo
     * @return valor do atributo formatado
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws AtributoNaoExisteException se o atributo não for reconhecido
     * @throws EmpregadoNaoComissionadoException se pedir comissão de não comissionado
     * @throws EmpregadoNaoSindicalizadoException se pedir dado sindical de não sindicalizado
     * @throws EmpregadoNaoRecebeEmBancoException se pedir dado bancário de quem não recebe em banco
     */
    public String getAtributoEmpregado(String emp, String atributo)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            EmpregadoNaoComissionadoException,
            EmpregadoNaoSindicalizadoException,
            EmpregadoNaoRecebeEmBancoException {

        Empregado empregado = sistema.buscarEmpregado(emp);

        switch (atributo) {
            case "nome":
                return empregado.getNome();
            case "endereco":
                return empregado.getEndereco();
            case "tipo":
                return empregado.getTipo();
            case "salario":
                return Formatador.valor(empregado.getSalario());
            case "comissao":
                return Formatador.valor(empregado.getComissao());
            case "metodoPagamento":
                return empregado.getMetodoPagamento().getTipo();
            case "banco":
                return empregado.getMetodoPagamento().getBanco();
            case "agencia":
                return empregado.getMetodoPagamento().getAgencia();
            case "contaCorrente":
                return empregado.getMetodoPagamento().getContaCorrente();
            case "sindicalizado":
                return String.valueOf(empregado.isSindicalizado());
            case "idSindicato":
                return empregado.getMembroSindicato().getIdentificacao();
            case "taxaSindical":
                return Formatador.valor(
                        empregado.getMembroSindicato().getTaxaSindical());
            default:
                throw new AtributoNaoExisteException();
        }
    }

    public void removerEmpregado(String emp)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        sistema.removerEmpregado(emp);
    }

    /**
     * Localiza o identificador da n-ésima pessoa cujo nome contém o texto.
     *
     * @param nome trecho de nome utilizado na busca
     * @param indice posição da correspondência desejada, começando em 1
     * @return identificador do empregado correspondente
     * @throws EmpregadoNomeNaoExisteException se não houver correspondência
     */
    public String getEmpregadoPorNome(String nome, int indice)
            throws EmpregadoNomeNaoExisteException {

        return sistema.buscarEmpregadoPorNome(nome, indice);
    }

    public void alteraEmpregado(String emp, String atributo, String valor)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            IdentificacaoSindicatoDuplicadaException,
            EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException,
            EmpregadoNaoComissionadoException,
            ValorSindicalizadoInvalidoException,
            IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException,
            MetodoPagamentoInvalidoException {

        switch (atributo) {
            case "nome":
                sistema.alterarNome(emp, valor);
                break;
            case "endereco":
                sistema.alterarEndereco(emp, valor);
                break;
            case "tipo":
                validarTipo(valor);
                if (!"assalariado".equals(valor)) {
                    throw new EmpregadoInvalidoException("Tipo invalido.");
                }
                sistema.alterarTipo(emp, EmpregadoAssalariado::new);
                break;
            case "salario":
                sistema.alterarSalario(emp, ConversorEntrada.salario(valor));
                break;
            case "comissao":
                sistema.alterarComissao(emp, ConversorEntrada.comissao(valor));
                break;
            case "metodoPagamento":
                sistema.alterarMetodoPagamento(
                        emp, ConversorEntrada.metodoPagamento(valor));
                break;
            case "sindicalizado":
                alterarSindicalizacao(emp, valor, null, null);
                break;
            default:
                throw new AtributoNaoExisteException();
        }
    }

    public void alteraEmpregado(
            String emp,
            String atributo,
            String valor,
            String valorExtra
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            EmpregadoInvalidoException,
            SalarioInvalidoException,
            ComissaoInvalidaException {

        if (!"tipo".equals(atributo)) {
            throw new AtributoNaoExisteException();
        }

        validarTipo(valor);

        if ("comissionado".equals(valor)) {
            BigDecimal comissao = ConversorEntrada.comissao(valorExtra);
            sistema.alterarTipo(emp,
                    atual -> new EmpregadoComissionado(atual, comissao));

        } else if ("horista".equals(valor)) {
            BigDecimal salario = ConversorEntrada.salario(valorExtra);
            sistema.alterarTipo(emp,
                    atual -> new EmpregadoHorista(atual, salario));

        } else {
            throw new EmpregadoInvalidoException("Tipo invalido.");
        }
    }

    public void alteraEmpregado(
            String emp,
            String atributo,
            String valor,
            String idSindicato,
            String taxaSindical
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            IdentificacaoSindicatoDuplicadaException,
            ValorSindicalizadoInvalidoException,
            IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException {

        if (!"sindicalizado".equals(atributo)) {
            throw new AtributoNaoExisteException();
        }

        alterarSindicalizacao(emp, valor, idSindicato, taxaSindical);
    }

    public void alteraEmpregado(
            String emp,
            String atributo,
            String valor1,
            String banco,
            String agencia,
            String contaCorrente
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            MetodoPagamentoInvalidoException,
            BancoInvalidoException,
            AgenciaInvalidaException,
            ContaCorrenteInvalidaException {

        if (!"metodoPagamento".equals(atributo)) {
            throw new AtributoNaoExisteException();
        }

        if (!"banco".equals(valor1)) {
            throw new MetodoPagamentoInvalidoException();
        }

        sistema.alterarMetodoPagamento(
                emp, new PagamentoBanco(banco, agencia, contaCorrente));
    }

    public void lancaTaxaServico(String membro, String data, String valor)
            throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException,
            DataInvalidaException,
            ValorTaxaServicoInvalidoException {

        sistema.lancaTaxaServico(
                membro,
                ConversorEntrada.data(data),
                ConversorEntrada.valorTaxaServico(valor)
        );
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoSindicalizadoException,
            DataInvalidaException {

        return Formatador.valor(sistema.getTaxasServico(
                emp,
                ConversorEntrada.dataInicial(dataInicial),
                ConversorEntrada.dataFinal(dataFinal)
        ));
    }

    public void lancaVenda(String emp, String data, String valor)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException,
            ValorVendaInvalidoException {

        sistema.lancaVenda(
                emp,
                ConversorEntrada.data(data),
                ConversorEntrada.valorVenda(valor)
        );
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException {

        return Formatador.valor(sistema.getVendasRealizadas(
                emp,
                ConversorEntrada.dataInicial(dataInicial),
                ConversorEntrada.dataFinal(dataFinal)
        ));
    }

    public void lancaCartao(String emp, String data, String horas)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException,
            HorasInvalidasException {

        sistema.lancaCartao(
                emp,
                ConversorEntrada.data(data),
                ConversorEntrada.horas(horas)
        );
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        return Formatador.numero(sistema.getHorasNormaisTrabalhadas(
                emp,
                ConversorEntrada.dataInicial(dataInicial),
                ConversorEntrada.dataFinal(dataFinal)
        ));
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        return Formatador.numero(sistema.getHorasExtrasTrabalhadas(
                emp,
                ConversorEntrada.dataInicial(dataInicial),
                ConversorEntrada.dataFinal(dataFinal)
        ));
    }

    private void alterarSindicalizacao(
            String emp,
            String valor,
            String idSindicato,
            String taxaSindical
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            IdentificacaoSindicatoDuplicadaException,
            ValorSindicalizadoInvalidoException,
            IdentificacaoSindicatoInvalidaException,
            TaxaSindicalInvalidaException {

        if (ConversorEntrada.sindicalizado(valor)) {
            sistema.sindicalizar(
                    emp, idSindicato, ConversorEntrada.taxaSindical(taxaSindical));
        } else {
            sistema.dessindicalizar(emp);
        }
    }

    /** Verifica se o token de tipo é um dos aceitos pela interface. */
    private static void validarTipo(String tipo)
            throws EmpregadoInvalidoException {

        if (!"horista".equals(tipo)
                && !"assalariado".equals(tipo)
                && !"comissionado".equals(tipo)) {

            throw new EmpregadoInvalidoException("Tipo invalido.");
        }
    }
}
