package traffic;

import traffic.db.AppConfig;
import traffic.db.StorageManager;
import traffic.engine.Benchmark;
import traffic.model.RunSummary;
import traffic.model.Scenario;
import traffic.ui.MainFrame;
import traffic.ui.Theme;

import javax.swing.*;

/** Entry point. Run with --selftest for a quick console check of the simulation engine. */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--selftest")) {
            selfTest();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            StorageManager storage = new StorageManager(AppConfig.load());
            new MainFrame(storage).setVisible(true);
        });
    }

    private static void selfTest() {
        for (Scenario sc : new Scenario[]{Scenario.NORMAL, Scenario.MORNING_RUSH, Scenario.EVENT}) {
            System.out.println("Scenario: " + sc);
            for (RunSummary r : Benchmark.run(sc, 600, true, 7, null)) {
                System.out.printf("  %-11s passed=%4d  avg delay=%5.1f s  max queue=%2d  idle fuel=%.2f L%n",
                        r.mode(), r.passed(), r.avgDelaySec(), r.maxQueue(), r.fuelLitres());
            }
        }
    }
}
