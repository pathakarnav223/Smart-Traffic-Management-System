package traffic.ui;

import traffic.engine.Lane;
import traffic.engine.SignalController;
import traffic.engine.TrafficSimulation;
import traffic.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Animated top-down view of the four-way junction (left-hand traffic, as in India). */
public class IntersectionPanel extends JPanel {
    private static final double LANE_W = 6.0;          // metres
    private static final double BOX = LANE_W;           // half-width of the junction box
    private static final double LO = LANE_W / 2;        // lane centre offset from road centre
    private static final double ARM = Lane.LENGTH + 8;  // road arm length from centre

    private TrafficSimulation sim;
    private double scale;
    private double cx;
    private double cy;
    private long frame;

    public IntersectionPanel(TrafficSimulation sim) {
        this.sim = sim;
        setBackground(new Color(0x1C2420));
        setPreferredSize(new Dimension(720, 640));
    }

    public void setSim(TrafficSimulation sim) {
        this.sim = sim;
    }

    // ---------------------------------------------------------------- geometry helpers

    private static double[] heading(Direction d) {
        return switch (d) {
            case NORTH -> new double[]{0, 1};
            case SOUTH -> new double[]{0, -1};
            case EAST -> new double[]{-1, 0};
            case WEST -> new double[]{1, 0};
        };
    }

    /** Stop-line point of the approach lane, in world metres (y grows downwards). */
    private static double[] stopLine(Direction d) {
        return switch (d) {
            case NORTH -> new double[]{LO, -BOX};
            case SOUTH -> new double[]{-LO, BOX};
            case EAST -> new double[]{BOX, LO};
            case WEST -> new double[]{-BOX, -LO};
        };
    }

    /** Unit vector pointing from the road towards the kerb for this approach. */
    private static double[] outward(Direction d) {
        return switch (d) {
            case NORTH -> new double[]{1, 0};
            case SOUTH -> new double[]{-1, 0};
            case EAST -> new double[]{0, 1};
            case WEST -> new double[]{0, -1};
        };
    }

    private double sx(double wx) {
        return cx + wx * scale;
    }

    private double sy(double wy) {
        return cy + wy * scale;
    }

    // ---------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        frame++;
        Graphics2D g = (Graphics2D) g0.create();
        Theme.antialias(g);
        int w = getWidth();
        int h = getHeight();
        cx = w / 2.0;
        cy = h / 2.0;
        scale = Math.min(w, h) / (2 * (ARM + 4));

        drawGround(g, w, h);
        drawRoads(g);
        drawHeat(g);
        drawCrosswalks(g);
        drawVehicles(g);
        drawSignals(g);
        drawLabels(g);
        drawHud(g, w);
        g.dispose();
    }

    private void drawGround(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x1E2823), 0, h, new Color(0x171E1A)));
        g.fillRect(0, 0, w, h);
        // blocks (buildings) in the four corners
        g.setColor(new Color(0x2B3530));
        double off = BOX + 7;
        double size = ARM - 14;
        for (int sxn : new int[]{-1, 1}) {
            for (int syn : new int[]{-1, 1}) {
                double x0 = sxn < 0 ? -off - size : off;
                double y0 = syn < 0 ? -off - size : off;
                g.fill(new RoundRectangle2D.Double(sx(x0), sy(y0), size * scale, size * scale, 14, 14));
            }
        }
        g.setColor(new Color(0x34413A));
        for (int sxn : new int[]{-1, 1}) {
            for (int syn : new int[]{-1, 1}) {
                double x0 = sxn < 0 ? -off - size : off;
                double y0 = syn < 0 ? -off - size : off;
                for (int i = 1; i < 4; i++) {
                    g.drawLine((int) sx(x0 + size * i / 4), (int) sy(y0), (int) sx(x0 + size * i / 4), (int) sy(y0 + size));
                }
            }
        }
    }

    private void drawRoads(Graphics2D g) {
        g.setColor(new Color(0x3B3B40));
        g.fill(new Rectangle2D.Double(sx(-BOX), sy(-ARM), 2 * BOX * scale, 2 * ARM * scale));
        g.fill(new Rectangle2D.Double(sx(-ARM), sy(-BOX), 2 * ARM * scale, 2 * BOX * scale));
        // kerbs
        g.setColor(new Color(0x5A5A62));
        g.setStroke(new BasicStroke(1.5f));
        for (int sgn : new int[]{-1, 1}) {
            g.draw(new java.awt.geom.Line2D.Double(sx(sgn * BOX), sy(-ARM), sx(sgn * BOX), sy(-BOX)));
            g.draw(new java.awt.geom.Line2D.Double(sx(sgn * BOX), sy(BOX), sx(sgn * BOX), sy(ARM)));
            g.draw(new java.awt.geom.Line2D.Double(sx(-ARM), sy(sgn * BOX), sx(-BOX), sy(sgn * BOX)));
            g.draw(new java.awt.geom.Line2D.Double(sx(BOX), sy(sgn * BOX), sx(ARM), sy(sgn * BOX)));
        }
        // centre dashes
        g.setColor(new Color(0xD9B84A));
        float[] dash = {(float) (4 * scale), (float) (4 * scale)};
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g.draw(new java.awt.geom.Line2D.Double(sx(0), sy(-ARM), sx(0), sy(-BOX)));
        g.draw(new java.awt.geom.Line2D.Double(sx(0), sy(BOX), sx(0), sy(ARM)));
        g.draw(new java.awt.geom.Line2D.Double(sx(-ARM), sy(0), sx(-BOX), sy(0)));
        g.draw(new java.awt.geom.Line2D.Double(sx(BOX), sy(0), sx(ARM), sy(0)));
        g.setStroke(new BasicStroke(1f));
        // stop lines
        g.setColor(new Color(0xF0F0F0));
        g.setStroke(new BasicStroke((float) Math.max(2, 0.6 * scale)));
        for (Direction d : Direction.values()) {
            double[] p = stopLine(d);
            double[] hd = heading(d);
            double[] lat = {Math.abs(hd[1]), Math.abs(hd[0])};  // perpendicular axis (absolute)
            double a = LANE_W / 2;
            g.draw(new java.awt.geom.Line2D.Double(sx(p[0] - lat[0] * a), sy(p[1] - lat[1] * a),
                    sx(p[0] + lat[0] * a), sy(p[1] + lat[1] * a)));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawHeat(Graphics2D g) {
        for (Direction d : Direction.values()) {
            double density = Math.min(1.0, sim.lane(d).waiting() / Lane.CAPACITY);
            Level lv = Level.fromDensity(density);
            double[] p = stopLine(d);
            double[] hd = heading(d);
            double len = Math.max(4, density * Lane.LENGTH);
            double x0 = p[0] - hd[0] * len;
            double y0 = p[1] - hd[1] * len;
            double minX = Math.min(p[0], x0), maxX = Math.max(p[0], x0);
            double minY = Math.min(p[1], y0), maxY = Math.max(p[1], y0);
            double padX = hd[0] == 0 ? LO : 0;
            double padY = hd[1] == 0 ? LO : 0;
            g.setColor(new Color(lv.color.getRed(), lv.color.getGreen(), lv.color.getBlue(), 55));
            g.fill(new Rectangle2D.Double(sx(minX - padX), sy(minY - padY), (maxX - minX + 2 * padX) * scale,
                    (maxY - minY + 2 * padY) * scale));
        }
    }

    private void drawCrosswalks(Graphics2D g) {
        g.setColor(new Color(0xCFCFD4));
        for (Direction d : Direction.values()) {
            double[] p = stopLine(d);
            double[] hd = heading(d);
            boolean vertical = hd[0] == 0;
            double step = 2 * BOX / 8;
            for (int i = 0; i < 8; i++) {
                double pos = -BOX + step * (i + 0.5);
                if (vertical) {
                    double yy = p[1] - hd[1] * 3.4;
                    g.fill(new Rectangle2D.Double(sx(pos - step * 0.25), sy(yy - 1.5), step * 0.5 * scale, 3.0 * scale));
                } else {
                    double xx = p[0] - hd[0] * 3.4;
                    g.fill(new Rectangle2D.Double(sx(xx - 1.5), sy(pos - step * 0.25), 3.0 * scale, step * 0.5 * scale));
                }
            }
        }
    }

    private void drawVehicles(Graphics2D g) {
        for (Direction d : Direction.values()) {
            double[] p = stopLine(d);
            double[] hd = heading(d);
            double angle = Math.atan2(hd[1], hd[0]);
            for (Vehicle v : sim.lane(d).vehicles()) {
                double len = v.type.length;
                double fx = p[0] + hd[0] * v.s;
                double fy = p[1] + hd[1] * v.s;
                double mx = fx - hd[0] * len / 2;
                double my = fy - hd[1] * len / 2;
                double wv = v.type == VehicleType.BIKE ? 1.9 : 3.1;
                AffineTransform old = g.getTransform();
                g.translate(sx(mx), sy(my));
                g.rotate(angle);
                double L = len * scale;
                double W = wv * scale;
                Shape body = new RoundRectangle2D.Double(-L / 2, -W / 2, L, W, W * 0.5, W * 0.5);
                g.setColor(new Color(0, 0, 0, 90));
                g.fill(new RoundRectangle2D.Double(-L / 2 + 1.5, -W / 2 + 1.5, L, W, W * 0.5, W * 0.5));
                g.setColor(v.type.color);
                g.fill(body);
                if (v.type != VehicleType.BIKE) {
                    g.setColor(new Color(0, 0, 0, 110));
                    g.fill(new Rectangle2D.Double(L * 0.12, -W * 0.36, L * 0.2, W * 0.72));   // windscreen
                }
                if (v.type == VehicleType.AMBULANCE) {
                    boolean flash = (frame / 6) % 2 == 0;
                    g.setColor(new Color(0xE53935));
                    g.fill(new Rectangle2D.Double(-L * 0.1, -W * 0.12, L * 0.3, W * 0.24));
                    g.fill(new Rectangle2D.Double(L * 0.0, -W * 0.34, L * 0.1, W * 0.68));
                    g.setColor(flash ? new Color(0xFF3B30) : new Color(0x3D8BFF));
                    g.fill(new Ellipse2D.Double(-L * 0.05, -W * 0.5, W * 0.4, W * 0.4));
                    g.setColor(flash ? new Color(0x3D8BFF) : new Color(0xFF3B30));
                    g.fill(new Ellipse2D.Double(-L * 0.05, W * 0.1, W * 0.4, W * 0.4));
                }
                if (v.braking) {
                    g.setColor(new Color(0xFF2A2A));
                    g.fill(new Rectangle2D.Double(-L / 2, -W / 2 + 1, Math.max(1.5, L * 0.07), W - 2));
                }
                g.setTransform(old);
            }
        }
    }

    private void drawSignals(Graphics2D g) {
        SignalController ctl = sim.controller();
        for (Direction d : Direction.values()) {
            double[] p = stopLine(d);
            double[] hd = heading(d);
            double[] out = outward(d);
            double hx = p[0] + out[0] * (LO + 3.6) - hd[0] * 2.5;
            double hy = p[1] + out[1] * (LO + 3.6) - hd[1] * 2.5;
            double bw = 14, bh = 38;
            double x = sx(hx) - bw / 2;
            double y = sy(hy) - bh / 2;
            g.setColor(new Color(0x101012));
            g.fill(new RoundRectangle2D.Double(x, y, bw, bh, 8, 8));
            g.setColor(new Color(0x55555C));
            g.draw(new RoundRectangle2D.Double(x, y, bw, bh, 8, 8));
            LightState st = ctl.lightFor(d);
            Color[] lamp = {Theme.BAD, Theme.WARN, Theme.OK};
            LightState[] order = {LightState.RED, LightState.YELLOW, LightState.GREEN};
            for (int i = 0; i < 3; i++) {
                boolean on = st == order[i];
                double ly = y + 4 + i * 11;
                if (on) {
                    g.setColor(new Color(lamp[i].getRed(), lamp[i].getGreen(), lamp[i].getBlue(), 70));
                    g.fill(new Ellipse2D.Double(x - 3, ly - 3, bw + 6, 16));
                }
                g.setColor(on ? lamp[i] : new Color(0x2B2B2F));
                g.fill(new Ellipse2D.Double(x + 2, ly, bw - 4, bw - 4));
            }
        }
    }

    private void drawLabels(Graphics2D g) {
        g.setFont(Theme.font(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        for (Direction d : Direction.values()) {
            Lane lane = sim.lane(d);
            double density = Math.min(1.0, lane.waiting() / Lane.CAPACITY);
            Level lv = Level.fromDensity(density);
            String text = d.label().charAt(0) + "  " + lane.waiting() + " waiting";
            double[] p = stopLine(d);
            double[] hd = heading(d);
            double[] out = outward(d);
            double lx = p[0] - hd[0] * (Lane.LENGTH * 0.55) + out[0] * (LO + 9);
            double ly = p[1] - hd[1] * (Lane.LENGTH * 0.55) + out[1] * (LO + 9);
            int tw = fm.stringWidth(text) + 16;
            int th = 22;
            double x = Math.max(4, Math.min(getWidth() - tw - 4, sx(lx) - tw / 2.0));
            double y = Math.max(4, Math.min(getHeight() - th - 4, sy(ly) - th / 2.0));
            g.setColor(new Color(0x18181B, false));
            g.fill(new RoundRectangle2D.Double(x, y, tw, th, 12, 12));
            g.setColor(lv.color);
            g.fill(new Ellipse2D.Double(x + 7, y + 7, 8, 8));
            g.setColor(Theme.FG);
            g.drawString(text, (float) (x + 20), (float) (y + 15));
        }
    }

    private void drawHud(Graphics2D g, int w) {
        SignalController c = sim.controller();
        int t = (int) sim.time();
        String clock = String.format("%02d:%02d", t / 60, t % 60);
        String line1 = "Sim time " + clock + "   |   " + c.mode().shortName() + " control";
        String line2 = c.phase() + " " + c.stage().name().replace('_', ' ').toLowerCase()
                + String.format("  (%.0f s)", c.elapsed());
        if (c.preempting()) line2 += "   EMERGENCY PRIORITY";
        g.setFont(Theme.font(Font.BOLD, 13f));
        FontMetrics fm = g.getFontMetrics();
        int bw = Math.max(fm.stringWidth(line1), fm.stringWidth(line2)) + 24;
        g.setColor(new Color(0x18181B, false));
        g.fill(new RoundRectangle2D.Double(10, 10, bw, 46, 12, 12));
        g.setColor(Theme.FG);
        g.drawString(line1, 22, 29);
        g.setColor(c.preempting() ? Theme.BAD : Theme.MUTED);
        g.setFont(Theme.font(Font.PLAIN, 12f));
        g.drawString(line2, 22, 47);

        g.setFont(Theme.font(Font.BOLD, 11f));
        g.setColor(Theme.MUTED);
        g.drawString("N", (int) (cx - 4), 14 + 6);
        g.drawString("S", (int) (cx - 4), getHeight() - 6);
        g.drawString("W", 6, (int) (cy + 4));
        g.drawString("E", w - 16, (int) (cy + 4));
    }
}
