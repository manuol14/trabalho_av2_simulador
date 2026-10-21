import java.util.LinkedHashMap;
import java.util.Map;

public class SimuladorMemoria {

    public static void main(String[] args) {

        int[] referenceString = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
        int frameCount = 3;

        System.out.println("Cadeia de Referência: " + java.util.Arrays.toString(referenceString));
        System.out.println("Quantidade de Molduras: " + frameCount + "\n");

        Map<String, PageReplacementAlgorithm> algorithms = new LinkedHashMap<>();
        algorithms.put("Método 1 - FIFO", new FifoAlgorithm());
        algorithms.put("Método 2 - LRU", new LruAlgorithm());
        algorithms.put("Método 3 - Relógio", new ClockAlgorithm());
        algorithms.put("Método 4 - Ótimo", new OptimalAlgorithm());


        algorithms.forEach((name, algorithm) -> {
            int faults = algorithm.run(referenceString, frameCount);
            System.out.println(name + " - " + faults + " faltas de página");
        });
    }
}