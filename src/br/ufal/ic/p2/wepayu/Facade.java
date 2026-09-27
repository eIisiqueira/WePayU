package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.SalarioInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.ComissaoInvalidaException;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.EmpregadoAssalariado;
import br.ufal.ic.p2.wepayu.models.EmpregadoHorista;
import br.ufal.ic.p2.wepayu.models.EmpregadoComissionado;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoEmpregadoInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.AtributoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNomeNaoExisteException;

import java.math.BigDecimal;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoHoristaException;
import br.ufal.ic.p2.wepayu.Exception.DataInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.HorasInvalidasException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoComissionadoException;
import br.ufal.ic.p2.wepayu.Exception.ValorVendaInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoMembroInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.MembroNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoSindicalizadoException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoSindicatoDuplicadaException;
import br.ufal.ic.p2.wepayu.Exception.ValorTaxaServicoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoRecebeEmBancoException;
import br.ufal.ic.p2.wepayu.Exception.ValorSindicalizadoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.IdentificacaoSindicatoInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.TaxaSindicalInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.MetodoPagamentoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.BancoInvalidoException;
import br.ufal.ic.p2.wepayu.Exception.AgenciaInvalidaException;
import br.ufal.ic.p2.wepayu.Exception.ContaCorrenteInvalidaException;

/**
 * Ponto de entrada utilizado pelos testes de aceitação para acessar
 * as operações do sistema de folha de pagamento.
 *
 * Adapta e valida parâmetros recebidos externamente e delega as
 * operações de negócio ao {@link SistemaFolha} e aos objetos de domínio.
 */

public class Facade {

    private final SistemaFolha sistema;

    public Facade() {
        this.sistema = new SistemaFolha();
    }

    public void zerarSistema() {
        sistema.zerar();
    }

    public void encerrarSistema() {
        sistema.salvar();
    }

    /**
     * Calcula o total bruto da folha para a data informada,
     * considerando apenas os empregados que devem receber nessa data.
     *
     * @param data data da folha
     * @return total bruto no formato monetário esperado pela interface
     * @throws DataInvalidaException se a data informada for inválida
     */
    public String totalFolha(String data)
            throws DataInvalidaException {

        return Empregado.formatarValor(
                sistema.totalFolha(data)
        );
    }

    /**
     * Processa a folha da data informada e gera o relatório
     * no arquivo de saída indicado.
     *
     * @param data data da folha
     * @param saida caminho do arquivo de saída
     * @throws DataInvalidaException se a data informada for inválida
     */
    public void rodaFolha(String data, String saida)
            throws DataInvalidaException {

        sistema.rodaFolha(data, saida);
    }

    /**
     * Cria e registra um empregado horista ou assalariado no sistema.
     *
     * Os dados recebidos são validados antes da criação. O identificador
     * do empregado é gerado automaticamente pelo sistema.
     *
     * @param nome nome do empregado
     * @param endereco endereço do empregado
     * @param tipo tipo do empregado, sendo aceitos nesta sobrecarga
     *             "horista" ou "assalariado"
     * @param salario salário do empregado no formato aceito pela aplicação
     * @return identificador gerado para o novo empregado
     * @throws EmpregadoInvalidoException se nome, endereço ou tipo forem inválidos,
     *                                    ou se esta sobrecarga for usada para
     *                                    criar um empregado comissionado
     * @throws SalarioInvalidoException se o salário informado for inválido
     */
    public String criarEmpregado(
            String nome,
            String endereco,
            String tipo,
            String salario
    ) throws EmpregadoInvalidoException, SalarioInvalidoException {

        validarNome(nome);
        validarEndereco(endereco);
        validarTipo(tipo);

        if (tipo.equals("comissionado")) {
            throw new EmpregadoInvalidoException(
                    "Tipo nao aplicavel."
            );
        }

        BigDecimal valorSalario = validarSalario(salario);

        String id = sistema.gerarId();

        Empregado empregado;

        switch (tipo) {
            case "horista":
                empregado = new EmpregadoHorista(
                        id, nome, endereco, valorSalario
                );
                break;

            case "assalariado":
                empregado = new EmpregadoAssalariado(
                        id, nome, endereco, valorSalario
                );
                break;

            default:
                throw new EmpregadoInvalidoException(
                        "Tipo invalido."
                );
        }

        sistema.adicionarEmpregado(empregado);

        return id;
    }

    /**
     * Cria e registra um empregado comissionado no sistema.
     *
     * Valida os dados recebidos, converte salário e comissão para os tipos
     * utilizados internamente e gera automaticamente o identificador
     * do novo empregado.
     *
     * @param nome nome do empregado
     * @param endereco endereço do empregado
     * @param tipo tipo do empregado, que deve ser "comissionado"
     * @param salario salário base do empregado
     * @param comissao taxa de comissão associada ao empregado
     * @return identificador gerado para o novo empregado
     * @throws EmpregadoInvalidoException se nome, endereço ou tipo forem inválidos,
     *                                    ou se o tipo não for comissionado
     * @throws SalarioInvalidoException se o salário informado for inválido
     * @throws ComissaoInvalidaException se a comissão informada for inválida
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

        validarNome(nome);
        validarEndereco(endereco);
        validarTipo(tipo);

        if (!tipo.equals("comissionado")) {
            throw new EmpregadoInvalidoException(
                    "Tipo nao aplicavel."
            );
        }

        BigDecimal valorSalario = validarSalario(salario);
        BigDecimal valorComissao = validarComissao(comissao);

        String id = sistema.gerarId();

        Empregado empregado = new EmpregadoComissionado(
                id,
                nome,
                endereco,
                valorSalario,
                valorComissao
        );

        sistema.adicionarEmpregado(empregado);

        return id;
    }

    /**
     * Consulta um atributo de um empregado identificado pelo seu ID.
     *
     * A obtenção do valor é delegada ao próprio objeto empregado, permitindo
     * que subclasses disponibilizem atributos específicos.
     *
     * @param emp identificador do empregado
     * @param atributo nome do atributo a ser consultado
     * @return representação textual do valor do atributo
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se não houver empregado com o identificador informado
     * @throws AtributoNaoExisteException se o atributo solicitado não for reconhecido
     * @throws EmpregadoNaoComissionadoException se a comissão for consultada em empregado não comissionado
     * @throws EmpregadoNaoSindicalizadoException se dados sindicais forem consultados em empregado não sindicalizado
     * @throws EmpregadoNaoRecebeEmBancoException se dados bancários forem consultados em empregado que não recebe por banco
     */
    public String getAtributoEmpregado(
            String emp,
            String atributo
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException,
            EmpregadoNaoComissionadoException,
            EmpregadoNaoSindicalizadoException,
            EmpregadoNaoRecebeEmBancoException {

        if (emp == null || emp.isEmpty()) {
            throw new IdentificacaoEmpregadoInvalidaException();
        }

        Empregado empregado = sistema.buscarEmpregado(emp);

        if (empregado == null) {
            throw new EmpregadoNaoExisteException();
        }

        return empregado.getAtributo(atributo);
    }

    /**
     * Remove do sistema o empregado correspondente ao identificador informado.
     *
     * @param emp identificador do empregado a ser removido
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se não houver empregado com o identificador informado
     */
    public void removerEmpregado(String emp)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        if (emp == null || emp.isEmpty()) {
            throw new IdentificacaoEmpregadoInvalidaException();
        }

        sistema.removerEmpregado(emp);
    }

    /**
     * Localiza um empregado a partir do nome e da posição entre as
     * correspondências encontradas.
     *
     * A busca propriamente dita é delegada ao {@link SistemaFolha}.
     *
     * @param nome trecho de nome utilizado na busca
     * @param indice posição da correspondência desejada, começando em 1
     * @return identificador do empregado correspondente
     * @throws EmpregadoNomeNaoExisteException se não existir uma correspondência
     *                                         para a posição solicitada
     */
    public String getEmpregadoPorNome(
            String nome,
            int indice
    ) throws EmpregadoNomeNaoExisteException {

        return sistema.buscarEmpregadoPorNome(nome, indice);
    }

    /**
     * Altera um atributo de um empregado utilizando a forma simples
     * do comando de alteração.
     *
     * Esta sobrecarga atende alterações que utilizam apenas o identificador
     * do empregado, o atributo e um novo valor.
     *
     * @param emp identificador do empregado na folha
     * @param atributo atributo a ser alterado
     * @param valor novo valor do atributo
     */
    public void alteraEmpregado(
            String emp,
            String atributo,
            String valor
    ) throws IdentificacaoEmpregadoInvalidaException,
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
                validarNome(valor);
                sistema.alterarNome(emp, valor);
                return;

            case "endereco":
                validarEndereco(valor);
                sistema.alterarEndereco(emp, valor);
                return;

            case "tipo":
                validarTipo(valor);

                if (!"assalariado".equals(valor)) {
                    throw new EmpregadoInvalidoException(
                            "Tipo invalido."
                    );
                }

                sistema.alterarTipoAssalariado(emp);
                return;

            case "salario":
                BigDecimal salario = validarSalario(valor);
                sistema.alterarSalario(emp, salario);
                return;

            case "comissao":
                BigDecimal comissao = validarComissao(valor);
                sistema.alterarComissao(emp, comissao);
                return;

            case "metodoPagamento":
                sistema.alterarMetodoPagamento(emp, valor);
                return;

            case "sindicalizado":
                boolean sindicalizado = validarSindicalizado(valor);

                sistema.alteraEmpregadoSindicalizado(
                        emp,
                        sindicalizado,
                        null,
                        null
                );
                return;

            default:
                throw new AtributoNaoExisteException();
        }
    }

    /**
     * Altera o tipo do empregado quando a nova categoria exige
     * um valor adicional.
     *
     * Nesta sobrecarga, o valor adicional representa a comissão para
     * empregados comissionados ou o salário para empregados horistas.
     *
     * @param emp identificador do empregado
     * @param atributo atributo a ser alterado
     * @param valor novo tipo do empregado
     * @param valorExtra valor adicional exigido pelo novo tipo
     */
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

        switch (valor) {

            case "comissionado":
                BigDecimal comissao = validarComissao(valorExtra);

                sistema.alterarTipoComissionado(
                        emp,
                        comissao
                );
                return;

            case "horista":
                BigDecimal salario = validarSalario(valorExtra);

                sistema.alterarTipoHorista(
                        emp,
                        salario
                );
                return;

            default:
                throw new EmpregadoInvalidoException(
                        "Tipo invalido."
                );
        }
    }

    /**
     * Configura a sindicalização de um empregado com identificação e taxa sindical.
     *
     * @param emp identificador do empregado na folha
     * @param atributo atributo a ser alterado
     * @param valor novo valor do atributo
     * @param idSindicato identificação do empregado no sindicato
     * @param taxaSindical taxa sindical do empregado
     */
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

        boolean sindicalizado = validarSindicalizado(valor);

        sistema.alteraEmpregadoSindicalizado(
                emp,
                sindicalizado,
                idSindicato,
                taxaSindical
        );
    }

    /**
     * Configura o método de pagamento do empregado como depósito bancário,
     * utilizando os dados da conta informados.
     *
     * @param emp identificador do empregado
     * @param atributo atributo a ser alterado
     * @param valor1 método de pagamento, que deve ser "banco"
     * @param banco nome do banco
     * @param agencia agência bancária
     * @param contaCorrente conta corrente
     */
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

        sistema.alterarMetodoPagamentoBanco(
                emp,
                banco,
                agencia,
                contaCorrente
        );
    }

    /**
     * Lança uma taxa de serviço para um membro identificado pelo ID sindical.
     *
     * @param membro identificação do empregado no sindicato
     * @param data data da taxa de serviço
     * @param valor valor da taxa de serviço
     */
    public void lancaTaxaServico(
            String membro,
            String data,
            String valor
    ) throws IdentificacaoMembroInvalidaException,
            MembroNaoExisteException,
            DataInvalidaException,
            ValorTaxaServicoInvalidoException {

        sistema.lancaTaxaServico(
                membro,
                data,
                valor
        );
    }

    /**
     * Consulta a soma das taxas de serviço de um empregado no período informado.
     *
     * @param emp identificador do empregado na folha
     * @param dataInicial início inclusivo do período
     * @param dataFinal fim exclusivo do período
     * @return total das taxas formatado como valor monetário
     */
    public String getTaxasServico(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoSindicalizadoException,
            DataInvalidaException {

        return Empregado.formatarValor(
                sistema.getTaxasServico(
                        emp,
                        dataInicial,
                        dataFinal
                )
        );
    }

    /**
     * Registra um resultado de venda para o empregado informado,
     * delegando a operação ao sistema de folha.
     *
     * @param emp identificador do empregado
     * @param data data da venda
     * @param valor valor da venda
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for inválido
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoComissionadoException se o empregado não for comissionado
     * @throws DataInvalidaException se a data for inválida
     * @throws ValorVendaInvalidoException se o valor da venda for inválido
     */
    public void lancaVenda(
            String emp,
            String data,
            String valor
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException,
            ValorVendaInvalidoException {

        sistema.lancaVenda(
                emp,
                data,
                valor
        );
    }

    /**
     * Consulta o valor total das vendas do empregado no período informado.
     *
     * @param emp identificador do empregado
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @return total das vendas no formato monetário esperado pela interface
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for inválido
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoComissionadoException se o empregado não for comissionado
     * @throws DataInvalidaException se as datas ou o período forem inválidos
     */
    public String getVendasRealizadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoComissionadoException,
            DataInvalidaException {

        return Empregado.formatarValor(
                sistema.getVendasRealizadas(
                        emp,
                        dataInicial,
                        dataFinal
                )
        );
    }

    /**
     * Registra um cartão de ponto para o empregado informado.
     *
     * A validação do identificador, da data e da quantidade de horas,
     * bem como a aplicação da operação ao tipo correto de empregado,
     * é delegada ao sistema.
     *
     * @param emp identificador do empregado
     * @param data data do cartão de ponto
     * @param horas quantidade de horas trabalhadas
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não aceitar cartões de ponto
     * @throws DataInvalidaException se a data informada for inválida
     * @throws HorasInvalidasException se a quantidade de horas não for positiva ou válida
     */
    public void lancaCartao(
            String emp,
            String data,
            String horas
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException,
            HorasInvalidasException {

        sistema.lancaCartao(
                emp,
                data,
                horas
        );
    }

    /**
     * Consulta as horas normais trabalhadas por um empregado horista
     * no intervalo informado.
     *
     * O valor calculado pelo sistema é convertido para o formato textual
     * esperado pela interface de testes.
     *
     * @param emp identificador do empregado
     * @param dataInicial data inicial do intervalo
     * @param dataFinal data final do intervalo
     * @return total de horas normais formatado como texto
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não for horista
     * @throws DataInvalidaException se alguma data ou o intervalo forem inválidos
     */
    public String getHorasNormaisTrabalhadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        return formatarHoras(
                sistema.getHorasNormaisTrabalhadas(
                        emp,
                        dataInicial,
                        dataFinal
                )
        );
    }

    /**
     * Consulta as horas extras trabalhadas por um empregado horista
     * no intervalo informado.
     *
     * O valor calculado pelo sistema é convertido para o formato textual
     * esperado pela interface de testes.
     *
     * @param emp identificador do empregado
     * @param dataInicial data inicial do intervalo
     * @param dataFinal data final do intervalo
     * @return total de horas extras formatado como texto
     * @throws IdentificacaoEmpregadoInvalidaException se o identificador for nulo ou vazio
     * @throws EmpregadoNaoExisteException se o empregado não existir
     * @throws EmpregadoNaoHoristaException se o empregado não for horista
     * @throws DataInvalidaException se alguma data ou o intervalo forem inválidos
     */
    public String getHorasExtrasTrabalhadas(
            String emp,
            String dataInicial,
            String dataFinal
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            EmpregadoNaoHoristaException,
            DataInvalidaException {

        return formatarHoras(
                sistema.getHorasExtrasTrabalhadas(
                        emp,
                        dataInicial,
                        dataFinal
                )
        );
    }

    private String formatarHoras(BigDecimal horas) {
        return horas
                .stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }

    private boolean validarSindicalizado(String valor)
            throws ValorSindicalizadoInvalidoException {

        if (!"true".equals(valor) && !"false".equals(valor)) {
            throw new ValorSindicalizadoInvalidoException();
        }

        return Boolean.parseBoolean(valor);
    }

    private void validarNome(String nome)
            throws EmpregadoInvalidoException {

        if (nome == null || nome.isEmpty()) {
            throw new EmpregadoInvalidoException(
                    "Nome nao pode ser nulo."
            );
        }
    }

    private void validarEndereco(String endereco)
            throws EmpregadoInvalidoException {

        if (endereco == null || endereco.isEmpty()) {
            throw new EmpregadoInvalidoException(
                    "Endereco nao pode ser nulo."
            );
        }
    }

    private void validarTipo(String tipo)
            throws EmpregadoInvalidoException {

        if (tipo == null ||
                (!tipo.equals("horista")
                        && !tipo.equals("assalariado")
                        && !tipo.equals("comissionado"))) {

            throw new EmpregadoInvalidoException(
                    "Tipo invalido."
            );
        }
    }

    private BigDecimal validarSalario(String salario)
            throws SalarioInvalidoException {

        if (salario == null || salario.isEmpty()) {
            throw new SalarioInvalidoException(
                    "Salario nao pode ser nulo."
            );
        }

        BigDecimal valor;

        try {
            valor = new BigDecimal(
                    salario.replace(",", ".")
            );
        } catch (NumberFormatException e) {
            throw new SalarioInvalidoException(
                    "Salario deve ser numerico."
            );
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new SalarioInvalidoException(
                    "Salario deve ser nao-negativo."
            );
        }

        return valor;
    }

    private BigDecimal validarComissao(String comissao)
            throws ComissaoInvalidaException {

        if (comissao == null || comissao.isEmpty()) {
            throw new ComissaoInvalidaException(
                    "Comissao nao pode ser nula."
            );
        }

        BigDecimal valor;

        try {
            valor = new BigDecimal(
                    comissao.replace(",", ".")
            );
        } catch (NumberFormatException e) {
            throw new ComissaoInvalidaException(
                    "Comissao deve ser numerica."
            );
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ComissaoInvalidaException(
                    "Comissao deve ser nao-negativa."
            );
        }

        return valor;
    }
}
