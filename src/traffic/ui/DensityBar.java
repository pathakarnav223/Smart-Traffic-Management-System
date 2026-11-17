package traffic.ui;

import traffic.model.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Slim rounded progress bar coloured by congestion level. */
public class DensityBar extends JComponent {
    private double value;
    private Level level = Level.LOW;

    public DensityBar() {
        setPreferredSize(new Dimension(100, 10));
        setMinimumSize(new Dimension(40, 10));
    }

    public void set(double value, Level level) {
        this.value = Math.max(0, Math.min(1, value));
        this.level = level;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.antialias(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(new Color(0x232326));
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
        g.setColor(level.color);
        g.fill(new RoundRectangle2D.Double(0, 0, Math.max(h, w * value), h, h, h));
        g.dispose();
    }
}
