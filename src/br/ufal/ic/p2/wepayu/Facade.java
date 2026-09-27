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

    public void removerEmpregado(String emp)
            throws IdentificacaoEmpregadoInvalidaException,
            EmpregadoNaoExisteException {

        if (emp == null || emp.isEmpty()) {
            throw new IdentificacaoEmpregadoInvalidaException();
        }

        sistema.removerEmpregado(emp);
    }

    public String getEmpregadoPorNome(
            String nome,
            int indice
    ) throws EmpregadoNomeNaoExisteException {

        return sistema.buscarEmpregadoPorNome(nome, indice);
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
