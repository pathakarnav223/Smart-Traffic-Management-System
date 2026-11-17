package traffic.engine;

import traffic.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The complete junction model: four lanes, a signal controller, statistics and sampling. */
public class TrafficSimulation {
    public static final double DT = 0.1;                       // seconds of simulated time per step
    public static final double IDLE_LITRES_PER_SEC = 0.8 / 3600.0;  // petrol vehicle idling ~0.8 L/h
    public static final double CO2_KG_PER_LITRE = 2.31;
    private static final int HISTORY_LIMIT = 450;

    private final long seed;
    private final Lane[] lanes = new Lane[4];
    private final SignalController controller = new SignalController();
    private final CongestionAnalyzer analyzer = new CongestionAnalyzer();
    private final List<double[]> history = new ArrayList<>();
    private final Level[] lastLevel = new Level[4];
    private final double[] rates = new double[4];

    private Scenario scenario = Scenario.NORMAL;
    private SimListener listener;
    private double time;
    private double lastSample;
    private double lastSnapshot;

    public TrafficSimulation(long seed) {
        this.seed = seed;
        for (Direction d : Direction.values()) rates[d.ordinal()] = scenario.rate(d);
        build();
    }

    private void build() {
        for (Direction d : Direction.values()) {
            Lane l = new Lane(d, seed, rates[d.ordinal()]);
            l.setEventSink(this::emit);
            lanes[d.ordinal()] = l;
            lastLevel[d.ordinal()] = Level.LOW;
        }
        history.clear();
        time = 0;
        lastSample = 0;
        lastSnapshot = 0;
    }

    /** Restart the clock and empty the roads but keep the mode, scenario and rates. */
    public void reset() {
        ControlMode m = controller.mode();
        boolean prio = controller.emergencyPriority();
        controller.setMode(m);
        controller.setEmergencyPriority(prio);
        controller.reset();
        build();
    }

    public void setListener(SimListener l) {
        this.listener = l;
    }

    // ---------------------------------------------------------------- configuration

    public void setScenario(Scenario s) {
        this.scenario = s;
        if (s != Scenario.CUSTOM) {
            for (Direction d : Direction.values()) setRateInternal(d, s.rate(d));
        }
        emit("SCENARIO", "Scenario set to " + s);
    }

    public void setRate(Direction d, double perMinute) {
        setRateInternal(d, perMinute);
        scenario = Scenario.CUSTOM;
    }

    private void setRateInternal(Direction d, double perMinute) {
        rates[d.ordinal()] = perMinute;
        lanes[d.ordinal()].setRate(perMinute);
    }

    public void setMode(ControlMode m) {
        controller.setMode(m);
        emit("MODE", "Control mode set to " + m.shortName());
    }

    public void setEmergencyPriority(boolean on) {
        controller.setEmergencyPriority(on);
    }

    public boolean emergencyPriority() {
        return controller.emergencyPriority();
    }

    public ControlMode mode() {
        return controller.mode();
    }

    public Scenario scenario() {
        return scenario;
    }

    public void sendEmergency(Direction d) {
        lanes[d.ordinal()].addEmergency(time);
        emit("EMERGENCY", "Ambulance dispatched on the " + d.label() + " approach");
    }

    // ---------------------------------------------------------------- stepping

    public void step() {
        controller.update(DT, this);
        for (Lane l : lanes) l.generate(DT, time);
        for (Lane l : lanes) l.move(DT, time, controller.lightFor(l.dir));
        time += DT;

        if (time - lastSample >= 2.0) {
            lastSample = time;
            double[] row = new double[5];
            row[0] = time;
            for (Lane l : lanes) row[1 + l.dir.ordinal()] = l.waiting();
            history.add(row);
            if (history.size() > HISTORY_LIMIT) history.remove(0);
            checkAlerts();
        }
        if (time - lastSnapshot >= 10.0) {
            lastSnapshot = time;
            if (listener != null) listener.onSnapshot(readings());
        }
    }

    private void checkAlerts() {
        for (CongestionAnalyzer.ApproachReport r : analyzer.analyze(this)) {
            int i = r.dir().ordinal();
            Level prev = lastLevel[i];
            Level cur = r.level();
            if (cur.ordinal() >= Level.HIGH.ordinal() && prev.ordinal() < Level.HIGH.ordinal()) {
                emit("ALERT", String.format("Congestion %s on %s approach (%d waiting)", cur.label.toUpperCase(),
                        r.dir().label(), r.waiting()));
            } else if (cur.ordinal() <= Level.MODERATE.ordinal() && prev.ordinal() >= Level.HIGH.ordinal()) {
                emit("INFO", "Congestion eased on " + r.dir().label() + " approach");
            }
            lastLevel[i] = cur;
        }
    }

    public void emit(String type, String description) {
        if (listener != null) listener.onEvent(new TrafficEvent(LocalDateTime.now(), time, type, description));
    }

    public List<TrafficReading> readings() {
        List<TrafficReading> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (CongestionAnalyzer.ApproachReport r : analyzer.analyze(this)) {
            out.add(new TrafficReading(now, time, scenario.toString(), controller.mode().shortName(),
                    r.dir().label(), r.waiting(), r.density(), r.level().label, r.avgSpeedKmh()));
        }
        return out;
    }

    // ---------------------------------------------------------------- queries used by the controller / UI

    public Lane lane(Direction d) {
        return lanes[d.ordinal()];
    }

    public SignalController controller() {
        return controller;
    }

    public CongestionAnalyzer analyzer() {
        return analyzer;
    }

    public double time() {
        return time;
    }

    public List<double[]> history() {
        return history;
    }

    /** Vehicles still to be served by a phase (the busier of its two approaches). */
    public int demand(Phase p) {
        int m = 0;
        for (Direction d : p.directions()) m = Math.max(m, lane(d).demand());
        return m;
    }

    public double maxWait(Phase p) {
        double m = 0;
        for (Direction d : p.directions()) m = Math.max(m, lane(d).maxWait());
        return m;
    }

    public boolean emergency(Phase p) {
        for (Direction d : p.directions()) if (lane(d).hasEmergency()) return true;
        return false;
    }

    // ---------------------------------------------------------------- statistics

    public int totalPassed() {
        int n = 0;
        for (Lane l : lanes) n += l.passed();
        return n;
    }

    public int throughputPerMinute() {
        int n = 0;
        for (Lane l : lanes) n += l.crossedLastMinute();
        if (time < 60 && time > 1) return (int) Math.round(n * 60.0 / time);
        return n;
    }

    /** Average delay per vehicle (s), counting vehicles that are still waiting. */
    public double averageDelay() {
        double sum = 0;
        int n = 0;
        for (Lane l : lanes) {
            sum += l.totalDelay() + l.unpassedDelay(time);
            n += l.passed() + l.unpassedCount();
        }
        return n == 0 ? 0 : sum / n;
    }

    public int maxQueue() {
        int m = 0;
        for (Lane l : lanes) m = Math.max(m, l.maxQueue());
        return m;
    }

    public double idleFuelLitres() {
        double s = 0;
        for (Lane l : lanes) s += l.idleSeconds();
        return s * IDLE_LITRES_PER_SEC;
    }

    public double co2Kg() {
        return idleFuelLitres() * CO2_KG_PER_LITRE;
    }

    public double averageAmbulanceDelay() {
        double sum = 0;
        int n = 0;
        for (Lane l : lanes) {
            sum += l.emergencyDelay();
            n += l.emergencyPassed();
        }
        return n == 0 ? 0 : sum / n;
    }

    public int ambulancesServed() {
        int n = 0;
        for (Lane l : lanes) n += l.emergencyPassed();
        return n;
    }

    public RunSummary summary() {
        return new RunSummary(LocalDateTime.now(), scenario.toString(), controller.mode().shortName(), time,
                totalPassed(), averageDelay(), maxQueue(), idleFuelLitres(), co2Kg(), averageAmbulanceDelay());
    }
}
