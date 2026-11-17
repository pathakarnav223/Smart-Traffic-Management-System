package traffic.engine;

import traffic.model.Direction;
import traffic.model.Level;
import traffic.model.Phase;

import java.util.ArrayList;
import java.util.List;

/** Congestion analysis: density per approach, congestion levels and Webster signal timing. */
public class CongestionAnalyzer {
    /** Saturation flow of one lane in vehicles per minute (measured from the simulator). */
    public static final double SATURATION_FLOW = 28.0;
    /** Lost time per cycle: 2 x (yellow + all-red) + start-up lost time. */
    public static final double LOST_TIME = 2 * (SignalController.YELLOW + SignalController.ALL_RED) + 2;

    public record ApproachReport(Direction dir, int waiting, double density, Level level,
                                 double avgSpeedKmh, double maxWait, double arrivalRate) {
    }

    public record Plan(double cycle, double[] greens, double flowRatio, boolean oversaturated) {
    }

    public List<ApproachReport> analyze(TrafficSimulation sim) {
        List<ApproachReport> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            Lane l = sim.lane(d);
            int waiting = l.waiting();
            double density = Math.min(1.0, waiting / Lane.CAPACITY);
            out.add(new ApproachReport(d, waiting, density, Level.fromDensity(density),
                    l.avgSpeedKmh(), l.maxWait(), l.measuredRate()));
        }
        return out;
    }

    /** Webster's method: C0 = (1.5 L + 5) / (1 - Y); green split proportional to flow ratio. */
    public Plan webster(TrafficSimulation sim) {
        double[] y = new double[2];
        for (Phase p : Phase.values()) {
            double q = 0;
            for (Direction d : p.directions()) q = Math.max(q, sim.lane(d).measuredRate());
            y[p.ordinal()] = q / SATURATION_FLOW;
        }
        double Y = y[0] + y[1];
        double cycle;
        boolean over = Y >= 0.9;
        if (over) cycle = 100;
        else cycle = Math.max(50, Math.min(100, (1.5 * LOST_TIME + 5) / (1 - Y)));
        double[] g = new double[2];
        double total = cycle - LOST_TIME;
        for (int i = 0; i < 2; i++) {
            double share = Y <= 0.0001 ? 0.5 : y[i] / Y;
            g[i] = Math.max(10, Math.min(70, total * share));
        }
        return new Plan(cycle, g, Y, over);
    }

    public List<String> recommendations(TrafficSimulation sim) {
        List<String> out = new ArrayList<>();
        List<ApproachReport> reports = analyze(sim);
        ApproachReport worst = reports.get(0);
        for (ApproachReport r : reports) if (r.density() > worst.density()) worst = r;

        out.add(String.format("Busiest approach: %s - %s (%d waiting, %.0f%% density).",
                worst.dir().label(), worst.level().label.toLowerCase(), worst.waiting(), worst.density() * 100));

        Plan plan = webster(sim);
        out.add(String.format("Webster plan: cycle %.0f s, N-S green %.0f s, E-W green %.0f s (flow ratio Y = %.2f).",
                plan.cycle(), plan.greens()[0], plan.greens()[1], plan.flowRatio()));
        if (plan.oversaturated()) {
            out.add("Demand is near or above junction capacity - delays will grow unless demand is diverted.");
        }
        double gap = Math.abs(plan.greens()[0] - plan.greens()[1]);
        if (sim.mode() == traffic.model.ControlMode.FIXED && gap > 8) {
            Phase favour = plan.greens()[0] > plan.greens()[1] ? Phase.NS : Phase.EW;
            out.add("Traffic is unbalanced: " + favour + " needs a longer green than the fixed 25 s. "
                    + "Switch to Adaptive or Webster control.");
        }
        for (ApproachReport r : reports) {
            if (r.level() == Level.SEVERE) {
                out.add("Severe congestion on " + r.dir().label() + ": extend its green or divert vehicles upstream.");
            }
        }
        for (Direction d : Direction.values()) {
            if (sim.lane(d).hasEmergency()) {
                out.add("Emergency vehicle on " + d.label() + " approach"
                        + (sim.mode() == traffic.model.ControlMode.FIXED || !sim.emergencyPriority()
                        ? " - no priority active, it must wait for the normal cycle."
                        : " - priority green is being given."));
            }
        }
        return out;
    }
}
