package traffic.engine;

import traffic.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Headless comparison of the three control modes. Every mode sees exactly the same random
 * arrivals (same seed), so differences in the results come only from the signal logic.
 */
public final class Benchmark {
    private Benchmark() {
    }

    public static List<RunSummary> run(Scenario scenario, double durationSec, boolean withAmbulances,
                                       long seed, IntConsumer progressPercent) {
        List<RunSummary> out = new ArrayList<>();
        ControlMode[] modes = ControlMode.values();
        for (int m = 0; m < modes.length; m++) {
            TrafficSimulation sim = new TrafficSimulation(seed);
            sim.setScenario(scenario);
            sim.setMode(modes[m]);
            sim.setEmergencyPriority(true);
            double nextAmbulance = 75;
            int amb = 0;
            long steps = Math.round(durationSec / TrafficSimulation.DT);
            for (long i = 0; i < steps; i++) {
                if (withAmbulances && sim.time() >= nextAmbulance) {
                    sim.sendEmergency(Direction.values()[amb++ % 4]);
                    nextAmbulance += 120;
                }
                sim.step();
                if (progressPercent != null && i % 200 == 0) {
                    progressPercent.accept((int) ((m + (double) i / steps) / modes.length * 100));
                }
            }
            out.add(sim.summary());
        }
        if (progressPercent != null) progressPercent.accept(100);
        return out;
    }
}
