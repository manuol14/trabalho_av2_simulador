# Simulador de Algoritmos de Substituição de Páginas

Simulador em Java que lê uma cadeia de referência (sequência de páginas) de um arquivo **CSV**, executa quatro algoritmos de substituição de páginas e mostra quantas **faltas de página** cada um teve. Além da saída no console, o programa gera **gráficos comparativos** em PNG.

Algoritmos implementados:

| Método | Algoritmo | Classe |
|---|---|---|
| 1 | FIFO (First In, First Out) | `FifoAlgorithm` |
| 2 | LRU (Least Recently Used) | `LruAlgorithm` |
| 3 | Relógio (Clock / segunda chance) | `ClockAlgorithm` |
| 4 | Ótimo | `OptimalAlgorithm` |

## Requisitos

- JDK 11 ou superior (testado com JDK 21)
- Nenhuma biblioteca externa: os gráficos são desenhados com Java2D (`java.awt`) e salvos com `ImageIO`

## Como executar

> Execute sempre a partir da **pasta raiz do projeto**, porque os caminhos `entradas/` e `graficos/` são relativos a ela.

### Pelo terminal

```bash
# 1. compilar
javac -encoding UTF-8 -d out src/*.java

# 2. executar com o arquivo padrão (entradas/exemplo_classico.csv e 3 molduras)
java -cp out SimuladorMemoria

# 3. executar escolhendo o arquivo CSV e a quantidade de molduras
java -cp out SimuladorMemoria entradas/aleatorio.csv 4
```

Formato geral: `java -cp out SimuladorMemoria [arquivo.csv] [quantidade de molduras]`

### Pelo IntelliJ IDEA

1. Abra a pasta do projeto no IntelliJ.
2. Abra `src/SimuladorMemoria.java` e clique em **Run** (seta verde ao lado do `main`).
3. Para usar outro CSV ou outra quantidade de molduras: **Run → Edit Configurations… → Program arguments** e informe, por exemplo, `entradas/aleatorio.csv 4`.

## Formato do arquivo CSV

O arquivo deve conter os números das páginas (inteiros ≥ 0). Todos os números do arquivo formam uma única cadeia de referência, na ordem em que aparecem. São aceitos:

```text
7,0,1,2,0,3,0,4          ← tudo em uma linha, separado por vírgula
```

```text
paginas                  ← cabeçalho opcional (linha sem números é ignorada)
7
0
1
```

```text
# linhas começando com # são comentários
7;0;1;2                  ← ponto e vírgula (padrão do Excel em português)
0;3;0;4
```

Também funciona com arquivos salvos pelo Excel (com aspas, BOM ou quebra de linha do Windows). Se houver algum valor inválido, o programa informa a linha do erro.

## Saída

No console, o resultado de cada método no formato pedido:

```text
Arquivo de entrada: entradas/exemplo_classico.csv
Cadeia de Referência: [7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1]
Quantidade de Molduras: 3

Método 1 - FIFO - 15 faltas de página
Método 2 - LRU - 12 faltas de página
Método 3 - Relógio - 14 faltas de página
Método 4 - Ótimo - 9 faltas de página

Faltas de página por quantidade de molduras:
Molduras       FIFO      LRU  Relógio    Ótimo
1                20       20       20       20
2                15       17       15       13
3                15       12       14        9
4                10        8        9        8
5                 9        7        9        7
6                 6        6        6        6

Gráficos salvos em: .../graficos/exemplo_classico
```

E dois gráficos na pasta `graficos/<nome-do-arquivo>/`:

- `faltas_por_algoritmo.png`: faltas de cada algoritmo com a quantidade de molduras escolhida.
- `faltas_por_molduras.png`: faltas de cada algoritmo variando de 1 moldura até a quantidade de páginas distintas da cadeia (limitado a 15).

![Faltas por algoritmo](graficos/exemplo_classico/faltas_por_algoritmo.png)

![Faltas por número de molduras](graficos/exemplo_classico/faltas_por_molduras.png)

## Cenários de teste incluídos

Na pasta `entradas/` há cinco arquivos prontos, usados no relatório:

| Arquivo | O que representa | Comando |
|---|---|---|
| `exemplo_classico.csv` | Cadeia clássica dos livros de Sistemas Operacionais | `java -cp out SimuladorMemoria entradas/exemplo_classico.csv 3` |
| `anomalia_belady.csv` | Cadeia que mostra a anomalia de Belady no FIFO | `java -cp out SimuladorMemoria entradas/anomalia_belady.csv 3` |
| `ciclo_sequencial.csv` | Laço que percorre 5 páginas em sequência | `java -cp out SimuladorMemoria entradas/ciclo_sequencial.csv 4` |
| `localidade.csv` | Uma página muito usada + páginas de dados que avançam aos poucos | `java -cp out SimuladorMemoria entradas/localidade.csv 3` |
| `aleatorio.csv` | 40 páginas aleatórias entre 0 e 9 (sem localidade) | `java -cp out SimuladorMemoria entradas/aleatorio.csv 4` |

Resultados obtidos:

| Cenário | Referências | Molduras | FIFO | LRU | Relógio | Ótimo |
|---|---:|---:|---:|---:|---:|---:|
| Exemplo clássico | 20 | 3 | 15 | 12 | 14 | 9 |
| Anomalia de Belady | 12 | 3 | 9 | 10 | 9 | 7 |
| Ciclo sequencial | 20 | 4 | 20 | 20 | 20 | 8 |
| Localidade | 40 | 3 | 15 | 12 | 13 | 12 |
| Aleatório | 40 | 4 | 29 | 26 | 29 | 17 |

## Estrutura do projeto

```text
src/
├── SimuladorMemoria.java          programa principal
├── PageReplacementAlgorithm.java  interface comum dos algoritmos
├── FifoAlgorithm.java             Método 1 - FIFO
├── LruAlgorithm.java              Método 2 - LRU
├── ClockAlgorithm.java            Método 3 - Relógio
├── OptimalAlgorithm.java          Método 4 - Ótimo
├── LeitorCsv.java                 leitura da cadeia de referência a partir do CSV
└── GeradorGraficos.java           geração dos gráficos comparativos (PNG)
entradas/                          arquivos CSV de exemplo
graficos/                          gráficos gerados pelo programa
relatorio/                         relatório do trabalho
```
