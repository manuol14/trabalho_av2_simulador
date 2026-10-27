import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SimuladorMemoria {

    private static final String ARQUIVO_PADRAO = "entradas/exemplo_classico.csv";
    private static final int MOLDURAS_PADRAO = 3;

    // uso: java SimuladorMemoria [arquivo.csv] [quantidade de molduras]
    public static void main(String[] args) {

        String arquivoCsv = args.length > 0 ? args[0] : ARQUIVO_PADRAO;
        int frameCount;
        try {
            frameCount = args.length > 1 ? Integer.parseInt(args[1]) : MOLDURAS_PADRAO;
        } catch (NumberFormatException e) {
            System.err.println("Quantidade de molduras inválida: " + args[1]);
            return;
        }
        if (frameCount <= 0) {
            System.err.println("A quantidade de molduras deve ser maior que zero.");
            return;
        }

        int[] referenceString;
        try {
            referenceString = LeitorCsv.lerCadeia(arquivoCsv);
        } catch (NoSuchFileException e) {
            System.err.println("Arquivo não encontrado: " + arquivoCsv);
            return;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Erro ao ler o arquivo CSV: " + e.getMessage());
            return;
        }

        System.out.println("Arquivo de entrada: " + arquivoCsv);
        System.out.println("Cadeia de Referência: " + java.util.Arrays.toString(referenceString));
        System.out.println("Quantidade de Molduras: " + frameCount + "\n");

        Map<String, PageReplacementAlgorithm> algorithms = new LinkedHashMap<>();
        algorithms.put("Método 1 - FIFO", new FifoAlgorithm());
        algorithms.put("Método 2 - LRU", new LruAlgorithm());
        algorithms.put("Método 3 - Relógio", new ClockAlgorithm());
        algorithms.put("Método 4 - Ótimo", new OptimalAlgorithm());


        Map<String, Integer> resultados = new LinkedHashMap<>();
        algorithms.forEach((name, algorithm) -> {
            int faults = algorithm.run(referenceString, frameCount);
            resultados.put(nomeCurto(name), faults);
            System.out.println(name + " - " + faults + " faltas de página");
        });

        gerarGraficos(arquivoCsv, referenceString, frameCount, algorithms, resultados);
    }

    private static void gerarGraficos(String arquivoCsv, int[] referenceString, int frameCount,
                                      Map<String, PageReplacementAlgorithm> algorithms,
                                      Map<String, Integer> resultados) {

        // simula de 1 moldura até a quantidade de páginas distintas (a partir daí as faltas não mudam mais)
        int maxMolduras = Math.max(frameCount, Math.min(paginasDistintas(referenceString), 15));

        Map<String, int[]> faltasPorMolduras = new LinkedHashMap<>();
        algorithms.forEach((name, algorithm) -> {
            int[] faltas = new int[maxMolduras];
            for (int m = 1; m <= maxMolduras; m++) {
                faltas[m - 1] = algorithm.run(referenceString, m);
            }
            faltasPorMolduras.put(nomeCurto(name), faltas);
        });

        imprimirTabela(faltasPorMolduras, maxMolduras);

        String nomeArquivo = Path.of(arquivoCsv).getFileName().toString();
        String nomeBase = nomeArquivo.replaceFirst("\\.[^.]+$", "");
        Path pasta = Path.of("graficos", nomeBase);

        try {
            GeradorGraficos.gerarGraficoBarras(resultados,
                    referenceString.length + " referências  ·  " + frameCount + " molduras  ·  arquivo " + nomeArquivo,
                    pasta.resolve("faltas_por_algoritmo.png"));
            GeradorGraficos.gerarGraficoLinhas(faltasPorMolduras,
                    referenceString.length + " referências  ·  de 1 a " + maxMolduras + " molduras  ·  arquivo " + nomeArquivo,
                    pasta.resolve("faltas_por_molduras.png"));
            System.out.println("\nGráficos salvos em: " + pasta.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Não foi possível salvar os gráficos: " + e.getMessage());
        }
    }

    private static void imprimirTabela(Map<String, int[]> faltasPorMolduras, int maxMolduras) {
        System.out.println("\nFaltas de página por quantidade de molduras:");
        System.out.printf("%-10s", "Molduras");
        for (String nome : faltasPorMolduras.keySet()) System.out.printf("%9s", nome);
        System.out.println();
        for (int m = 1; m <= maxMolduras; m++) {
            System.out.printf("%-10d", m);
            for (int[] faltas : faltasPorMolduras.values()) System.out.printf("%9d", faltas[m - 1]);
            System.out.println();
        }
    }

    private static int paginasDistintas(int[] referenceString) {
        Set<Integer> distintas = new HashSet<>();
        for (int pagina : referenceString) distintas.add(pagina);
        return distintas.size();
    }

    // "Método 1 - FIFO" -> "FIFO"
    private static String nomeCurto(String name) {
        return name.substring(name.lastIndexOf(" - ") + 3);
    }
}
