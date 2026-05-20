package es.upm.careeradvisor.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Panel Swing que muestra el resultado del ranking de carreras.
 * Se lanza desde el AgenteVisualizacion una vez que recibe el INFORM.
 */
public class ResultadoGUI extends JFrame {

    // Colores de la paleta
    private static final Color C_BG        = new Color(245, 247, 252);
    private static final Color C_CARD_TOP  = new Color(63, 81, 181);
    private static final Color C_CARD_REST = new Color(255, 255, 255);
    private static final Color C_ACCENT    = new Color(63, 81, 181);
    private static final Color C_TEXT_DARK = new Color(33, 33, 33);
    private static final Color C_TEXT_GRAY = new Color(100, 100, 100);
    private static final Color C_BAR_BG   = new Color(220, 224, 240);
    private static final Color C_GOLD     = new Color(255, 193, 7);
    private static final Color C_SILVER   = new Color(189, 189, 189);
    private static final Color C_BRONZE   = new Color(188, 143, 143);

    // Datos a mostrar
    public static class CarreraResultado {
        public String nombre;
        public String emoji;
        public String descripcion;
        public double score;
        public long keywordsMatched;
        public long keywordsPenalty;

        public CarreraResultado(String nombre, String emoji, String descripcion,
                                double score, long keywordsMatched, long keywordsPenalty) {
            this.nombre = nombre;
            this.emoji = emoji;
            this.descripcion = descripcion;
            this.score = score;
            this.keywordsMatched = keywordsMatched;
            this.keywordsPenalty = keywordsPenalty;
        }
    }

    private final String intereses;
    private final List<CarreraResultado> ranking;

    public ResultadoGUI(String intereses, List<CarreraResultado> ranking) {
        this.intereses = intereses;
        this.ranking = ranking;
        construirUI();
    }

    private void construirUI() {
        setTitle("CareerAdvisor MAS — Resultados");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBackground(C_BG);
        setMinimumSize(new Dimension(680, 700));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BG);

        root.add(crearCabecera(), BorderLayout.NORTH);
        root.add(crearCuerpo(), BorderLayout.CENTER);
        root.add(crearPie(), BorderLayout.SOUTH);

        add(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Cabecera
    // -------------------------------------------------------------------------
    private JPanel crearCabecera() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_CARD_TOP,
                    getWidth(), getHeight(), new Color(92, 107, 192));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel titulo = new JLabel("🎓  CareerAdvisor MAS");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("Recomendación de carreras basada en tus intereses");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitulo.setForeground(new Color(200, 210, 255));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 4));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);
        header.add(textos, BorderLayout.NORTH);

        // Intereses
        JPanel interesesPanel = new JPanel(new BorderLayout(6, 0));
        interesesPanel.setOpaque(false);
        interesesPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel lbl = new JLabel("Intereses analizados: ");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(new Color(180, 200, 255));

        JTextArea taIntereses = new JTextArea(intereses);
        taIntereses.setFont(new Font("SansSerif", Font.ITALIC, 12));
        taIntereses.setForeground(Color.WHITE);
        taIntereses.setOpaque(false);
        taIntereses.setEditable(false);
        taIntereses.setLineWrap(true);
        taIntereses.setWrapStyleWord(true);
        taIntereses.setBorder(null);

        interesesPanel.add(lbl, BorderLayout.WEST);
        interesesPanel.add(taIntereses, BorderLayout.CENTER);
        header.add(interesesPanel, BorderLayout.CENTER);

        return header;
    }

    // -------------------------------------------------------------------------
    // Cuerpo: tarjetas con el ranking
    // -------------------------------------------------------------------------
    private JScrollPane crearCuerpo() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(C_BG);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lbl = new JLabel("🏆  Top " + ranking.size() + " carreras recomendadas");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(C_ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 12, 0));
        body.add(lbl);

        double maxScore = ranking.isEmpty() ? 1.0 : Math.max(ranking.get(0).score, 0.001);

        for (int i = 0; i < ranking.size(); i++) {
            body.add(crearTarjetaCarrera(ranking.get(i), i, maxScore));
            body.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBackground(C_BG);
        scroll.getViewport().setBackground(C_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel crearTarjetaCarrera(CarreraResultado c, int posicion, double maxScore) {
        // Panel principal con sombra simulada
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(3, 5, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.setColor(posicion == 0 ? C_CARD_REST : C_CARD_REST);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Medalla / número de posición
        Color medallaColor = posicion == 0 ? C_GOLD : posicion == 1 ? C_SILVER : posicion == 2 ? C_BRONZE : C_BAR_BG;
        String medallaText = posicion == 0 ? "🥇" : posicion == 1 ? "🥈" : posicion == 2 ? "🥉"
            : String.valueOf(posicion + 1);
        JLabel medalla = new JLabel(medallaText, SwingConstants.CENTER);
        medalla.setFont(new Font("SansSerif", posicion < 3 ? Font.PLAIN : Font.BOLD, posicion < 3 ? 26 : 16));
        medalla.setPreferredSize(new Dimension(44, 44));
        card.add(medalla, BorderLayout.WEST);

        // Contenido central
        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.setOpaque(false);

        JLabel nombreLbl = new JLabel(c.emoji + "  " + c.nombre);
        nombreLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        nombreLbl.setForeground(C_TEXT_DARK);

        JLabel descLbl = new JLabel(c.descripcion);
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLbl.setForeground(C_TEXT_GRAY);

        // Barra de progreso
        double pct = maxScore > 0 ? c.score / maxScore : 0;
        JPanel barraPanel = crearBarraProgreso(pct, c.score, c.keywordsMatched, c.keywordsPenalty);

        centro.add(nombreLbl, BorderLayout.NORTH);
        centro.add(descLbl, BorderLayout.CENTER);
        centro.add(barraPanel, BorderLayout.SOUTH);
        card.add(centro, BorderLayout.CENTER);

        return card;
    }

    private JPanel crearBarraProgreso(double pct, double score, long matched, long penalty) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 0, 0, 0));

        // Barra visual
        JPanel barra = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fondo
                g2.setColor(C_BAR_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Relleno
                int w = (int) (getWidth() * pct);
                if (w > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, C_ACCENT,
                        w, 0, new Color(121, 134, 203));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, w, getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        barra.setPreferredSize(new Dimension(0, 10));
        barra.setOpaque(false);

        String penaltyStr = penalty > 0 ? "  ✗" + penalty : "";
        JLabel scoreLabel = new JLabel(
            String.format("%.1f%%  (+%d%s)", score * 100, matched, penaltyStr));
        scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        scoreLabel.setForeground(penalty > 0 ? new Color(198, 40, 40) : C_TEXT_GRAY);
        scoreLabel.setPreferredSize(new Dimension(175, 14));

        p.add(barra, BorderLayout.CENTER);
        p.add(scoreLabel, BorderLayout.EAST);
        return p;
    }

    // -------------------------------------------------------------------------
    // Pie de página
    // -------------------------------------------------------------------------
    private JPanel crearPie() {
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        pie.setBackground(new Color(235, 237, 247));
        pie.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 235)));

        JLabel info = new JLabel("CareerAdvisor MAS · UPM · Sistemas Inteligentes 2025–26");
        info.setFont(new Font("SansSerif", Font.PLAIN, 11));
        info.setForeground(C_TEXT_GRAY);

        JButton cerrar = new JButton("Cerrar");
        cerrar.setFont(new Font("SansSerif", Font.BOLD, 12));
        cerrar.setBackground(C_ACCENT);
        cerrar.setForeground(Color.WHITE);
        cerrar.setFocusPainted(false);
        cerrar.setBorderPainted(false);
        cerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrar.addActionListener(e -> dispose());

        pie.add(info);
        pie.add(cerrar);
        return pie;
    }
}
