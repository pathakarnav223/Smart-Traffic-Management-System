# Smart Traffic Management System (Java Swing)

Java PBL project - SRM Institute of Science and Technology, Delhi NCR Campus
Team: Arnav Pathak (RA2511056030033), Atharv Kapoor (RA2511056030037), Rushil Nishad (RA2511056030019)

A desktop application that simulates a four-way junction, detects congestion, controls the signals
with three different strategies, stores everything in MySQL through JDBC, and proves with a
benchmark that smarter signal logic reduces delay and fuel waste. It follows the six modules in the
project presentation: **input -> processing -> congestion analysis -> signal management -> database storage -> results display.**

## Run it

Requirements: **JDK 17 or newer**. No other libraries are needed.

| Windows | Linux / macOS |
|---|---|
| double-click `run.bat` | `./run.sh` (run `chmod +x run.sh` once) |

Or from any IDE (IntelliJ / Eclipse / NetBeans): mark `src` as the source folder and run `traffic.Main`.

Quick engine check without the GUI: `java -cp out traffic.Main --selftest`

## MySQL setup (optional - the app works without it)

1. Install MySQL and make sure the server is running.
2. Download **mysql-connector-j** (the JDBC driver jar) from the MySQL site and copy the jar into the `lib/` folder.
3. Start the app, open **Database Records -> Database settings**, enter your URL / user / password,
   press **Test connection**, then **Save & apply**.

The three tables are created automatically (`schema.sql` shows the structure). If MySQL or the driver is missing,
the app automatically falls back to CSV files in `data/` and shows why in the status bar, so the demo never breaks.
The settings are saved in `config.properties`.

> The MySQL code path could not be tested against a live server while building this project, so run
> **Test connection** once before your presentation. The CSV fallback was tested.

## What is in the application

**Live Dashboard**
- Animated top-down junction: individual cars, two-wheelers, buses, trucks and ambulances, signal heads, zebra crossings, brake lights. Traffic drives on the left, as in India.
- Each approach shows its queue and a colour heat strip (Low / Moderate / High / Severe).
- Controls: pause, reset, speed up to 20x, control mode, traffic scenario (late night, normal, morning rush, evening rush, stadium event) and four demand sliders.
- "Send ambulance" to test emergency priority.
- Live KPIs: throughput, average delay, vehicles passed, max queue, idle fuel and CO2 estimate.
- Traffic advisor: plain-language recommendations, including a Webster timing plan.
- Event log: signal changes, congestion alerts, emergency events.

**Analytics & Benchmark**
- Live line chart of queues per approach and a congestion density bar chart.
- Benchmark: runs Fixed-time, Adaptive and Webster on **identical random traffic** (same seed) and compares delay, queue, fuel, CO2 and ambulance delay.

**Database Records** - browse stored readings, events and benchmark runs, sort any column, export CSV, clear data, edit database settings.

**About** - team details and a demo script.

## Control strategies

| Mode | How it decides |
|---|---|
| Fixed-time | 25 s green for each direction, 3 s yellow, 1.5 s all-red. The "traditional signal" from the problem statement. |
| Adaptive | Sensor-actuated. Minimum green 8 s, maximum 50 s. Ends green early when nobody is left to serve (gap-out), or when the other phase's demand plus its waiting time outweighs the current one. Ambulance pre-emption. |
| Webster | Recomputes cycle length and green split from measured flow every phase: C = (1.5L + 5) / (1 - Y). |

## Results from our test runs (15 simulated minutes, same traffic for each mode)

| Scenario | Fixed-time delay | Adaptive delay | Webster delay |
|---|---|---|---|
| Normal day | 12.2 s | 6.8 s | 10.9 s |
| Morning rush (N-S heavy) | 13.6 s | 7.5 s | 15.8 s |
| Evening rush (E-W heavy) | 14.1 s | 8.2 s | 15.1 s |
| Stadium event (East heavy) | 31.8 s | 8.3 s | 12.7 s |

Adaptive control wins in every scenario. Webster helps when traffic is lopsided but is not better than fixed-time on
balanced heavy traffic, which is a known limit of a pre-timed method. Numbers vary slightly with the random seed;
run the benchmark yourself during the demo.

## Project structure

```
src/traffic/
  Main.java                 entry point
  model/                    Direction, Phase, LightState, Vehicle(Type), Level, ControlMode, Scenario,
                            TrafficReading, TrafficEvent, RunSummary
  engine/
    Lane.java               one approach: Poisson arrivals, car-following, obeying the signal
    SignalController.java   fixed / adaptive / Webster state machine (green -> yellow -> all-red)
    CongestionAnalyzer.java density, levels, Webster plan, recommendations
    TrafficSimulation.java  the junction: 4 lanes + controller + statistics + sampling
    Benchmark.java          headless comparison of the three modes
  db/
    DataStore.java          persistence interface
    MySqlDataStore.java     JDBC implementation
    CsvDataStore.java       fallback implementation
    StorageManager.java     async writes, fallback logic
    AppConfig.java          config.properties
  ui/                       Swing: Theme, IntersectionPanel, DashboardTab, AnalyticsTab, RecordsTab, charts...
schema.sql                  MySQL tables
```

## Key ideas to explain in the viva

- **Why simulate?** No real sensors are available, so vehicles arrive randomly (Poisson process) at a rate per minute. The PPT calls this "manual or simulated data".
- **Car-following:** each vehicle slows to a safe speed based on the gap to the vehicle ahead (Krauss-style safe-speed formula) and stops at the line on red. On yellow, a vehicle stops only if it can brake in time; otherwise it goes through.
- **Congestion density** = waiting vehicles / lane capacity (10 vehicles). Below 25% Low, 50% Moderate, 75% High, above that Severe.
- **Why the benchmark is fair:** each mode uses the same random seed, so the same vehicles arrive at the same moments.
- **Average delay** = actual time to reach the stop line minus free-flow time; vehicles still waiting at the end are counted too, so a mode cannot look good by leaving cars stuck.
- **Fuel / CO2:** idling is estimated at 0.8 L per hour per vehicle and 2.31 kg CO2 per litre of petrol. These are estimates, not measurements.
- **Why a database?** Readings, events and benchmark runs are stored for reports and later analysis (the "historical data" benefit in the PPT).
- **Threading:** the simulation runs on the Swing timer (UI thread); database writes use a background thread; the benchmark and record loading use SwingWorker, so the UI never freezes.
- **Future scope (from the PPT):** real sensors / camera input, GPS routing, AI traffic prediction, multi-junction coordination (green wave).

## Limitations (be upfront about these)

- One lane per approach and two signal phases; no turning movements, pedestrians or multiple junctions.
- Simulated input only; real sensor integration is future scope.
- Webster and the saturation flow constant are tuned to this simulator.

  ## Configuration

The app reads its settings from `config.properties` in the project's main folder.
Edit it to set your MySQL username and password. If MySQL isn't available,
the app falls back to saving data in CSV files (in the `data` folder).
