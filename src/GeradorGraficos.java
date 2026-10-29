import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gera os gráficos comparativos em PNG usando apenas Java2D (sem bibliotecas externas).
 *
 *  - Gráfico de barras: faltas de página de cada algoritmo para a quantidade de molduras escolhida.
 *  - Gráfico de linhas: faltas de página de cada algoritmo variando a quantidade de molduras.
 */
public class GeradorGraficos {

    // a imagem é desenhada em 2x para ficar nítida quando for para o relatório em PDF
    private static final int ESCALA = 2;

    private static final Color FUNDO = new Color(0xFCFCFB);
    private static final Color TEXTO = new Color(0x0B0B0B);
    private static final Color TEXTO_SECUNDARIO = new Color(0x52514E);
    private static final Color TEXTO_SUAVE = new Color(0x898781);
    private static final Color GRADE = new Color(0xE1E0D9);
    private static final Color EIXO = new Color(0xC3C2B7);

    // cada algoritmo sempre recebe a mesma cor, na ordem em que aparece no mapa
    private static final Color[] CORES = {
            new Color(0x2A78D6), // azul
            new Color(0xEB6834), // laranja
            new Color(0x1BAF7A), // verde-água
            new Color(0xEDA100), // amarelo
            new Color(0xE87BA4), // rosa
            new Color(0x008300), // verde
            new Color(0x6250D6), // violeta
            new Color(0xE34948)  // vermelho
    };

    private static final Font FONTE_TITULO = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONTE_SUBTITULO = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONTE_ROTULO = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONTE_VALOR = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONTE_EIXO = new Font("SansSerif", Font.PLAIN, 12);

    // cor usada para o algoritmo na posição informada (a interface usa a mesma cor na tabela)
    public static Color corDoAlgoritmo(int indice) {
        return CORES[indice];
    }

    // desenha e já salva o gráfico em PNG (usado pelo programa de console)
    public static void gerarGraficoBarras(Map<String, Integer> faltas, String subtitulo, Path arquivo) throws IOException {
        salvar(criarGraficoBarras(faltas, subtitulo), arquivo);
    }

    public static void gerarGraficoLinhas(Map<String, int[]> faltasPorMolduras, String subtitulo, Path arquivo) throws IOException {
        salvar(criarGraficoLinhas(faltasPorMolduras, subtitulo), arquivo);
    }

    /**
     * Desenha o gráfico de barras e devolve a imagem (usado também pela interface gráfica).
     *
     * @param faltas    nome do algoritmo -> quantidade de faltas de página
     * @param subtitulo texto exibido abaixo do título (ex.: tamanho da cadeia e molduras)
     */
    public static BufferedImage criarGraficoBarras(Map<String, Integer> faltas, String subtitulo) {
        int largura = 900;
        int alturaLinha = 48;
        int espessuraBarra = 24;
        int topo = 92;
        int margemEsquerda = 110;
        int margemDireita = 60;
        int altura = topo + faltas.size() * alturaLinha + 64;

        BufferedImage imagem = new BufferedImage(largura * ESCALA, altura * ESCALA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararDesenho(imagem, largura, altura);
        desenharTitulo(g, "Faltas de página por algoritmo", subtitulo);

        int maiorValor = 0;
        for (int valor : faltas.values()) maiorValor = Math.max(maiorValor, valor);
        int passo = passoDaEscala(maiorValor, 5);
        int maximoEscala = Math.max(passo, (int) Math.ceil((double) maiorValor / passo) * passo);

        double x0 = margemEsquerda;
        double x1 = largura - margemDireita;
        double y0 = topo;
        double y1 = topo + faltas.size() * alturaLinha;

        // linhas de grade e valores do eixo
        g.setFont(FONTE_EIXO);
        for (int valor = 0; valor <= maximoEscala; valor += passo) {
            double x = x0 + (x1 - x0) * valor / maximoEscala;
            g.setColor(GRADE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(x, y0, x, y1));
            g.setColor(TEXTO_SUAVE);
            textoCentralizado(g, String.valueOf(valor), x, y1 + 20);
        }
        g.setColor(TEXTO_SUAVE);
        textoCentralizado(g, "Faltas de página", (x0 + x1) / 2, y1 + 46);

        // barras
        int i = 0;
        for (Map.Entry<String, Integer> item : faltas.entrySet()) {
            double centro = y0 + i * alturaLinha + alturaLinha / 2.0;
            double comprimento = (x1 - x0) * item.getValue() / maximoEscala;

            g.setFont(FONTE_ROTULO);
            g.setColor(TEXTO);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(item.getKey(), (float) (x0 - 14 - fm.stringWidth(item.getKey())),
                    (float) (centro + fm.getAscent() / 2.0 - 2));

            if (comprimento > 0) {
                g.setColor(CORES[i]);
                g.fill(barraArredondada(x0, centro - espessuraBarra / 2.0, comprimento, espessuraBarra));
            }

            g.setFont(FONTE_VALOR);
            g.setColor(TEXTO);
            fm = g.getFontMetrics();
            g.drawString(String.valueOf(item.getValue()), (float) (x0 + comprimento + 8),
                    (float) (centro + fm.getAscent() / 2.0 - 2));
            i++;
        }

        // linha de base
        g.setColor(EIXO);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(x0, y0, x0, y1));

        g.dispose();
        return imagem;
    }

    /**
     * Desenha o gráfico de linhas e devolve a imagem (usado também pela interface gráfica).
     *
     * @param faltasPorMolduras nome do algoritmo -> vetor onde a posição i guarda as faltas com (i + 1) molduras
     * @param subtitulo         texto exibido abaixo do título
     */
    public static BufferedImage criarGraficoLinhas(Map<String, int[]> faltasPorMolduras, String subtitulo) {
        int largura = 900;
        int altura = 550;

        BufferedImage imagem = new BufferedImage(largura * ESCALA, altura * ESCALA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararDesenho(imagem, largura, altura);
        desenharTitulo(g, "Faltas de página × número de molduras", subtitulo);

        int quantidadeMolduras = 0;
        int maiorValor = 0;
        for (int[] valores : faltasPorMolduras.values()) {
            quantidadeMolduras = Math.max(quantidadeMolduras, valores.length);
            for (int v : valores) maiorValor = Math.max(maiorValor, v);
        }
        int passoY = passoDaEscala(maiorValor, 5);
        int maximoY = Math.max(passoY, (int) Math.ceil((double) maiorValor / passoY) * passoY);
        int passoX = quantidadeMolduras <= 20 ? 1 : passoDaEscala(quantidadeMolduras, 10);
        boolean desenharMarcadores = quantidadeMolduras <= 30;

        double x0 = 74;
        double x1 = largura - 40;
        double y0 = 146;
        double y1 = altura - 74;
        double folga = 26; // espaço para os marcadores das pontas não encostarem nas bordas

        desenharLegenda(g, faltasPorMolduras, x0, 92, desenharMarcadores);

        // grade horizontal e eixo Y
        g.setFont(FONTE_EIXO);
        for (int valor = 0; valor <= maximoY; valor += passoY) {
            double y = y1 - (y1 - y0) * valor / maximoY;
            g.setColor(valor == 0 ? EIXO : GRADE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(x0, y, x1, y));
            g.setColor(TEXTO_SUAVE);
            String texto = String.valueOf(valor);
            g.drawString(texto, (float) (x0 - 10 - g.getFontMetrics().stringWidth(texto)), (float) (y + 4));
        }
        g.setColor(TEXTO_SUAVE);
        g.drawString("Faltas de página", (float) x0 - 30, (float) y0 - 16);

        // eixo X
        for (int m = 1; m <= quantidadeMolduras; m++) {
            if (m != 1 && m % passoX != 0) continue;
            textoCentralizado(g, String.valueOf(m), posicaoX(m, quantidadeMolduras, x0 + folga, x1 - folga), y1 + 20);
        }
        textoCentralizado(g, "Número de molduras", (x0 + x1) / 2, y1 + 48);

        // linhas de cada algoritmo
        int i = 0;
        for (int[] valores : faltasPorMolduras.values()) {
            g.setColor(CORES[i]);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D linha = new Path2D.Double();
            for (int m = 1; m <= valores.length; m++) {
                double x = posicaoX(m, quantidadeMolduras, x0 + folga, x1 - folga);
                double y = y1 - (y1 - y0) * valores[m - 1] / maximoY;
                if (m == 1) linha.moveTo(x, y);
                else linha.lineTo(x, y);
            }
            g.draw(linha);
            i++;
        }

        // marcadores por cima das linhas (cada algoritmo com um formato diferente)
        if (desenharMarcadores) {
            List<int[]> series = new ArrayList<>(faltasPorMolduras.values());
            double espacoEntrePontos = (x1 - x0 - 2 * folga) / Math.max(1, quantidadeMolduras - 1);
            double afastamento = Math.min(11, espacoEntrePontos / (series.size() + 1));

            for (int s = 0; s < series.size(); s++) {
                int[] valores = series.get(s);
                for (int m = 1; m <= valores.length; m++) {
                    // quando algoritmos empatam no mesmo ponto, os marcadores ficam lado a lado
                    // para que nenhum fique escondido atrás do outro
                    int empatados = 0;
                    int posicao = 0;
                    for (int outra = 0; outra < series.size(); outra++) {
                        int[] outros = series.get(outra);
                        if (m <= outros.length && outros[m - 1] == valores[m - 1]) {
                            if (outra < s) posicao++;
                            empatados++;
                        }
                    }
                    double deslocamento = (posicao - (empatados - 1) / 2.0) * afastamento;

                    double x = posicaoX(m, quantidadeMolduras, x0 + folga, x1 - folga) + deslocamento;
                    double y = y1 - (y1 - y0) * valores[m - 1] / maximoY;
                    desenharMarcador(g, s, x, y);
                }
            }
        }

        g.dispose();
        return imagem;
    }

    private static void desenharLegenda(Graphics2D g, Map<String, int[]> series, double x, double centroY, boolean comMarcador) {
        g.setFont(FONTE_ROTULO);
        FontMetrics fm = g.getFontMetrics();
        int i = 0;
        for (String nome : series.keySet()) {
            g.setColor(CORES[i]);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(x, centroY, x + 24, centroY));
            if (comMarcador) desenharMarcador(g, i, x + 12, centroY);

            g.setColor(TEXTO_SECUNDARIO);
            g.drawString(nome, (float) (x + 32), (float) (centroY + fm.getAscent() / 2.0 - 2));
            x += 32 + fm.stringWidth(nome) + 28;
            i++;
        }
    }

    private static void desenharMarcador(Graphics2D g, int indice, double x, double y) {
        Shape forma;
        switch (indice % 4) {
            case 0: // círculo
                forma = new Ellipse2D.Double(x - 4.5, y - 4.5, 9, 9);
                break;
            case 1: // quadrado
                forma = new Rectangle2D.Double(x - 4, y - 4, 8, 8);
                break;
            case 2: // triângulo
                Path2D triangulo = new Path2D.Double();
                triangulo.moveTo(x, y - 5.5);
                triangulo.lineTo(x + 5.5, y + 4.5);
                triangulo.lineTo(x - 5.5, y + 4.5);
                triangulo.closePath();
                forma = triangulo;
                break;
            default: // losango
                Path2D losango = new Path2D.Double();
                losango.moveTo(x, y - 6);
                losango.lineTo(x + 6, y);
                losango.lineTo(x, y + 6);
                losango.lineTo(x - 6, y);
                losango.closePath();
                forma = losango;
        }
        // contorno na cor do fundo para o marcador continuar visível quando cruza outra linha
        g.setColor(FUNDO);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(forma);
        g.setColor(CORES[indice]);
        g.fill(forma);
    }

    private static Graphics2D prepararDesenho(BufferedImage imagem, int largura, int altura) {
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.scale(ESCALA, ESCALA);
        g.setColor(FUNDO);
        g.fillRect(0, 0, largura, altura);
        return g;
    }

    private static void desenharTitulo(Graphics2D g, String titulo, String subtitulo) {
        g.setColor(TEXTO);
        g.setFont(FONTE_TITULO);
        g.drawString(titulo, 32, 38);
        g.setColor(TEXTO_SECUNDARIO);
        g.setFont(FONTE_SUBTITULO);
        g.drawString(subtitulo, 32, 60);
    }

    private static Shape barraArredondada(double x, double yTopo, double comprimento, double espessura) {
        // ponta arredondada no fim da barra e reta na linha de base
        double raio = Math.min(4, comprimento);
        double fim = x + comprimento;
        Path2D barra = new Path2D.Double();
        barra.moveTo(x, yTopo);
        barra.lineTo(fim - raio, yTopo);
        barra.quadTo(fim, yTopo, fim, yTopo + raio);
        barra.lineTo(fim, yTopo + espessura - raio);
        barra.quadTo(fim, yTopo + espessura, fim - raio, yTopo + espessura);
        barra.lineTo(x, yTopo + espessura);
        barra.closePath();
        return barra;
    }

    private static double posicaoX(int moldura, int total, double inicio, double fim) {
        if (total <= 1) return (inicio + fim) / 2;
        return inicio + (fim - inicio) * (moldura - 1) / (total - 1);
    }

    private static void textoCentralizado(Graphics2D g, String texto, double centroX, double baseY) {
        int larguraTexto = g.getFontMetrics().stringWidth(texto);
        g.drawString(texto, (float) (centroX - larguraTexto / 2.0), (float) baseY);
    }

    // escolhe um passo "redondo" (1, 2, 5, 10, 20, 50...) para os valores do eixo
    private static int passoDaEscala(int maximo, int divisoes) {
        if (maximo <= divisoes) return 1;
        double bruto = (double) maximo / divisoes;
        double potencia = Math.pow(10, Math.floor(Math.log10(bruto)));
        for (double fator : new double[]{1, 2, 5, 10}) {
            if (fator * potencia >= bruto) return (int) Math.round(fator * potencia);
        }
        return (int) Math.round(10 * potencia);
    }

    private static void salvar(BufferedImage imagem, Path arquivo) throws IOException {
        if (arquivo.getParent() != null) Files.createDirectories(arquivo.getParent());
        ImageIO.write(imagem, "png", arquivo.toFile());
    }
}
