package traffic.ui;

import traffic.db.StorageManager;
import traffic.engine.TrafficSimulation;

import javax.swing.*;
import java.awt.*;

/** The whole application content: header, tabs and footer. Kept separate from the frame so it can be tested. */
public class AppPanel extends JPanel {
    private final DashboardTab dashboard;
    private final AnalyticsTab analytics;
    private final RecordsTab records;
    private final JTabbedPane tabs = new JTabbedPane();

    public AppPanel(StorageManager storage) {
        super(new BorderLayout());
        setBackground(Theme.BG);

        dashboard = new DashboardTab(new TrafficSimulation(System.nanoTime() & 0xFFFFF), storage);
        analytics = new AnalyticsTab(dashboard, storage);
        records = new RecordsTab(storage);

        tabs.addTab("Live Dashboard", dashboard);
        tabs.addTab("Analytics & Benchmark", analytics);
        tabs.addTab("Database Records", records);
        tabs.addTab("About", new AboutTab());
        tabs.setFont(Theme.font(Font.BOLD, 13f));

        add(header(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private JComponent header() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(0x1B1B1E));
        h.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel t = new JLabel("Smart Traffic Management System");
        t.setFont(Theme.font(Font.BOLD, 20f));
        t.setForeground(Theme.FG);
        JLabel s = new JLabel("Java PBL Project  |  SRM Institute of Science and Technology, Delhi NCR");
        s.setForeground(Theme.MUTED);
        h.add(t, BorderLayout.WEST);
        h.add(s, BorderLayout.EAST);
        return h;
    }

    public DashboardTab dashboard() {
        return dashboard;
    }

    public AnalyticsTab analytics() {
        return analytics;
    }

    public RecordsTab records() {
        return records;
    }

    public JTabbedPane tabs() {
        return tabs;
    }

    public void shutdown() {
        dashboard.stop();
        analytics.stop();
    }
}
