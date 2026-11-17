package traffic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Simple multi-series line chart drawn with Java2D (no external chart library needed). */
public class LineChart extends JPanel {
    private final String title;
    private final String yLabel;
    private final String[] names;
    private final Color[] colors;
    private List<double[]> rows = new ArrayList<>();   // each row: x, y1..yn

    public LineChart(String title, String yLabel, String[] names, Color[] colors) {
        this.title = title;
        this.yLabel = yLabel;
        this.names = names;
        this.colors = colors;
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(520, 260));
    }

    public void setData(List<double[]> data) {
        this.rows = new ArrayList<>(data);
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

        // legend
        g.setFont(Theme.font(Font.PLAIN, 11f));
        FontMetrics fm = g.getFontMetrics();
        int lx = w - 14;
        for (int i = names.length - 1; i >= 0; i--) {
            int tw = fm.stringWidth(names[i]);
            lx -= tw;
            g.setColor(Theme.MUTED);
            g.drawString(names[i], lx, 21);
            lx -= 14;
            g.setColor(colors[i]);
            g.fillRoundRect(lx, 13, 10, 10, 4, 4);
            lx -= 12;
        }

        int left = 44;
        int right = 14;
        int top = 36;
        int bottom = 30;
        int pw = w - left - right;
        int ph = h - top - bottom;
        if (pw < 20 || ph < 20) {
            g.dispose();
            return;
        }

        double maxY = 4;
        double minX = 0;
        double maxX = 60;
        if (!rows.isEmpty()) {
            minX = rows.get(0)[0];
            maxX = Math.max(rows.get(rows.size() - 1)[0], minX + 60);
            for (double[] r : rows) for (int i = 1; i < r.length; i++) maxY = Math.max(maxY, r[i]);
        }
        maxY = Math.ceil(maxY / 4.0) * 4.0;

        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            int y = top + ph - ph * i / 4;
            g.setColor(new Color(0x4A4A52));
            g.drawLine(left, y, left + pw, y);
            g.setColor(Theme.MUTED);
            String lab = String.valueOf((int) Math.round(maxY * i / 4));
            g.drawString(lab, left - 8 - g.getFontMetrics().stringWidth(lab), y + 4);
        }
        for (int i = 0; i <= 4; i++) {
            double t = minX + (maxX - minX) * i / 4;
            int x = left + pw * i / 4;
            String lab = String.format("%d:%02d", (int) t / 60, (int) t % 60);
            g.setColor(Theme.MUTED);
            g.drawString(lab, x - g.getFontMetrics().stringWidth(lab) / 2, top + ph + 18);
        }
        g.setColor(Theme.MUTED);
        g.drawString(yLabel, left + 4, top - 6);

        if (rows.size() < 2) {
            g.setColor(Theme.MUTED);
            g.drawString("Collecting data...", left + pw / 2 - 40, top + ph / 2);
            g.dispose();
            return;
        }
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int s = 0; s < names.length; s++) {
            Path2D path = new Path2D.Double();
            boolean first = true;
            for (double[] r : rows) {
                double x = left + (r[0] - minX) / (maxX - minX) * pw;
                double y = top + ph - r[s + 1] / maxY * ph;
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else path.lineTo(x, y);
            }
            g.setColor(colors[s]);
            g.draw(path);
        }
        g.dispose();
    }
}
