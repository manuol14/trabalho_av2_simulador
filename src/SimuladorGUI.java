import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interface gráfica (Swing) do simulador.
 *
 * Permite digitar a cadeia de referência ou abrir um arquivo CSV, escolher a quantidade de molduras
 * e ver as faltas de página de cada método junto com os gráficos comparativos.
 */
public class SimuladorGUI extends JFrame {

    private static final Path PASTA_ENTRADAS = Path.of("entradas");
    private static final String EXEMPLO_INICIAL = "exemplo_classico.csv";
    private static final String SELECIONE = "Selecione…";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Color FUNDO_GRAFICO = new Color(0xFCFCFB);

    private final JTextArea campoCadeia = new JTextArea(3, 50);
    private final JSpinner campoMolduras = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JComboBox<String> listaExemplos = new JComboBox<>();
    private final JButton botaoSimular = new JButton("Simular");
    private final JButton botaoSalvar = new JButton("Salvar gráficos (PNG)");

    private final DefaultTableModel modeloResultados =
            tabelaSomenteLeitura("Método", "Algoritmo", "Faltas", "Taxa de faltas");
    private final DefaultTableModel modeloPorMolduras = tabelaSomenteLeitura();
    private final JTable tabelaResultados = new JTable(modeloResultados);
    private final JTable tabelaPorMolduras = new JTable(modeloPorMolduras);
    private final JTextArea saidaTexto = new JTextArea(5, 28);
    private final JLabel resumo = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final PainelImagem painelBarras = new PainelImagem();
    private final PainelImagem painelLinhas = new PainelImagem();

    private String arquivoCarregado;   // nome do último CSV aberto (ex.: "aleatorio.csv")
    private String textoCarregado;     // cadeia como foi escrita na tela ao abrir o CSV
    private Resultado ultimoResultado;
    private int numeroSimulacao = 0;
    private boolean atualizandoLista = false;

    public SimuladorGUI() {
        super("Simulador de Substituição de Páginas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBorder(new EmptyBorder(12, 12, 8, 12));
        raiz.add(criarPainelEntrada(), BorderLayout.NORTH);
        raiz.add(criarPainelCentral(), BorderLayout.CENTER);
        raiz.add(criarBarraStatus(), BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(botaoSimular);

        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        carregarExemploInicial();
    }

    // ------------------------------------------------------------------ montagem da tela

    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Entrada"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        painel.add(new JLabel("Cadeia de referência:"), c);

        campoCadeia.setLineWrap(true);
        campoCadeia.setWrapStyleWord(true);
        campoCadeia.setToolTipText("Números das páginas separados por vírgula, ponto e vírgula ou espaço (Ctrl+Enter simula)");
        campoCadeia.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "simular");
        campoCadeia.getActionMap().put("simular", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simular();
            }
        });
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        painel.add(new JScrollPane(campoCadeia), c);

        JButton botaoAbrir = new JButton("Abrir CSV…");
        botaoAbrir.addActionListener(e -> escolherArquivoCsv());

        listaExemplos.addActionListener(e -> {
            if (atualizandoLista) return;
            Object selecionado = listaExemplos.getSelectedItem();
            if (selecionado != null && !SELECIONE.equals(selecionado)) {
                carregarCsv(PASTA_ENTRADAS.resolve(selecionado.toString()));
            }
        });

        ((JSpinner.DefaultEditor) campoMolduras.getEditor()).getTextField().setColumns(3);
        campoMolduras.addChangeListener(e -> {
            if (ultimoResultado != null) simular();
        });

        botaoSimular.addActionListener(e -> simular());
        botaoSimular.setFont(botaoSimular.getFont().deriveFont(Font.BOLD));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controles.add(botaoAbrir);
        controles.add(Box.createHorizontalStrut(8));
        controles.add(new JLabel("Exemplos:"));
        controles.add(listaExemplos);
        controles.add(Box.createHorizontalStrut(16));
        controles.add(new JLabel("Molduras:"));
        controles.add(campoMolduras);
        controles.add(Box.createHorizontalStrut(16));
        controles.add(botaoSimular);

        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        painel.add(controles, c);
        return painel;
    }

    private JComponent criarPainelCentral() {
        // lado esquerdo: resultados de cada método
        configurarTabela(tabelaResultados);
        DefaultTableCellRenderer esquerda = new DefaultTableCellRenderer();
        DefaultTableCellRenderer direita = new DefaultTableCellRenderer();
        direita.setHorizontalAlignment(SwingConstants.RIGHT);
        tabelaResultados.getColumnModel().getColumn(0).setCellRenderer(esquerda);
        tabelaResultados.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorAlgoritmo());
        tabelaResultados.getColumnModel().getColumn(2).setCellRenderer(direita);
        tabelaResultados.getColumnModel().getColumn(3).setCellRenderer(direita);

        tabelaResultados.setPreferredScrollableViewportSize(new Dimension(360, tabelaResultados.getRowHeight() * 4));
        JScrollPane rolagemTabela = new JScrollPane(tabelaResultados);

        saidaTexto.setEditable(false);
        saidaTexto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        saidaTexto.setBorder(new EmptyBorder(6, 8, 6, 8));

        JPanel painelSaida = new JPanel(new BorderLayout(0, 4));
        painelSaida.add(new JLabel("Saída do simulador:"), BorderLayout.NORTH);
        painelSaida.add(new JScrollPane(saidaTexto), BorderLayout.CENTER);

        resumo.setVerticalAlignment(SwingConstants.TOP);
        resumo.setBorder(new EmptyBorder(4, 2, 4, 2));

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.add(rolagemTabela, BorderLayout.NORTH);
        conteudo.add(painelSaida, BorderLayout.CENTER);
        conteudo.add(resumo, BorderLayout.SOUTH);

        JPanel painelResultado = new JPanel(new BorderLayout());
        painelResultado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Resultado"), new EmptyBorder(4, 6, 6, 6)));
        painelResultado.add(conteudo, BorderLayout.NORTH);

        // lado direito: gráficos comparativos
        configurarTabela(tabelaPorMolduras);
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Faltas por algoritmo", painelBarras);
        abas.addTab("Faltas × número de molduras", painelLinhas);
        abas.addTab("Tabela por molduras", new JScrollPane(tabelaPorMolduras));

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelResultado, abas);
        divisao.setDividerLocation(400);
        divisao.setResizeWeight(0);
        divisao.setBorder(null);
        return divisao;
    }

    private JPanel criarBarraStatus() {
        botaoSalvar.setEnabled(false);
        botaoSalvar.addActionListener(e -> salvarGraficos());
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(status, BorderLayout.CENTER);
        barra.add(botaoSalvar, BorderLayout.EAST);
        return barra;
    }

    // ------------------------------------------------------------------ ações

    private void carregarExemploInicial() {
        List<String> exemplos = listarExemplos();
        atualizandoLista = true;
        listaExemplos.addItem(SELECIONE);
        for (String nome : exemplos) listaExemplos.addItem(nome);
        listaExemplos.setEnabled(!exemplos.isEmpty());
        atualizandoLista = false;

        if (exemplos.contains(EXEMPLO_INICIAL)) {
            listaExemplos.setSelectedItem(EXEMPLO_INICIAL); // dispara o carregamento
        } else {
            campoCadeia.setText("7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1");
            simular();
        }
    }

    private static List<String> listarExemplos() {
        List<String> nomes = new ArrayList<>();
        if (!Files.isDirectory(PASTA_ENTRADAS)) return nomes;
        try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(PASTA_ENTRADAS, "*.csv")) {
            for (Path arquivo : arquivos) nomes.add(arquivo.getFileName().toString());
        } catch (IOException e) {
            // sem exemplos, a lista fica desabilitada
        }
        Collections.sort(nomes);
        return nomes;
    }

    private void escolherArquivoCsv() {
        File pastaInicial = Files.isDirectory(PASTA_ENTRADAS) ? PASTA_ENTRADAS.toFile() : new File(".");
        JFileChooser seletor = new JFileChooser(pastaInicial);
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv, *.txt)", "csv", "txt"));
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path arquivo = seletor.getSelectedFile().toPath();
        atualizandoLista = true;
        listaExemplos.setSelectedItem(SELECIONE);
        atualizandoLista = false;
        carregarCsv(arquivo);
    }

    private void carregarCsv(Path arquivo) {
        try {
            int[] cadeia = LeitorCsv.lerCadeia(arquivo.toString());
            String texto = juntar(cadeia);
            campoCadeia.setText(texto);
            campoCadeia.setCaretPosition(0);
            textoCarregado = texto;
            arquivoCarregado = arquivo.getFileName().toString();
            simular();
        } catch (NoSuchFileException e) {
            mostrarErro("Arquivo não encontrado: " + arquivo);
        } catch (IOException | IllegalArgumentException e) {
            mostrarErro("Não foi possível ler o arquivo CSV:\n" + e.getMessage());
        }
    }

    private void simular() {
        int[] cadeia;
        try {
            cadeia = LeitorCsv.lerCadeiaDeTexto(campoCadeia.getText());
        } catch (IllegalArgumentException e) {
            mostrarErro("Cadeia de referência inválida: " + e.getMessage());
            return;
        }
        int molduras = (Integer) campoMolduras.getValue();
        boolean veioDoCsv = textoCarregado != null && campoCadeia.getText().equals(textoCarregado);
        String origem = veioDoCsv ? arquivoCarregado : null;

        int id = ++numeroSimulacao;
        botaoSimular.setEnabled(false);
        status.setText("Simulando…");

        // a simulação roda fora da thread da tela para ela não travar com cadeias grandes
        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() {
                return calcular(cadeia, molduras, origem);
            }

            @Override
            protected void done() {
                if (id != numeroSimulacao) return; // já existe uma simulação mais nova
                botaoSimular.setEnabled(true);
                try {
                    mostrar(get());
                } catch (Exception e) {
                    status.setText("Erro na simulação: " + e.getMessage());
                }
            }
        }.execute();
    }

    private static Resultado calcular(int[] cadeia, int molduras, String arquivo) {
        Resultado r = new Resultado();
        r.cadeia = cadeia;
        r.molduras = molduras;
        r.pasta = arquivo == null ? "entrada_manual" : arquivo.replaceFirst("\\.[^.]+$", "");
        String origem = arquivo == null ? "cadeia digitada" : "arquivo " + arquivo;

        Map<String, PageReplacementAlgorithm> algoritmos = SimuladorMemoria.criarAlgoritmos();
        algoritmos.forEach((nome, algoritmo) -> {
            int faltas = algoritmo.run(cadeia, molduras);
            r.faltas.put(nome, faltas);
            r.faltasPorAlgoritmo.put(SimuladorMemoria.nomeCurto(nome), faltas);
        });

        int maxMolduras = SimuladorMemoria.calcularMaxMolduras(cadeia, molduras);
        r.faltasPorMolduras = SimuladorMemoria.simularVariandoMolduras(algoritmos, cadeia, maxMolduras);

        r.subtituloBarras = cadeia.length + " referências  ·  " + molduras + " molduras  ·  " + origem;
        r.subtituloLinhas = cadeia.length + " referências  ·  de 1 a " + maxMolduras + " molduras  ·  " + origem;
        r.imagemBarras = GeradorGraficos.criarGraficoBarras(r.faltasPorAlgoritmo, r.subtituloBarras);
        r.imagemLinhas = GeradorGraficos.criarGraficoLinhas(r.faltasPorMolduras, r.subtituloLinhas);
        return r;
    }

    private void mostrar(Resultado r) {
        ultimoResultado = r;

        // tabela e saída no formato pedido no trabalho
        modeloResultados.setRowCount(0);
        StringBuilder saida = new StringBuilder();
        for (Map.Entry<String, Integer> item : r.faltas.entrySet()) {
            String nomeCompleto = item.getKey();
            String metodo = nomeCompleto.substring(0, nomeCompleto.indexOf(" - "));
            double taxa = 100.0 * item.getValue() / r.cadeia.length;
            modeloResultados.addRow(new Object[]{
                    metodo, SimuladorMemoria.nomeCurto(nomeCompleto), item.getValue(), String.format(PT_BR, "%.1f%%", taxa)});
            saida.append(nomeCompleto).append(" - ").append(item.getValue()).append(" faltas de página\n");
        }
        saidaTexto.setText(saida.toString().trim());
        resumo.setText(montarResumo(r));

        painelBarras.setImagem(r.imagemBarras);
        painelLinhas.setImagem(r.imagemLinhas);
        preencherTabelaPorMolduras(r);

        status.setText(r.cadeia.length + " referências  ·  " + SimuladorMemoria.paginasDistintas(r.cadeia)
                + " páginas distintas  ·  " + r.molduras + " molduras");
        botaoSalvar.setEnabled(true);
    }

    private void preencherTabelaPorMolduras(Resultado r) {
        List<String> colunas = new ArrayList<>();
        colunas.add("Molduras");
        colunas.addAll(r.faltasPorMolduras.keySet());
        modeloPorMolduras.setColumnIdentifiers(colunas.toArray());
        modeloPorMolduras.setRowCount(0);

        int linhas = r.faltasPorMolduras.values().iterator().next().length;
        for (int m = 1; m <= linhas; m++) {
            List<Object> linha = new ArrayList<>();
            linha.add(m);
            for (int[] faltas : r.faltasPorMolduras.values()) linha.add(faltas[m - 1]);
            modeloPorMolduras.addRow(linha.toArray());
        }

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        if (tabelaPorMolduras.getTableHeader().getDefaultRenderer() instanceof DefaultTableCellRenderer) {
            ((DefaultTableCellRenderer) tabelaPorMolduras.getTableHeader().getDefaultRenderer())
                    .setHorizontalAlignment(SwingConstants.CENTER);
        }
        for (int i = 0; i < tabelaPorMolduras.getColumnCount(); i++) {
            tabelaPorMolduras.getColumnModel().getColumn(i).setCellRenderer(centro);
        }
        // destaca a linha com a quantidade de molduras escolhida
        tabelaPorMolduras.setRowSelectionInterval(r.molduras - 1, r.molduras - 1);
    }

    private static String montarResumo(Resultado r) {
        // melhor algoritmo que pode ser usado na prática (o Ótimo serve só de referência)
        int menor = Integer.MAX_VALUE;
        List<String> melhores = new ArrayList<>();
        for (Map.Entry<String, Integer> item : r.faltasPorAlgoritmo.entrySet()) {
            if (item.getKey().equals("Ótimo")) continue;
            if (item.getValue() < menor) {
                menor = item.getValue();
                melhores.clear();
            }
            if (item.getValue() == menor) melhores.add(item.getKey());
        }

        StringBuilder texto = new StringBuilder("<html><div style='width:280px'>");
        texto.append("Menos faltas (sem contar o Ótimo): <b>")
                .append(String.join(", ", melhores)).append("</b> – ").append(menor).append(" faltas<br>");
        Integer otimo = r.faltasPorAlgoritmo.get("Ótimo");
        if (otimo != null) {
            texto.append("Ótimo (limite teórico): <b>").append(otimo).append(" faltas</b>");
        }

        // anomalia de Belady: mais molduras gerando mais faltas
        for (Map.Entry<String, int[]> item : r.faltasPorMolduras.entrySet()) {
            int[] faltas = item.getValue();
            for (int m = 1; m < faltas.length; m++) {
                if (faltas[m] > faltas[m - 1]) {
                    texto.append("<br>Anomalia de Belady no <b>").append(item.getKey()).append("</b>: de ")
                            .append(m).append(" para ").append(m + 1).append(" molduras, as faltas subiram de ")
                            .append(faltas[m - 1]).append(" para ").append(faltas[m]);
                    break;
                }
            }
        }
        return texto.append("</div></html>").toString();
    }

    private void salvarGraficos() {
        Resultado r = ultimoResultado;
        if (r == null) return;
        Path pasta = Path.of("graficos", r.pasta);
        try {
            GeradorGraficos.gerarGraficoBarras(r.faltasPorAlgoritmo, r.subtituloBarras, pasta.resolve("faltas_por_algoritmo.png"));
            GeradorGraficos.gerarGraficoLinhas(r.faltasPorMolduras, r.subtituloLinhas, pasta.resolve("faltas_por_molduras.png"));
            status.setText("Gráficos salvos em " + pasta.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Gráficos salvos em:\n" + pasta.toAbsolutePath(),
                    "Gráficos salvos", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            mostrarErro("Não foi possível salvar os gráficos: " + e.getMessage());
        }
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------ utilitários

    private static String juntar(int[] cadeia) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < cadeia.length; i++) {
            if (i > 0) texto.append(", ");
            texto.append(cadeia[i]);
        }
        return texto.toString();
    }

    private static DefaultTableModel tabelaSomenteLeitura(Object... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    private static void configurarTabela(JTable tabela) {
        tabela.setRowHeight(26);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setFocusable(false);
    }

    private static class Resultado {
        int[] cadeia;
        int molduras;
        String pasta;
        final Map<String, Integer> faltas = new LinkedHashMap<>();             // "Método 1 - FIFO" -> faltas
        final Map<String, Integer> faltasPorAlgoritmo = new LinkedHashMap<>(); // "FIFO" -> faltas
        Map<String, int[]> faltasPorMolduras;
        String subtituloBarras;
        String subtituloLinhas;
        BufferedImage imagemBarras;
        BufferedImage imagemLinhas;
    }

    /** Mostra a imagem do gráfico ajustada ao tamanho do painel, sem distorcer. */
    private static class PainelImagem extends JPanel {
        private BufferedImage imagem;
        private BufferedImage reduzida;

        PainelImagem() {
            setBackground(FUNDO_GRAFICO);
        }

        void setImagem(BufferedImage imagem) {
            this.imagem = imagem;
            this.reduzida = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagem == null) return;

            double escala = Math.min((getWidth() - 24.0) / imagem.getWidth(), (getHeight() - 24.0) / imagem.getHeight());
            int largura = (int) (imagem.getWidth() * escala);
            int altura = (int) (imagem.getHeight() * escala);
            if (largura <= 0 || altura <= 0) return;

            // considera a escala da tela (monitores de alta resolução) para a imagem ficar nítida
            Graphics2D g2 = (Graphics2D) g.create();
            double escalaTela = g2.getTransform().getScaleX();
            int larguraReal = (int) Math.round(largura * escalaTela);
            int alturaReal = (int) Math.round(altura * escalaTela);
            if (reduzida == null || reduzida.getWidth() != larguraReal || reduzida.getHeight() != alturaReal) {
                reduzida = redimensionar(imagem, larguraReal, alturaReal);
            }
            g2.drawImage(reduzida, (getWidth() - largura) / 2, (getHeight() - altura) / 2, largura, altura, null);
            g2.dispose();
        }

        // reduz aos poucos (metade por vez) para a imagem não ficar serrilhada
        private static BufferedImage redimensionar(BufferedImage origem, int largura, int altura) {
            BufferedImage atual = origem;
            int w = origem.getWidth();
            int h = origem.getHeight();
            do {
                w = w > largura ? Math.max(w / 2, largura) : largura;
                h = h > altura ? Math.max(h / 2, altura) : altura;
                BufferedImage passo = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = passo.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(atual, 0, 0, w, h, null);
                g.dispose();
                atual = passo;
            } while (w != largura || h != altura);
            return atual;
        }
    }

    /** Mostra um quadradinho com a cor do algoritmo, a mesma usada nos gráficos. */
    private static class RenderizadorAlgoritmo extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionado,
                                                       boolean foco, int linha, int coluna) {
            JLabel rotulo = (JLabel) super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
            rotulo.setIcon(new IconeCor(GeradorGraficos.corDoAlgoritmo(linha)));
            rotulo.setIconTextGap(8);
            return rotulo;
        }
    }

    private static class IconeCor implements Icon {
        private final Color cor;

        IconeCor(Color cor) {
            this.cor = cor;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillRoundRect(x, y + 1, 12, 12, 4, 4);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // se não conseguir, continua com o visual padrão do Java
        }
        SwingUtilities.invokeLater(() -> new SimuladorGUI().setVisible(true));
    }
}
