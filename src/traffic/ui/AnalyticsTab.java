package traffic.ui;

import traffic.db.StorageManager;
import traffic.engine.Benchmark;
import traffic.engine.CongestionAnalyzer;
import traffic.engine.TrafficSimulation;
import traffic.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Live charts plus the Fixed vs Adaptive vs Webster benchmark. */
public class AnalyticsTab extends JPanel {
    private final DashboardTab dashboard;
    private final StorageManager storage;
    private final LineChart queueChart;
    private final BarChart densityChart = new BarChart("Current congestion density", "%");
    private final BarChart delayChart = new BarChart("Average delay per vehicle (benchmark)", " s");

    private final JComboBox<Scenario> scenarioBox = new JComboBox<>(Scenario.values());
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(15, 3, 60, 1));
    private final JCheckBox ambulances = new JCheckBox("Include ambulances (one every 2 min)", true);
    private final JButton runButton = new JButton("Run benchmark");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel verdict = new JLabel("Pick a scenario and run the benchmark to compare the three control modes.");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Control mode", "Vehicles passed", "Avg delay (s)", "Max queue", "Idle fuel (L)",
                    "CO2 (kg)", "Ambulance delay (s)", "Delay vs fixed"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final javax.swing.Timer refresher;

    public AnalyticsTab(DashboardTab dashboard, StorageManager storage) {
        super(new BorderLayout(10, 10));
        this.dashboard = dashboard;
        this.storage = storage;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        queueChart = new LineChart("Vehicles waiting per approach (live)", "vehicles",
                new String[]{"North", "South", "East", "West"}, Theme.DIR_COLORS);
        JPanel top = new JPanel(new GridLayout(1, 2, 10, 0));
        top.setOpaque(false);
        top.add(wrapChart(queueChart));
        top.add(wrapChart(densityChart));
        densityChart.setFixedMax(100);

        add(top, BorderLayout.CENTER);
        add(buildBenchmark(), BorderLayout.SOUTH);

        refresher = new javax.swing.Timer(1000, e -> refreshCharts());
        refresher.start();
        refreshCharts();
    }

    private static JComponent wrapChart(JComponent c) {
        JPanel p = Theme.card(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public void refreshCharts() {
        TrafficSimulation sim = dashboard.sim();
        queueChart.setData(sim.history());
        List<CongestionAnalyzer.ApproachReport> reports = sim.analyzer().analyze(sim);
        String[] labels = new String[4];
        double[] vals = new double[4];
        Color[] cols = new Color[4];
        for (CongestionAnalyzer.ApproachReport r : reports) {
            int i = r.dir().ordinal();
            labels[i] = r.dir().label();
            vals[i] = r.density() * 100;
            cols[i] = r.level().color;
        }
        densityChart.setData(labels, vals, cols);
    }

    private JPanel buildBenchmark() {
        JPanel p = Theme.card(new BorderLayout(10, 10));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        head.add(Theme.title("Benchmark: same traffic, three signal strategies"));
        head.add(scenarioBox);
        head.add(Theme.muted("Duration (min):"));
        head.add(minutes);
        ambulances.setOpaque(false);
        ambulances.setForeground(Theme.FG);
        head.add(ambulances);
        head.add(runButton);
        progress.setPreferredSize(new Dimension(120, 18));
        head.add(progress);
        scenarioBox.removeItem(Scenario.CUSTOM);
        scenarioBox.setSelectedItem(Scenario.MORNING_RUSH);
        p.add(head, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(500, 118));
        sp.getViewport().setBackground(new Color(0x2A2A2E));

        JPanel mid = new JPanel(new BorderLayout(10, 6));
        mid.setOpaque(false);
        mid.add(sp, BorderLayout.CENTER);
        verdict.setForeground(Theme.MUTED);
        verdict.setFont(Theme.font(Font.BOLD, 13f));
        mid.add(verdict, BorderLayout.SOUTH);
        delayChart.setPreferredSize(new Dimension(380, 190));
        p.add(mid, BorderLayout.CENTER);
        p.add(wrapChart(delayChart), BorderLayout.EAST);

        runButton.addActionListener(e -> runBenchmark());
        return p;
    }

    private void runBenchmark() {
        Scenario sc = (Scenario) scenarioBox.getSelectedItem();
        double duration = ((Number) minutes.getValue()).intValue() * 60.0;
        boolean amb = ambulances.isSelected();
        runButton.setEnabled(false);
        progress.setValue(0);
        verdict.setForeground(Theme.MUTED);
        verdict.setText("Running three simulations of " + (int) (duration / 60) + " minutes each...");

        new SwingWorker<List<RunSummary>, Integer>() {
            @Override
            protected List<RunSummary> doInBackground() {
                return Benchmark.run(sc, duration, amb, System.nanoTime() & 0xFFFFF, this::publishProgress);
            }

            private void publishProgress(int pct) {
                publish(pct);
            }

            @Override
            protected void process(List<Integer> chunks) {
                progress.setValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                progress.setValue(100);
                try {
                    showResults(get());
                } catch (Exception ex) {
                    verdict.setForeground(Theme.BAD);
                    verdict.setText("Benchmark failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showResults(List<RunSummary> runs) {
        tableModel.setRowCount(0);
        RunSummary fixed = runs.get(0);
        RunSummary best = fixed;
        String[] labels = new String[runs.size()];
        double[] vals = new double[runs.size()];
        Color[] cols = {Theme.MUTED, Theme.ACCENT, Theme.OK};
        for (int i = 0; i < runs.size(); i++) {
            RunSummary r = runs.get(i);
            String vs = i == 0 ? "baseline"
                    : String.format("%+.0f%%", (r.avgDelaySec() - fixed.avgDelaySec()) / Math.max(0.01, fixed.avgDelaySec()) * 100);
            tableModel.addRow(new Object[]{r.mode(), r.passed(), String.format("%.1f", r.avgDelaySec()), r.maxQueue(),
                    String.format("%.2f", r.fuelLitres()), String.format("%.2f", r.co2Kg()),
                    r.avgAmbulanceDelaySec() > 0 ? String.format("%.1f", r.avgAmbulanceDelaySec()) : "-", vs});
            labels[i] = r.mode();
            vals[i] = r.avgDelaySec();
            if (r.avgDelaySec() < best.avgDelaySec()) best = r;
            storage.logRun(r);
        }
        delayChart.setData(labels, vals, cols);
        double gain = (fixed.avgDelaySec() - best.avgDelaySec()) / Math.max(0.01, fixed.avgDelaySec()) * 100;
        verdict.setForeground(Theme.OK);
        if (best == fixed) {
            verdict.setText("Fixed-time was best for this scenario - demand is light and balanced.");
        } else {
            verdict.setText(String.format("%s cut average delay by %.0f%% and idle fuel by %.0f%% compared with fixed-time.",
                    best.mode(), gain, (fixed.fuelLitres() - best.fuelLitres()) / Math.max(0.0001, fixed.fuelLitres()) * 100));
        }
    }

    public void stop() {
        refresher.stop();
    }
}
