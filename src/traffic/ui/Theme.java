package traffic.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/** Dark theme matching the project presentation, built on the Nimbus look and feel. */
public final class Theme {
    public static final Color BG = new Color(0x232326);
    public static final Color PANEL = new Color(0x2C2C30);
    public static final Color CARD = new Color(0x36363B);
    public static final Color BORDER = new Color(0x4A4A52);
    public static final Color FG = new Color(0xE8E8EA);
    public static final Color MUTED = new Color(0xA2A2AB);
    public static final Color ACCENT = new Color(0x4DA3FF);
    public static final Color OK = new Color(0x3DDC84);
    public static final Color WARN = new Color(0xFFC83D);
    public static final Color ORANGE = new Color(0xFF9A3D);
    public static final Color BAD = new Color(0xFF5A52);

    /** One colour per approach: North, South, East, West. */
    public static final Color[] DIR_COLORS = {
            new Color(0x4DA3FF), new Color(0xFF9A3D), new Color(0x3DDC84), new Color(0xB48CFF)
    };

    private Theme() {
    }

    public static void install() {
        UIManager.put("control", PANEL);
        UIManager.put("info", CARD);
        UIManager.put("nimbusBase", new Color(0x3C3C42));
        UIManager.put("nimbusBlueGrey", new Color(0x45454C));
        UIManager.put("nimbusLightBackground", new Color(0x2A2A2E));
        UIManager.put("nimbusSelectionBackground", new Color(0x2F6FB5));
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusDisabledText", new Color(0x77777F));
        UIManager.put("nimbusOrange", ACCENT);
        UIManager.put("text", FG);
        UIManager.put("controlText", FG);
        UIManager.put("infoText", FG);
        UIManager.put("menuText", FG);
        UIManager.put("textForeground", FG);
        UIManager.put("textBackground", new Color(0x2A2A2E));
        UIManager.put("Table.alternateRowColor", new Color(0x303034));
        UIManager.put("Table.background", new ColorUIResource(0x2A2A2E));
        UIManager.put("Table.foreground", new ColorUIResource(FG));
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("ToolTip.background", new ColorUIResource(CARD));
        UIManager.put("ToolTip.foreground", new ColorUIResource(FG));
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // keep default look and feel
        }
        Font base = font(Font.PLAIN, 13f);
        for (Object key : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            if (key.toString().endsWith(".font")) UIManager.put(key, base);
        }
    }

    public static Font font(int style, float size) {
        return new Font("SansSerif", style, Math.round(size)).deriveFont(style, size);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    public static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CARD);
        p.setBorder(cardBorder());
        return p;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 14f));
        l.setForeground(FG);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 12f));
        l.setForeground(MUTED);
        return l;
    }

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
