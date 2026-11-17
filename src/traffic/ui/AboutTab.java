package traffic.ui;

import javax.swing.*;
import java.awt.*;

/** Project information page: team, features and how the system works. */
public class AboutTab extends JPanel {
    public AboutTab() {
        super(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String html = """
                <html><body style='font-family:SansSerif; color:#E8E8EA; font-size:13px; margin:14px'>
                <h1 style='margin-bottom:0'>Smart Traffic Management System</h1>
                <p style='color:#A2A2AB; margin-top:2px'>Java PBL Project &nbsp;|&nbsp; Java 17+ &nbsp;|&nbsp; Swing &nbsp;|&nbsp; JDBC / MySQL</p>

                <h3>Team</h3>
                <table cellpadding='3'>
                <tr><td>1.</td><td>Arnav Pathak</td><td style='color:#A2A2AB'>RA2511056030033</td></tr>
                <tr><td>2.</td><td>Atharv Kapoor</td><td style='color:#A2A2AB'>RA2511056030037</td></tr>
                <tr><td>3.</td><td>Rushil Nishad</td><td style='color:#A2A2AB'>RA2511056030019</td></tr>
                </table>
                <p style='color:#A2A2AB'>Branch: Data Science &nbsp;/&nbsp; Semester 3<br>
                SRM Institute of Science and Technology, Delhi NCR Campus</p>

                <h3>How the system works</h3>
                <ol>
                <li><b>Traffic data input</b> - simulated Poisson arrivals per approach (scenario presets or sliders).</li>
                <li><b>Data processing</b> - vehicles move with a car-following model; queues and speeds are measured every step.</li>
                <li><b>Congestion analysis</b> - density = waiting vehicles / lane capacity, classed Low / Moderate / High / Severe.</li>
                <li><b>Signal management</b> - Fixed-time, Adaptive (sensor-actuated, with emergency pre-emption) or Webster auto-timing.</li>
                <li><b>Database storage</b> - readings, events and benchmark runs go to MySQL through JDBC (CSV fallback).</li>
                <li><b>Results display</b> - this Swing dashboard, live charts, records browser and benchmark.</li>
                </ol>

                <h3>Things to try in the demo</h3>
                <ul>
                <li>Choose <i>Stadium event (East heavy)</i> and watch the East queue turn red under Fixed-time, then switch to Adaptive.</li>
                <li>Press <i>Send ambulance</i> - with Adaptive and priority on, the signal pre-empts and clears the way; in Fixed-time it has to wait.</li>
                <li>Open <i>Analytics</i> and run the benchmark: all three modes face identical traffic, so the comparison is fair.</li>
                <li>Open <i>Records</i> to show the data stored in the database and export it as CSV.</li>
                </ul>

                <h3>Notes</h3>
                <p style='color:#A2A2AB'>Traffic drives on the left (as in India). Fuel and CO2 figures are estimates:
                idling at about 0.8 L/hour per vehicle and 2.31 kg CO2 per litre of petrol.</p>
                </body></html>
                """;
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBackground(Theme.PANEL);
        JScrollPane sp = new JScrollPane(pane);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(sp, BorderLayout.CENTER);
    }
}
