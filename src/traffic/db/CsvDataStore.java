package traffic.db;

import traffic.model.RunSummary;
import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fallback persistence: plain CSV files in the data/ folder (works with no database installed). */
public class CsvDataStore implements DataStore {
    private static final String READINGS_HEADER = "timestamp,sim_time,scenario,mode,direction,waiting,density,level,avg_speed_kmh";
    private static final String EVENTS_HEADER = "timestamp,sim_time,type,description";
    private static final String RUNS_HEADER = "timestamp,scenario,mode,duration_sec,passed,avg_delay_sec,max_queue,fuel_litres,co2_kg,avg_ambulance_delay_sec";

    private final Path dir;
    private final Path readingsFile;
    private final Path eventsFile;
    private final Path runsFile;

    public CsvDataStore(Path dir) throws IOException {
        this.dir = dir;
        Files.createDirectories(dir);
        readingsFile = dir.resolve("readings.csv");
        eventsFile = dir.resolve("events.csv");
        runsFile = dir.resolve("runs.csv");
        ensure(readingsFile, READINGS_HEADER);
        ensure(eventsFile, EVENTS_HEADER);
        ensure(runsFile, RUNS_HEADER);
    }

    private static void ensure(Path f, String header) throws IOException {
        if (!Files.exists(f)) Files.writeString(f, header + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void append(Path f, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append(System.lineSeparator());
        Files.writeString(f, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    static String quote(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    static List<String> parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else inQuotes = false;
                } else cur.append(c);
            } else if (c == '"') inQuotes = true;
            else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    private static List<List<String>> readRows(Path f, int limit) throws IOException {
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        List<List<String>> rows = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 1 && rows.size() < limit; i--) {
            if (!lines.get(i).isBlank()) rows.add(parse(lines.get(i)));
        }
        return rows;
    }

    @Override
    public synchronized void saveReadings(List<TrafficReading> rs) throws IOException {
        List<String> lines = new ArrayList<>();
        for (TrafficReading r : rs) {
            lines.add(String.join(",", r.timestamp().toString(), String.format("%.1f", r.simTime()), quote(r.scenario()),
                    r.mode(), r.direction(), String.valueOf(r.waiting()), String.format("%.3f", r.density()),
                    r.level(), String.format("%.1f", r.avgSpeedKmh())));
        }
        append(readingsFile, lines);
    }

    @Override
    public synchronized void saveEvent(TrafficEvent e) throws IOException {
        append(eventsFile, List.of(String.join(",", e.timestamp().toString(), String.format("%.1f", e.simTime()),
                e.type(), quote(e.description()))));
    }

    @Override
    public synchronized void saveRun(RunSummary r) throws IOException {
        append(runsFile, List.of(String.join(",", r.timestamp().toString(), quote(r.scenario()), r.mode(),
                String.format("%.0f", r.durationSec()), String.valueOf(r.passed()), String.format("%.2f", r.avgDelaySec()),
                String.valueOf(r.maxQueue()), String.format("%.4f", r.fuelLitres()), String.format("%.4f", r.co2Kg()),
                String.format("%.2f", r.avgAmbulanceDelaySec()))));
    }

    @Override
    public synchronized List<TrafficReading> loadReadings(int limit) throws IOException {
        List<TrafficReading> out = new ArrayList<>();
        for (List<String> c : readRows(readingsFile, limit)) {
            if (c.size() < 9) continue;
            out.add(new TrafficReading(LocalDateTime.parse(c.get(0)), Double.parseDouble(c.get(1)), c.get(2), c.get(3),
                    c.get(4), Integer.parseInt(c.get(5)), Double.parseDouble(c.get(6)), c.get(7),
                    Double.parseDouble(c.get(8))));
        }
        return out;
    }

    @Override
    public synchronized List<TrafficEvent> loadEvents(int limit) throws IOException {
        List<TrafficEvent> out = new ArrayList<>();
        for (List<String> c : readRows(eventsFile, limit)) {
            if (c.size() < 4) continue;
            out.add(new TrafficEvent(LocalDateTime.parse(c.get(0)), Double.parseDouble(c.get(1)), c.get(2), c.get(3)));
        }
        return out;
    }

    @Override
    public synchronized List<RunSummary> loadRuns(int limit) throws IOException {
        List<RunSummary> out = new ArrayList<>();
        for (List<String> c : readRows(runsFile, limit)) {
            if (c.size() < 10) continue;
            out.add(new RunSummary(LocalDateTime.parse(c.get(0)), c.get(1), c.get(2), Double.parseDouble(c.get(3)),
                    Integer.parseInt(c.get(4)), Double.parseDouble(c.get(5)), Integer.parseInt(c.get(6)),
                    Double.parseDouble(c.get(7)), Double.parseDouble(c.get(8)), Double.parseDouble(c.get(9))));
        }
        return out;
    }

    @Override
    public synchronized void clearAll() throws IOException {
        Files.writeString(readingsFile, READINGS_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(eventsFile, EVENTS_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(runsFile, RUNS_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    @Override
    public String describe() {
        return "CSV files in " + dir.toAbsolutePath().normalize();
    }

    @Override
    public boolean isDatabase() {
        return false;
    }

    @Override
    public void close() {
        // nothing to release
    }
}
