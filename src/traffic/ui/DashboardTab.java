package traffic.ui;

import traffic.db.StorageManager;
import traffic.engine.CongestionAnalyzer;
import traffic.engine.TrafficSimulation;
import traffic.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Live dashboard: animated junction, controls, per-approach status, KPIs and advisor. */
public class DashboardTab extends JPanel {
    private final TrafficSimulation sim;
    private final StorageManager storage;
    private final IntersectionPanel canvas;
    private final javax.swing.Timer timer;

    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JToggleButton runButton = new JToggleButton("Pause");
    private final JComboBox<String> speedBox = new JComboBox<>(new String[]{"1x", "2x", "5x", "10x", "20x"});
    private final JComboBox<ControlMode> modeBox = new JComboBox<>(ControlMode.values());
    private final JComboBox<Scenario> scenarioBox = new JComboBox<>(Scenario.values());
    private final JCheckBox priorityBox = new JCheckBox("Emergency vehicle priority", true);
    private final JComboBox<Direction> ambulanceBox = new JComboBox<>(Direction.values());
    private final JSlider[] sliders = new JSlider[4];
    private final JLabel[] sliderLabels = new JLabel[4];
    private final JLabel[] approachCount = new JLabel[4];
    private final JLabel[] approachLevel = new JLabel[4];
    private final DensityBar[] approachBar = new DensityBar[4];
    private final JLabel[] kpiValue = new JLabel[6];
    private final JTextArea advisor = new JTextArea();

    private boolean updatingControls;
    private boolean running = true;
    private double speed = 1;
    private double accumulator;
    private long lastNano;
    private int refreshCounter;

    public DashboardTab(TrafficSimulation sim, StorageManager storage) {
        super(new BorderLayout(10, 10));
        this.sim = sim;
        this.storage = storage;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        canvas = new IntersectionPanel(sim);
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        canvas.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        left.add(canvas, BorderLayout.CENTER);
        left.add(buildLog(), BorderLayout.SOUTH);
        add(left, BorderLayout.CENTER);

        JScrollPane side = new JScrollPane(buildSidePanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        side.setBorder(BorderFactory.createEmptyBorder());
        side.getViewport().setBackground(Theme.BG);
        side.getVerticalScrollBar().setUnitIncrement(16);
        side.setPreferredSize(new Dimension(380, 600));
        add(side, BorderLayout.EAST);

        sim.setListener(new traffic.engine.SimListener() {
            @Override
            public void onEvent(TrafficEvent e) {
                int t = (int) e.simTime();
                logModel.add(0, String.format("[%02d:%02d]  %-9s %s", t / 60, t % 60, e.type(), e.description()));
                while (logModel.size() > 200) logModel.remove(logModel.size() - 1);
                storage.logEvent(e);
            }

            @Override
            public void onSnapshot(List<TrafficReading> readings) {
                storage.logReadings(readings);
            }
        });

        wireControls();
        syncSliders();
        lastNano = System.nanoTime();
        timer = new javax.swing.Timer(30, e -> tick());
        timer.start();
        refresh();
    }

    public TrafficSimulation sim() {
        return sim;
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildLog() {
        JList<String> list = new JList<>(logModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        list.setBackground(new Color(0x1C1C1F));
        list.setForeground(Theme.FG);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
                String t = String.valueOf(v);
                Color col = Theme.FG;
                if (t.contains("] ALERT")) col = Theme.ORANGE;
                else if (t.contains("] EMERGENCY")) col = Theme.BAD;
                else if (t.contains("] SIGNAL")) col = Theme.OK;
                else if (t.contains("] MODE") || t.contains("] SCENARIO")) col = Theme.ACCENT;
                else if (t.contains("] INFO")) col = Theme.MUTED;
                c.setForeground(col);
                c.setBackground(new Color(0x1C1C1F));
                return c;
            }
        });
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Theme.BORDER), "Live event log",
                0, 0, Theme.font(Font.BOLD, 12f), Theme.MUTED));
        sp.setPreferredSize(new Dimension(100, 120));
        return sp;
    }

    private JPanel buildSidePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        p.add(controlsCard());
        p.add(Box.createVerticalStrut(10));
        p.add(demandCard());
        p.add(Box.createVerticalStrut(10));
        p.add(approachCard());
        p.add(Box.createVerticalStrut(10));
        p.add(kpiCard());
        p.add(Box.createVerticalStrut(10));
        p.add(advisorCard());
        return p;
    }

    private JPanel controlsCard() {
        JPanel c = Theme.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 0, 3, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        c.add(Theme.title("Simulation controls"), g);

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            sim.reset();
            logModel.clear();
            refresh();
        });
        runButton.addActionListener(e -> {
            running = !runButton.isSelected();
            runButton.setText(running ? "Pause" : "Resume");
        });
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(runButton);
        buttons.add(reset);
        g.gridy++;
        c.add(buttons, g);

        addRow(c, g, "Speed", speedBox);
        addRow(c, g, "Control mode", modeBox);
        addRow(c, g, "Traffic scenario", scenarioBox);
        priorityBox.setOpaque(false);
        priorityBox.setForeground(Theme.FG);
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        c.add(priorityBox, g);
        return wrap(c);
    }

    private JPanel demandCard() {
        JPanel c = Theme.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 0, 2, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 3;
        c.add(Theme.title("Traffic demand (vehicles / minute)"), g);
        for (Direction d : Direction.values()) {
            int i = d.ordinal();
            sliders[i] = new JSlider(0, 30, 8);
            sliders[i].setOpaque(false);
            sliderLabels[i] = new JLabel("8");
            sliderLabels[i].setForeground(Theme.FG);
            sliderLabels[i].setFont(Theme.font(Font.BOLD, 13f));
            JLabel name = new JLabel(d.label());
            name.setForeground(Theme.DIR_COLORS[i]);
            g.gridy++;
            g.gridwidth = 1;
            g.gridx = 0;
            g.weightx = 0;
            c.add(name, g);
            g.gridx = 1;
            g.weightx = 1;
            c.add(sliders[i], g);
            g.gridx = 2;
            g.weightx = 0;
            c.add(sliderLabels[i], g);
        }
        JButton send = new JButton("Send Ambulance");
        send.addActionListener(e -> sim.sendEmergency((Direction) ambulanceBox.getSelectedItem()));
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(ambulanceBox, BorderLayout.WEST);
        row.add(send, BorderLayout.CENTER);
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 3;
        g.insets = new Insets(10, 0, 2, 6);
        c.add(row, g);
        return wrap(c);
    }

    private JPanel approachCard() {
        JPanel c = Theme.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 0, 3, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        c.add(Theme.title("Approach status"), g);
        for (Direction d : Direction.values()) {
            int i = d.ordinal();
            approachCount[i] = new JLabel("0");
            approachCount[i].setForeground(Theme.FG);
            approachCount[i].setFont(Theme.font(Font.BOLD, 13f));
            approachLevel[i] = new JLabel("Low");
            approachLevel[i].setFont(Theme.font(Font.PLAIN, 12f));
            approachBar[i] = new DensityBar();
            JLabel name = new JLabel(d.label());
            name.setForeground(Theme.DIR_COLORS[i]);
            name.setPreferredSize(new Dimension(48, 18));
            g.gridy++;
            g.gridwidth = 1;
            g.gridx = 0;
            g.weightx = 0;
            c.add(name, g);
            g.gridx = 1;
            g.weightx = 1;
            c.add(approachBar[i], g);
            g.gridx = 2;
            g.weightx = 0;
            approachCount[i].setPreferredSize(new Dimension(24, 18));
            c.add(approachCount[i], g);
            g.gridx = 3;
            approachLevel[i].setPreferredSize(new Dimension(68, 18));
            c.add(approachLevel[i], g);
        }
        return wrap(c);
    }

    private JPanel kpiCard() {
        JPanel c = Theme.card(new BorderLayout(0, 8));
        c.add(Theme.title("Live performance"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 3, 8, 8));
        grid.setOpaque(false);
        String[] names = {"Throughput /min", "Avg delay (s)", "Vehicles passed", "Max queue", "Idle fuel (L)", "CO2 (kg)"};
        for (int i = 0; i < 6; i++) {
            JPanel k = new JPanel(new BorderLayout());
            k.setBackground(new Color(0x2C2C30));
            k.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            kpiValue[i] = new JLabel("0");
            kpiValue[i].setFont(Theme.font(Font.BOLD, 18f));
            kpiValue[i].setForeground(Theme.FG);
            k.add(kpiValue[i], BorderLayout.CENTER);
            JLabel n = Theme.muted(names[i]);
            n.setFont(Theme.font(Font.PLAIN, 11f));
            k.add(n, BorderLayout.SOUTH);
            grid.add(k);
        }
        c.add(grid, BorderLayout.CENTER);
        return wrap(c);
    }

    private JPanel advisorCard() {
        JPanel c = Theme.card(new BorderLayout(0, 6));
        c.add(Theme.title("Traffic advisor"), BorderLayout.NORTH);
        advisor.setEditable(false);
        advisor.setLineWrap(true);
        advisor.setWrapStyleWord(true);
        advisor.setOpaque(false);
        advisor.setForeground(Theme.FG);
        advisor.setFont(Theme.font(Font.PLAIN, 12f));
        advisor.setRows(8);
        ((javax.swing.text.DefaultCaret) advisor.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        c.add(advisor, BorderLayout.CENTER);
        return wrap(c);
    }

    private static void addRow(JPanel c, GridBagConstraints g, String label, JComponent comp) {
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        c.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        c.add(comp, g);
    }

    private static JPanel wrap(JPanel c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 400));
        return c;
    }

    // ------------------------------------------------------------------ behaviour

    private void wireControls() {
        speedBox.addActionListener(e -> {
            String s = (String) speedBox.getSelectedItem();
            speed = Double.parseDouble(s.replace("x", ""));
        });
        modeBox.addActionListener(e -> {
            if (!updatingControls) sim.setMode((ControlMode) modeBox.getSelectedItem());
        });
        scenarioBox.setSelectedItem(sim.scenario());
        scenarioBox.addActionListener(e -> {
            if (updatingControls) return;
            Scenario s = (Scenario) scenarioBox.getSelectedItem();
            if (s == Scenario.CUSTOM) {
                sim.setScenario(Scenario.CUSTOM);
            } else {
                sim.setScenario(s);
                syncSliders();
            }
        });
        priorityBox.addActionListener(e -> sim.setEmergencyPriority(priorityBox.isSelected()));
        for (Direction d : Direction.values()) {
            JSlider s = sliders[d.ordinal()];
            s.addChangeListener(e -> {
                sliderLabels[d.ordinal()].setText(String.valueOf(s.getValue()));
                if (updatingControls) return;
                sim.setRate(d, s.getValue());
                updatingControls = true;
                scenarioBox.setSelectedItem(Scenario.CUSTOM);
                updatingControls = false;
            });
        }
    }

    private void syncSliders() {
        updatingControls = true;
        for (Direction d : Direction.values()) {
            int v = (int) Math.round(sim.lane(d).getRate());
            sliders[d.ordinal()].setValue(v);
            sliderLabels[d.ordinal()].setText(String.valueOf(v));
        }
        updatingControls = false;
    }

    private void tick() {
        long now = System.nanoTime();
        double real = Math.min(0.1, (now - lastNano) / 1e9);
        lastNano = now;
        if (running) {
            accumulator += real * speed;
            int steps = 0;
            while (accumulator >= TrafficSimulation.DT && steps < 250) {
                sim.step();
                accumulator -= TrafficSimulation.DT;
                steps++;
            }
        }
        if (++refreshCounter % 3 == 0) refresh();
        canvas.repaint();
    }

    /** Advance the simulation without the timer (used for screenshots and tests). */
    public void advance(double simSeconds) {
        long steps = Math.round(simSeconds / TrafficSimulation.DT);
        for (long i = 0; i < steps; i++) sim.step();
        refresh();
    }

    public void refresh() {
        List<CongestionAnalyzer.ApproachReport> reports = sim.analyzer().analyze(sim);
        for (CongestionAnalyzer.ApproachReport r : reports) {
            int i = r.dir().ordinal();
            approachBar[i].set(r.density(), r.level());
            approachCount[i].setText(String.valueOf(r.waiting()));
            approachLevel[i].setText(r.level().label);
            approachLevel[i].setForeground(r.level().color);
        }
        kpiValue[0].setText(String.valueOf(sim.throughputPerMinute()));
        kpiValue[1].setText(String.format("%.1f", sim.averageDelay()));
        kpiValue[2].setText(String.valueOf(sim.totalPassed()));
        kpiValue[3].setText(String.valueOf(sim.maxQueue()));
        kpiValue[4].setText(String.format("%.2f", sim.idleFuelLitres()));
        kpiValue[5].setText(String.format("%.2f", sim.co2Kg()));
        StringBuilder sb = new StringBuilder();
        for (String s : sim.analyzer().recommendations(sim)) sb.append("• ").append(s).append('\n');
        String text = sb.toString().trim();
        if (!text.equals(advisor.getText())) advisor.setText(text);
        updatingControls = true;
        modeBox.setSelectedItem(sim.mode());
        updatingControls = false;
    }

    public void stop() {
        timer.stop();
    }
}
