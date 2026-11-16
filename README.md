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

### Pacote `br.ufal.ic.p2.wepayu`

- `Facade`: interface usada pelos testes do EasyAccept. Converte os parâmetros recebidos, delega a operação ao `SistemaFolha` e formata o retorno.
- `SistemaFolha`: coordena o sistema. Mantém e localiza os empregados, gera os IDs, verifica regras que envolvem mais de um empregado (como a identificação sindical duplicada), controla o undo/redo e a persistência.
- `ConversorEntrada`: converte os textos recebidos pela `Facade` em datas, números e métodos de pagamento.
- `Formatador`: formata valores e horas no padrão esperado pelos testes (vírgula como separador decimal).
- `RelatorioFolha`: monta e grava o arquivo da folha de pagamento.

### Pacote `models`

Os empregados são representados pela classe abstrata `Empregado`, que guarda apenas os dados comuns (id, nome, endereço e salário), e pelas subclasses:

- `EmpregadoHorista`
- `EmpregadoAssalariado`
- `EmpregadoComissionado`

As diferenças entre os tipos (cartão de ponto, vendas, comissão, data de pagamento e cálculo do salário) são resolvidas por polimorfismo, sem uso de `instanceof`.

Outras classes do domínio:

- `MembroSindicato`: identificação sindical, taxa sindical, taxas de serviço e cálculo dos descontos;
- `MetodoPagamento`, com as subclasses `PagamentoEmMaos`, `PagamentoCorreios` e `PagamentoBanco`;
- `CartaoPonto`, `ResultadoVenda`, `TaxaServico` e `Contracheque`;
- `SecoesFolha`: permite que cada tipo de empregado indique em qual seção da folha aparece.

Os objetos de domínio validam os próprios dados no construtor e nos métodos de alteração, então não podem assumir estados inválidos.

### Pacote `exception`

Exceções específicas para cada erro de domínio, com as mensagens exigidas pelos testes.

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

Os testes com sufixo `_1` verificam a persistência e devem ser executados logo após o teste correspondente.

## Persistência

Os dados do sistema são salvos em `wepayu.dat`. Se a estrutura das classes mudar, é preciso apagar esse arquivo antes de rodar os testes novamente.

## Observação

Este README se refere apenas ao primeiro milestone, que envolve as User Stories 1 a 8.
