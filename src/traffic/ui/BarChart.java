package traffic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Simple bar chart with value labels. */
public class BarChart extends JPanel {
    private final String title;
    private final String unit;
    private String[] labels = new String[0];
    private double[] values = new double[0];
    private Color[] colors = new Color[0];
    private double fixedMax = -1;

    public BarChart(String title, String unit) {
        this.title = title;
        this.unit = unit;
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(320, 260));
    }

    public void setFixedMax(double max) {
        this.fixedMax = max;
    }

    public void setData(String[] labels, double[] values, Color[] colors) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        Theme.antialias(g);
        int w = getWidth();
        int h = getHeight();
        g.setFont(Theme.font(Font.BOLD, 13f));
        g.setColor(Theme.FG);
        g.drawString(title, 14, 22);

        int left = 16;
        int right = 16;
        int top = 40;
        int bottom = 30;
        int pw = w - left - right;
        int ph = h - top - bottom;
        if (values.length == 0 || pw < 20 || ph < 20) {
            g.setFont(Theme.font(Font.PLAIN, 12f));
            g.setColor(Theme.MUTED);
            g.drawString("No data yet", left, top + 20);
            g.dispose();
            return;
        }
        double max = fixedMax > 0 ? fixedMax : 1;
        if (fixedMax <= 0) for (double v : values) max = Math.max(max, v);
        max *= 1.12;

        int n = values.length;
        double slot = pw / (double) n;
        double bw = Math.min(54, slot * 0.58);
        g.setColor(new Color(0x4A4A52));
        g.drawLine(left, top + ph, left + pw, top + ph);
        for (int i = 0; i < n; i++) {
            double bh = values[i] / max * ph;
            double x = left + slot * i + (slot - bw) / 2;
            double y = top + ph - bh;
            g.setColor(colors[i % colors.length]);
            g.fill(new RoundRectangle2D.Double(x, y, bw, Math.max(bh, 2), 8, 8));
            g.setFont(Theme.font(Font.BOLD, 12f));
            String v = unit.equals("%") ? String.format("%.0f%%", values[i]) : String.format("%.1f%s", values[i], unit);
            int tw = g.getFontMetrics().stringWidth(v);
            g.setColor(Theme.FG);
            g.drawString(v, (int) (x + bw / 2 - tw / 2.0), (int) y - 5);
            g.setFont(Theme.font(Font.PLAIN, 11f));
            g.setColor(Theme.MUTED);
            int lw = g.getFontMetrics().stringWidth(labels[i]);
            g.drawString(labels[i], (int) (x + bw / 2 - lw / 2.0), top + ph + 17);
        }
        g.dispose();
    }
}
