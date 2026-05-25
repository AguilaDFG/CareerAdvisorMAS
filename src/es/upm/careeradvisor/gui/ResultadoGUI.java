package es.upm.careeradvisor.gui;

import es.upm.careeradvisor.agents.AgenteVisualizacion.ResultadoParsed;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Ventana Swing que muestra el ranking global producido por el sistema
 * multiagente. Cada tarjeta indica el dominio KB que la propuso y si
 * proviene del agente ganador del Contract-Net.
 */
public class ResultadoGUI extends JFrame {

    // ── Modelo de datos ──────────────────────────────────────────────────
    public static class CarreraResultado {
        public final String  nombre;
        public final String  emoji;
        public final String  descripcion;
        public final double  score;
        public final long    positive;
        public final long    negative;
        public final String  domain;
        public final boolean winner;   // proviene del KB ganador del Contract-Net

        public CarreraResultado(String nombre, String emoji, String descripcion,
                                double score, long positive, long negative,
                                String domain, boolean winner) {
            this.nombre      = nombre;
            this.emoji       = emoji;
            this.descripcion = descripcion;
            this.score       = score;
            this.positive    = positive;
            this.negative    = negative;
            this.domain      = domain;
            this.winner      = winner;
        }
    }

    // ── Paleta de colores ────────────────────────────────────────────────
    private static final Color C_BG         = new Color(245, 247, 252);
    private static final Color C_HEADER_A   = new Color(63,  81,  181);
    private static final Color C_HEADER_B   = new Color(92,  107, 192);
    private static final Color C_ACCENT     = new Color(63,  81,  181);
    private static final Color C_WINNER     = new Color(255, 193, 7);    // dorado
    private static final Color C_DARK       = new Color(33,  33,  33);
    private static final Color C_GRAY       = new Color(100, 100, 100);
    private static final Color C_BAR_BG     = new Color(220, 224, 240);
    private static final Color C_RED        = new Color(198, 40,  40);
    private static final Color C_SILVER     = new Color(189, 189, 189);
    private static final Color C_BRONZE     = new Color(188, 143, 143);

    // Colores de dominio
    private static final java.util.Map<String, Color> DOMAIN_COLORS = new java.util.HashMap<>();
    static {
        DOMAIN_COLORS.put("tecnologia",  new Color(63,  81,  181));
        DOMAIN_COLORS.put("ciencias",    new Color(56,  142, 60));
        DOMAIN_COLORS.put("humanidades", new Color(245, 124, 0));
        DOMAIN_COLORS.put("salud",       new Color(211, 47,  47));
        DOMAIN_COLORS.put("arte",        new Color(123, 31,  162));
    }

    // ── Constructor ──────────────────────────────────────────────────────
    public ResultadoGUI(ResultadoParsed data) {
        setTitle("CareerAdvisor MAS — Resultados");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBackground(C_BG);
        setMinimumSize(new Dimension(720, 750));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildHeader(data), BorderLayout.NORTH);
        root.add(buildBody(data), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        add(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Cabecera ─────────────────────────────────────────────────────────
    private JPanel buildHeader(ResultadoParsed data) {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, C_HEADER_A, getWidth(), getHeight(), C_HEADER_B));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        h.setBorder(new EmptyBorder(18, 22, 14, 22));

        JLabel title = new JLabel("🎓  CareerAdvisor MAS");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Sistema multiagente · Protocolo FIPA Contract-Net · " +
            data.careers.size() + " carreras evaluadas");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(200, 210, 255));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 3));
        top.setOpaque(false);
        top.add(title);
        top.add(sub);
        h.add(top, BorderLayout.NORTH);

        // Intereses / desintereses
        JPanel info = new JPanel(new GridLayout(0, 1, 0, 3));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(10, 0, 0, 0));
        info.add(labelInfo("Intereses:",    data.intereses,    new Color(200, 210, 255)));
        if (!data.desintereses.isEmpty())
            info.add(labelInfo("Desintereses:", data.desintereses, new Color(255, 200, 200)));
        h.add(info, BorderLayout.CENTER);

        return h;
    }

    private JPanel labelInfo(String label, String value, Color valueColor) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(new Color(180, 200, 255));
        lbl.setPreferredSize(new Dimension(95, 16));

        JLabel val = new JLabel(value.length() > 90 ? value.substring(0, 87) + "…" : value);
        val.setFont(new Font("SansSerif", Font.ITALIC, 11));
        val.setForeground(valueColor);

        p.add(lbl, BorderLayout.WEST);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    // ── Cuerpo ────────────────────────────────────────────────────────────
    private JScrollPane buildBody(ResultadoParsed data) {
        List<CarreraResultado> careers = data.careers;

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(C_BG);
        body.setBorder(new EmptyBorder(14, 18, 14, 18));

        // ── Banner dominio ganador ──────────────────────────────────────
        body.add(buildWinnerBanner(data.winnerDomain));
        body.add(Box.createVerticalStrut(14));

        JLabel sectionLabel = new JLabel("🏆  Ranking global de carreras recomendadas");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        sectionLabel.setForeground(C_ACCENT);
        sectionLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        body.add(sectionLabel);

        double maxScore = careers.isEmpty() ? 1.0
            : Math.max(careers.stream().mapToDouble(c -> c.score).max().orElse(1.0), 0.001);

        for (int i = 0; i < careers.size(); i++) {
            body.add(buildCard(careers.get(i), i, maxScore));
            body.add(Box.createVerticalStrut(8));
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.getViewport().setBackground(C_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ── Banner dominio ganador ────────────────────────────────────────────
    /**
     * Muestra un banner destacado con el dominio ganador del Contract-Net.
     * El AgenteCoordinador eligió este dominio porque su mejor carrera
     * obtuvo el mayor score global entre todos los agentes KB.
     */
    private JPanel buildWinnerBanner(String winnerDomainAgent) {
        String key        = extraerDominioKey(winnerDomainAgent);
        Color  domColor   = DOMAIN_COLORS.getOrDefault(key, C_ACCENT);
        String domDisplay = key.substring(0, 1).toUpperCase() + key.substring(1);

        // Emoji y descripción según el dominio
        String[] info = domainInfo(key);
        String emoji  = info[0];
        String desc   = info[1];

        JPanel banner = new JPanel(new BorderLayout(14, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(3, 5, getWidth() - 4, getHeight() - 4, 14, 14);
                // Fondo con gradiente horizontal
                GradientPaint gp = new GradientPaint(
                    0, 0, domColor,
                    getWidth(), 0, domColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(12, 16, 12, 16));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // Icono / emoji izquierda
        JLabel iconLbl = new JLabel(emoji, SwingConstants.CENTER);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 30));
        iconLbl.setPreferredSize(new Dimension(44, 44));
        banner.add(iconLbl, BorderLayout.WEST);

        // Textos centrales
        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 3));
        txt.setOpaque(false);

        JLabel titleLbl = new JLabel(
            "★  Mayor afinidad detectada: dominio de " + domDisplay);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLbl.setForeground(new Color(220, 235, 255));

        txt.add(titleLbl);
        txt.add(descLbl);
        banner.add(txt, BorderLayout.CENTER);

        // Badge "KB ganador" derecha
        JLabel badge = new JLabel("KB ganador") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setForeground(Color.WHITE);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(88, 30));
        banner.add(badge, BorderLayout.EAST);

        return banner;
    }

    /** Devuelve {emoji, descripción} para cada dominio. */
    private String[] domainInfo(String key) {
        switch (key) {
            case "tecnologia":  return new String[]{"💻",
                "Tus intereses encajan mejor con carreras tecnológicas e ingenierías."};
            case "ciencias":    return new String[]{"🔬",
                "Tus intereses encajan mejor con carreras científicas y experimentales."};
            case "humanidades": return new String[]{"📖",
                "Tus intereses encajan mejor con carreras de humanidades y ciencias sociales."};
            case "salud":       return new String[]{"🏥",
                "Tus intereses encajan mejor con carreras del ámbito de la salud."};
            case "arte":        return new String[]{"🎨",
                "Tus intereses encajan mejor con carreras artísticas y creativas."};
            default:            return new String[]{"🎓",
                "Dominio con mayor afinidad según el análisis multiagente."};
        }
    }

    private JPanel buildCard(CarreraResultado c, int pos, double maxScore) {
        boolean isTop = pos == 0;
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(3, 5, getWidth() - 4, getHeight() - 4, 14, 14);
                // Fondo
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                // Borde izquierdo de color del dominio
                Color dc = DOMAIN_COLORS.getOrDefault(
                    extraerDominioKey(c.domain), C_ACCENT);
                g2.setColor(isTop && c.winner ? C_WINNER : dc);
                g2.fillRoundRect(0, 0, 5, getHeight() - 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));

        // ── Medalla ──
        String medalTxt = pos == 0 ? "🥇" : pos == 1 ? "🥈" : pos == 2 ? "🥉"
            : String.format("%2d.", pos + 1);
        JLabel medal = new JLabel(medalTxt, SwingConstants.CENTER);
        medal.setFont(new Font("SansSerif", pos < 3 ? Font.PLAIN : Font.BOLD, pos < 3 ? 24 : 14));
        medal.setPreferredSize(new Dimension(40, 40));
        card.add(medal, BorderLayout.WEST);

        // ── Centro ──
        JPanel centro = new JPanel(new BorderLayout(0, 3));
        centro.setOpaque(false);

        // Nombre + badge de dominio
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);

        JLabel nameLbl = new JLabel(c.emoji + "  " + c.nombre);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        nameLbl.setForeground(C_DARK);
        nameRow.add(nameLbl);

        nameRow.add(buildDomainBadge(c.domain, c.winner));

        centro.add(nameRow, BorderLayout.NORTH);

        JLabel descLbl = new JLabel(c.descripcion);
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descLbl.setForeground(C_GRAY);
        centro.add(descLbl, BorderLayout.CENTER);

        centro.add(buildBar(c, maxScore), BorderLayout.SOUTH);
        card.add(centro, BorderLayout.CENTER);
        return card;
    }

    private JLabel buildDomainBadge(String domain, boolean winner) {
        String key   = extraerDominioKey(domain);
        Color  bgCol = DOMAIN_COLORS.getOrDefault(key, C_ACCENT);
        String label = (winner ? "★ " : "") + key.toUpperCase();

        JLabel badge = new JLabel(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgCol);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(Color.WHITE);
        badge.setBorder(new EmptyBorder(2, 7, 2, 7));
        badge.setOpaque(false);
        return badge;
    }

    private JPanel buildBar(CarreraResultado c, double maxScore) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(3, 0, 0, 0));

        double pct = maxScore > 0 ? Math.max(0, c.score) / maxScore : 0;
        Color barColor = DOMAIN_COLORS.getOrDefault(extraerDominioKey(c.domain), C_ACCENT);

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BAR_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                int w = (int)(getWidth() * pct);
                if (w > 0) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, w, getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 9));

        String penalty = c.negative > 0 ? "  ✗" + c.negative : "";
        JLabel scoreLbl = new JLabel(
            String.format("%.1f%%  +%d%s", c.score * 100, c.positive, penalty));
        scoreLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        scoreLbl.setForeground(c.negative > 0 ? C_RED : C_GRAY);
        scoreLbl.setPreferredSize(new Dimension(130, 13));

        p.add(bar, BorderLayout.CENTER);
        p.add(scoreLbl, BorderLayout.EAST);
        return p;
    }

    // ── Pie ───────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        footer.setBackground(new Color(235, 237, 247));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 235)));

        JLabel info = new JLabel("CareerAdvisor MAS · UPM · Sistemas Inteligentes 2025–26");
        info.setFont(new Font("SansSerif", Font.PLAIN, 10));
        info.setForeground(C_GRAY);

        JButton btn = new JButton("Cerrar");
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(C_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> dispose());

        footer.add(info);
        footer.add(btn);
        return footer;
    }

    // ── Utilidades ────────────────────────────────────────────────────────
    /** Extrae el nombre corto del dominio del nombre del agente KB. */
    private String extraerDominioKey(String agentName) {
        if (agentName == null) return "desconocido";
        // agentName puede ser "agenteKB_tecnologia@host:port/JADE"
        String lower = agentName.toLowerCase();
        for (String dom : new String[]{"tecnologia","ciencias","humanidades","salud","arte"}) {
            if (lower.contains(dom)) return dom;
        }
        return agentName;
    }
}
