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
     */
    public String getAtributoEmpregado(
            String emp,
            String atributo
    ) throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException,
            AtributoNaoExisteException {

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
