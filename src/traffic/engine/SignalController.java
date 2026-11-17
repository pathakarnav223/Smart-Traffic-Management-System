package traffic.engine;

import traffic.model.ControlMode;
import traffic.model.Direction;
import traffic.model.LightState;
import traffic.model.Phase;

/**
 * Two-phase signal controller (North-South / East-West) with three strategies:
 * fixed-time, adaptive (sensor-actuated with gap-out, max-green and emergency pre-emption)
 * and Webster auto-timing (green splits recomputed from measured flow).
 */
public class SignalController {
    public enum Stage { GREEN, YELLOW, ALL_RED }

    public static final double YELLOW = 3.0;
    public static final double ALL_RED = 1.5;
    public static final double FIXED_GREEN = 25.0;
    public static final double MIN_GREEN = 8.0;
    public static final double MAX_GREEN = 50.0;
    private static final double MAX_EMERGENCY_HOLD = 45.0;

    private ControlMode mode = ControlMode.FIXED;
    private boolean emergencyPriority = true;
    private Phase phase = Phase.NS;
    private Stage stage = Stage.GREEN;
    private double elapsed;
    private double greenTarget = FIXED_GREEN;
    private boolean preempting;

    public void setMode(ControlMode mode) {
        this.mode = mode;
        this.greenTarget = FIXED_GREEN;
    }

    public ControlMode mode() {
        return mode;
    }

    public void reset() {
        phase = Phase.NS;
        stage = Stage.GREEN;
        elapsed = 0;
        greenTarget = FIXED_GREEN;
        preempting = false;
    }

    public void setEmergencyPriority(boolean on) {
        this.emergencyPriority = on;
    }

    public boolean emergencyPriority() {
        return emergencyPriority;
    }

    public Phase phase() {
        return phase;
    }

    public Stage stage() {
        return stage;
    }

    public double elapsed() {
        return elapsed;
    }

    public double greenTarget() {
        return greenTarget;
    }

    public boolean preempting() {
        return preempting;
    }

    public LightState lightFor(Direction d) {
        if (d.phase() != phase) return LightState.RED;
        return switch (stage) {
            case GREEN -> LightState.GREEN;
            case YELLOW -> LightState.YELLOW;
            case ALL_RED -> LightState.RED;
        };
    }

    public void update(double dt, TrafficSimulation sim) {
        elapsed += dt;
        switch (stage) {
            case GREEN -> {
                if (shouldEndGreen(sim)) {
                    stage = Stage.YELLOW;
                    elapsed = 0;
                }
            }
            case YELLOW -> {
                if (elapsed >= YELLOW) {
                    stage = Stage.ALL_RED;
                    elapsed = 0;
                }
            }
            case ALL_RED -> {
                if (elapsed >= ALL_RED) {
                    phase = phase.other();
                    stage = Stage.GREEN;
                    elapsed = 0;
                    greenTarget = computeGreen(sim);
                    sim.emit("SIGNAL", String.format("%s green started (%s, planned %s)", phase,
                            mode.shortName(), mode == ControlMode.ADAPTIVE ? "sensor-driven"
                                    : String.format("%.0f s", greenTarget)));
                }
            }
        }
    }

    private double computeGreen(TrafficSimulation sim) {
        return switch (mode) {
            case FIXED, ADAPTIVE -> FIXED_GREEN;
            case WEBSTER -> {
                double[] g = sim.analyzer().webster(sim).greens();
                yield g[phase.ordinal()];
            }
        };
    }

    private boolean shouldEndGreen(TrafficSimulation sim) {
        Phase cur = phase;
        Phase oth = cur.other();
        preempting = false;

        if (emergencyPriority && mode != ControlMode.FIXED) {
            if (sim.emergency(cur)) {
                preempting = true;
                return elapsed >= MAX_EMERGENCY_HOLD;
            }
            if (sim.emergency(oth)) {
                preempting = true;
                if (elapsed >= 3.0) {
                    sim.emit("EMERGENCY", "Signal pre-empted: clearing " + cur + " for ambulance on " + oth);
                    return true;
                }
                return false;
            }
        }

        if (mode != ControlMode.ADAPTIVE) return elapsed >= greenTarget;

        if (elapsed < MIN_GREEN) return false;
        if (elapsed >= MAX_GREEN) return true;
        int dCur = sim.demand(cur);
        int dOth = sim.demand(oth);
        if (dCur == 0) return dOth > 0;                      // gap-out: nobody left to serve
        double scoreCur = dCur;
        double scoreOth = dOth + 0.12 * sim.maxWait(oth);     // waiting time prevents starvation
        return scoreOth > scoreCur * 1.3 + 2;
    }
}
