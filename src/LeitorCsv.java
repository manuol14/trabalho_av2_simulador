import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê a cadeia de referência (sequência de páginas) a partir de um arquivo CSV.
 *
 * Formatos aceitos:
 *  - tudo em uma linha:      7,0,1,2,0,3
 *  - uma página por linha:   7
 *                            0
 *                            1
 *  - separado por ";" (padrão do Excel em português) ou por espaços.
 *
 * Linhas vazias e linhas que começam com "#" são ignoradas.
 * Se a primeira linha tiver texto (ex.: "paginas"), ela é tratada como cabeçalho.
 */
public class LeitorCsv {

    public static int[] lerCadeia(String caminhoArquivo) throws IOException {
        List<String> linhas = Files.readAllLines(Path.of(caminhoArquivo), StandardCharsets.UTF_8);
        List<Integer> paginas = new ArrayList<>();
        boolean primeiraLinhaDeDados = true;

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i).replace("﻿", "").trim();
            if (linha.isEmpty() || linha.startsWith("#")) continue;

            List<Integer> valoresDaLinha = new ArrayList<>();
            String primeiroValorInvalido = null;

            for (String valor : linha.split("[,;\\s]+")) {
                valor = valor.replace("\"", "").trim();
                if (valor.isEmpty()) continue;
                try {
                    valoresDaLinha.add(Integer.parseInt(valor));
                } catch (NumberFormatException e) {
                    if (primeiroValorInvalido == null) primeiroValorInvalido = valor;
                }
            }

            boolean ehCabecalho = primeiraLinhaDeDados && valoresDaLinha.isEmpty();
            primeiraLinhaDeDados = false;
            if (ehCabecalho) continue;

            if (primeiroValorInvalido != null) {
                throw new IllegalArgumentException(
                        "valor inválido \"" + primeiroValorInvalido + "\" na linha " + (i + 1) + " do arquivo " + caminhoArquivo);
            }

            for (int pagina : valoresDaLinha) {
                if (pagina < 0) {
                    throw new IllegalArgumentException(
                            "página negativa (" + pagina + ") na linha " + (i + 1) + "; use apenas números >= 0");
                }
                paginas.add(pagina);
            }
        }

        if (paginas.isEmpty()) {
            throw new IllegalArgumentException("nenhuma página encontrada no arquivo " + caminhoArquivo);
        }

        int[] cadeia = new int[paginas.size()];
        for (int i = 0; i < cadeia.length; i++) {
            cadeia[i] = paginas.get(i);
        }
        return cadeia;
    }
}
