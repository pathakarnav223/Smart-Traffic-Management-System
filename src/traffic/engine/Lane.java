package traffic.engine;

import traffic.model.*;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * One single-lane approach to the junction. Handles vehicle generation (Poisson arrivals),
 * car-following (Krauss-style safe speed) and obeying the signal at the stop line.
 */
public class Lane {
    public static final double LENGTH = 80.0;       // visible approach length (m)
    public static final double EXIT = 45.0;         // run-out after the stop line (m)
    public static final double CAPACITY = 10.0;     // vehicles that fit in a full queue
    public static final double FREE_SPEED = 13.9;   // 50 km/h

    private static final double STOP_TARGET = -0.5;
    private static final double MIN_GAP = 1.5;
    private static final double ACCEL = 2.8;
    private static final double DECEL = 4.5;
    private static final double MAX_DECEL = 8.0;
    private static final double TAU = 0.8;

    public final Direction dir;
    private final List<Vehicle> vehicles = new ArrayList<>();      // index 0 = front-most
    private final ArrayDeque<Vehicle> pending = new ArrayDeque<>(); // arrived but no room to enter yet
    private final ArrayDeque<Double> arrivalTimes = new ArrayDeque<>();
    private final ArrayDeque<Double> crossTimes = new ArrayDeque<>();
    private final Random rng;

    private double rate;          // configured arrivals, vehicles per minute
    private double now;
    private int arrived;
    private int passed;
    private double totalDelay;
    private double idleSeconds;   // vehicle-seconds spent stationary before the stop line
    private int maxQueue;
    private int emergencyPassed;
    private double emergencyDelay;
    private BiConsumer<String, String> eventSink = (t, d) -> { };

    public Lane(Direction dir, long seed, double rate) {
        this.dir = dir;
        this.rng = new Random(seed * 31 + dir.ordinal() * 7919L);
        this.rate = rate;
    }

    public void setEventSink(BiConsumer<String, String> sink) {
        this.eventSink = sink;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getRate() {
        return rate;
    }

    // ---------------------------------------------------------------- simulation step

    public void generate(double dt, double time) {
        this.now = time;
        if (rng.nextDouble() < rate / 60.0 * dt) {
            pending.addLast(new Vehicle(dir, randomType(), time));
            arrivalTimes.addLast(time);
            arrived++;
        }
        while (!arrivalTimes.isEmpty() && time - arrivalTimes.peekFirst() > 240) arrivalTimes.pollFirst();

        Vehicle next = pending.peekFirst();
        if (next != null) {
            Vehicle tail = vehicles.isEmpty() ? null : vehicles.get(vehicles.size() - 1);
            double room = tail == null ? Double.MAX_VALUE : (tail.s - tail.type.length) + LENGTH;
            if (room > MIN_GAP + 1.0) {
                pending.pollFirst();
                next.s = -LENGTH;
                double start = next.type.freeSpeed * 0.8;
                next.v = tail == null ? start : Math.min(start, tail.v);
                vehicles.add(next);
            }
        }
    }

    public void move(double dt, double time, LightState light) {
        this.now = time;
        Vehicle leader = null;
        Iterator<Vehicle> it = vehicles.iterator();
        while (it.hasNext()) {
            Vehicle v = it.next();
            double target = v.type.freeSpeed;
            boolean constrained = false;

            if (!v.crossed && !v.committed) {
                double toLine = STOP_TARGET - v.s;
                if (toLine < -1e-9) {
                    v.committed = true;
                } else if (light == LightState.RED) {
                    constrained = true;
                } else if (light == LightState.YELLOW) {
                    double brakeDist = v.v * v.v / (2 * DECEL);
                    if (brakeDist > toLine) v.committed = true;
                    else constrained = true;
                }
                if (constrained) target = Math.min(target, safeSpeed(toLine, 0));
            }
            if (leader != null) {
                double gap = leader.s - leader.type.length - v.s - MIN_GAP;
                target = Math.min(target, safeSpeed(gap, leader.v));
            }

            double nv = target > v.v ? Math.min(target, v.v + ACCEL * dt) : Math.max(target, v.v - MAX_DECEL * dt);
            nv = Math.max(0, nv);
            double ns = v.s + (v.v + nv) / 2 * dt;

            if (leader != null) {
                double maxS = leader.s - leader.type.length - 0.5;
                if (ns > maxS) {
                    ns = Math.max(v.s, maxS);
                    nv = Math.min(nv, leader.v);
                }
            }
            if (constrained && ns > STOP_TARGET) {
                ns = STOP_TARGET;
                nv = 0;
            }
            v.braking = nv < v.v - 0.05 || (nv < 0.5 && v.s < 0);
            v.s = ns;
            v.v = nv;

            if (v.s < 0) {
                if (v.v < 2.0) v.stoppedTime += dt;
                if (v.v < 0.5) idleSeconds += dt;
            }
            if (!v.crossed && v.s >= 0) {
                v.crossed = true;
                double delay = Math.max(0, (time - v.arrivalTime) - (LENGTH + 0.5) / FREE_SPEED);
                if (v.isEmergency()) {
                    emergencyPassed++;
                    emergencyDelay += delay;
                    eventSink.accept("EMERGENCY", String.format("Ambulance from %s cleared the junction (delay %.1f s)",
                            dir.label(), delay));
                } else {
                    passed++;
                    totalDelay += delay;
                }
                crossTimes.addLast(time);
            }
            if (v.s - v.type.length > EXIT) {
                it.remove();
            } else {
                leader = v;
            }
        }
        while (!crossTimes.isEmpty() && time - crossTimes.peekFirst() > 60) crossTimes.pollFirst();
        maxQueue = Math.max(maxQueue, waiting());
    }

    private static double safeSpeed(double gap, double leaderSpeed) {
        double bt = DECEL * TAU;
        return -bt + Math.sqrt(bt * bt + leaderSpeed * leaderSpeed + 2 * DECEL * Math.max(gap, 0));
    }

    private VehicleType randomType() {
        double r = rng.nextDouble();
        if (r < 0.52) return VehicleType.CAR;
        if (r < 0.77) return VehicleType.BIKE;
        if (r < 0.88) return VehicleType.TRUCK;
        return VehicleType.BUS;
    }

    public void addEmergency(double time) {
        pending.addFirst(new Vehicle(dir, VehicleType.AMBULANCE, time));
    }

    // ---------------------------------------------------------------- queries

    public List<Vehicle> vehicles() {
        return vehicles;
    }

    /** Vehicles that are stopped or crawling before the stop line, plus those waiting to enter. */
    public int waiting() {
        int n = pending.size();
        for (Vehicle v : vehicles) if (v.s < 0 && v.v < 2.0) n++;
        return n;
    }

    /** Everything that has not yet crossed the stop line (what the signal still has to serve). */
    public int demand() {
        int n = pending.size();
        for (Vehicle v : vehicles) if (v.s < 0) n++;
        return n;
    }

    public double maxWait() {
        double m = 0;
        for (Vehicle v : vehicles) if (v.s < 0) m = Math.max(m, v.stoppedTime);
        for (Vehicle v : pending) m = Math.max(m, now - v.arrivalTime);
        return m;
    }

    public double avgSpeedKmh() {
        double sum = 0;
        int n = 0;
        for (Vehicle v : vehicles) {
            if (v.s < 0) {
                sum += v.v;
                n++;
            }
        }
        return n == 0 ? 0 : sum / n * 3.6;
    }

    public boolean hasEmergency() {
        for (Vehicle v : vehicles) if (v.isEmergency() && !v.crossed) return true;
        for (Vehicle v : pending) if (v.isEmergency()) return true;
        return false;
    }

    public int pendingCount() {
        return pending.size();
    }

    /** Measured arrival rate (veh/min) over the last 2 minutes, falling back to the configured rate early on. */
    public double measuredRate() {
        if (now < 30) return rate;
        double window = Math.min(240.0, now);
        return arrivalTimes.size() / window * 60.0;
    }

    public int crossedLastMinute() {
        return crossTimes.size();
    }

    public int arrived() {
        return arrived;
    }

    public int passed() {
        return passed;
    }

    public double totalDelay() {
        return totalDelay;
    }

    public double idleSeconds() {
        return idleSeconds;
    }

    public int maxQueue() {
        return maxQueue;
    }

    public int emergencyPassed() {
        return emergencyPassed;
    }

    public double emergencyDelay() {
        return emergencyDelay;
    }

    /** Delay accumulated so far by vehicles that have arrived but not yet crossed. */
    public double unpassedDelay(double time) {
        double sum = 0;
        for (Vehicle v : vehicles) {
            if (!v.crossed && !v.isEmergency()) sum += Math.max(0, time - v.arrivalTime - (LENGTH + 0.5) / FREE_SPEED);
        }
        for (Vehicle v : pending) if (!v.isEmergency()) sum += Math.max(0, time - v.arrivalTime);
        return sum;
    }

    public int unpassedCount() {
        int n = 0;
        for (Vehicle v : vehicles) if (!v.crossed && !v.isEmergency()) n++;
        for (Vehicle v : pending) if (!v.isEmergency()) n++;
        return n;
    }
}
