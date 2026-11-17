package traffic.ui;

import traffic.db.AppConfig;
import traffic.db.StorageManager;
import traffic.model.RunSummary;
import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Browse what was stored in MySQL / CSV: readings, events and benchmark runs. Export to CSV. */
public class RecordsTab extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int LIMIT = 500;

    private final StorageManager storage;
    private final JLabel status = new JLabel();
    private final JTabbedPane sub = new JTabbedPane();
    private final DefaultTableModel readings = model("Time", "Sim time (s)", "Scenario", "Mode", "Approach",
            "Waiting", "Density", "Level", "Avg speed (km/h)");
    private final DefaultTableModel events = model("Time", "Sim time (s)", "Type", "Description");
    private final DefaultTableModel runs = model("Time", "Scenario", "Mode", "Duration (s)", "Passed",
            "Avg delay (s)", "Max queue", "Fuel (L)", "CO2 (kg)", "Ambulance delay (s)");
    private final JTable[] tables = new JTable[3];

    public RecordsTab(StorageManager storage) {
        super(new BorderLayout(10, 10));
        this.storage = storage;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel bar = Theme.card(new BorderLayout(10, 0));
        status.setForeground(Theme.MUTED);
        bar.add(status, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton refresh = new JButton("Refresh");
        JButton export = new JButton("Export CSV");
        JButton settings = new JButton("Database settings...");
        JButton clear = new JButton("Clear all data");
        buttons.add(refresh);
        buttons.add(export);
        buttons.add(settings);
        buttons.add(clear);
        bar.add(buttons, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        DefaultTableModel[] models = {readings, events, runs};
        String[] names = {"Traffic readings", "Event log", "Benchmark runs"};
        for (int i = 0; i < 3; i++) {
            tables[i] = new JTable(models[i]);
            tables[i].setRowHeight(24);
            tables[i].setFillsViewportHeight(true);
            tables[i].setAutoCreateRowSorter(true);
            JScrollPane sp = new JScrollPane(tables[i]);
            sp.getViewport().setBackground(new Color(0x2A2A2E));
            sub.addTab(names[i], sp);
        }
        add(sub, BorderLayout.CENTER);

        refresh.addActionListener(e -> reload());
        export.addActionListener(e -> exportCurrent());
        settings.addActionListener(e -> {
            new DbSettingsDialog(SwingUtilities.getWindowAncestor(this), storage).setVisible(true);
            reload();
        });
        clear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Delete all stored readings, events and benchmark runs?",
                    "Clear data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.OK_OPTION) {
                try {
                    storage.store().clearAll();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not clear data: " + ex.getMessage());
                }
                reload();
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                reload();
            }
        });
        status.setText(storage.status());
    }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    /** Load the newest rows in the background and fill the three tables. */
    public void reload() {
        status.setText(storage.status());
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[]{storage.store().loadReadings(LIMIT), storage.store().loadEvents(LIMIT),
                        storage.store().loadRuns(100)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    readings.setRowCount(0);
                    for (TrafficReading x : (List<TrafficReading>) r[0]) {
                        readings.addRow(new Object[]{x.timestamp().format(FMT), String.format("%.0f", x.simTime()),
                                x.scenario(), x.mode(), x.direction(), x.waiting(),
                                String.format("%.0f%%", x.density() * 100), x.level(), String.format("%.1f", x.avgSpeedKmh())});
                    }
                    events.setRowCount(0);
                    for (TrafficEvent x : (List<TrafficEvent>) r[1]) {
                        events.addRow(new Object[]{x.timestamp().format(FMT), String.format("%.0f", x.simTime()),
                                x.type(), x.description()});
                    }
                    runs.setRowCount(0);
                    for (RunSummary x : (List<RunSummary>) r[2]) {
                        runs.addRow(new Object[]{x.timestamp().format(FMT), x.scenario(), x.mode(),
                                String.format("%.0f", x.durationSec()), x.passed(), String.format("%.1f", x.avgDelaySec()),
                                x.maxQueue(), String.format("%.2f", x.fuelLitres()), String.format("%.2f", x.co2Kg()),
                                String.format("%.1f", x.avgAmbulanceDelaySec())});
                    }
                } catch (Exception ex) {
                    status.setText("Could not load records: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void exportCurrent() {
        int idx = sub.getSelectedIndex();
        JTable t = tables[idx];
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(new String[]{"readings", "events", "benchmark_runs"}[idx] + "_export.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
            List<String> head = new ArrayList<>();
            for (int c = 0; c < t.getColumnCount(); c++) head.add(csv(t.getColumnName(c)));
            w.println(String.join(",", head));
            for (int r = 0; r < t.getRowCount(); r++) {
                List<String> row = new ArrayList<>();
                for (int c = 0; c < t.getColumnCount(); c++) row.add(csv(String.valueOf(t.getValueAt(r, c))));
                w.println(String.join(",", row));
            }
            JOptionPane.showMessageDialog(this, "Exported " + t.getRowCount() + " rows to " + fc.getSelectedFile().getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private static String csv(String s) {
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
