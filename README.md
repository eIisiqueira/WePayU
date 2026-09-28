# WePayU

Projeto de folha de pagamento desenvolvido em Java para a disciplina de Programação 2.

Neste primeiro milestone foram implementadas as User Stories 1 a 8.

## Funcionalidades

- cadastro e remoção de empregados;
- empregados horistas, assalariados e comissionados;
- lançamento de cartão de ponto;
- lançamento de vendas;
- lançamento de taxas de serviço;
- alteração de dados do empregado;
- métodos de pagamento;
- geração da folha de pagamento;
- persistência dos dados;
- undo e redo.

## Estrutura principal

O projeto possui uma `Facade`, usada pelos testes do EasyAccept, e a classe `SistemaFolha`, responsável por coordenar as operações do sistema.

Os empregados são representados por uma classe abstrata `Empregado` e pelas subclasses:

- `EmpregadoHorista`
- `EmpregadoAssalariado`
- `EmpregadoComissionado`

Também existem classes para cartão de ponto, vendas, taxas de serviço e contracheque.

## Testes

Os testes estão na pasta `tests/` e usam o `easyaccept.jar`.

Para compilar no Linux/WSL:

```bash
mkdir -p out
javac -cp lib/easyaccept.jar -d out $(find src -name "*.java")
```

Exemplo para executar a US1:

```bash
java -cp "out:lib/easyaccept.jar" easyaccept.EasyAccept br.ufal.ic.p2.wepayu.Facade tests/us1.txt
```

Os testes do primeiro milestone são:

```text
us1.txt
us1_1.txt
us2.txt
us2_1.txt
us3.txt
us3_1.txt
us4.txt
us4_1.txt
us5.txt
us5_1.txt
us6.txt
us6_1.txt
us7.txt
us8.txt
```

## Persistência

Os dados do sistema são salvos em `wepayu.dat`.

## Observação

Este README se refere apenas ao primeiro milestone, que envolve as User Stories 1 a 8.
