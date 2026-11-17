package traffic.db;

import traffic.model.RunSummary;
import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** MySQL persistence through JDBC. Needs mysql-connector-j on the classpath (put the jar in lib/). */
public class MySqlDataStore implements DataStore {
    private final Connection conn;
    private final String description;

    public MySqlDataStore(String url, String user, String password) throws SQLException {
        DriverManager.setLoginTimeout(4);
        try {
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
                throw new SQLException("MySQL JDBC driver not found - put mysql-connector-j.jar in the lib/ folder", e);
            }
            throw e;
        }
        this.description = "MySQL @ " + conn.getMetaData().getURL().replaceFirst("\\?.*", "").replace("jdbc:mysql://", "");
        createSchema();
    }

    private void createSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS traffic_reading ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, ts DATETIME NOT NULL, sim_time DOUBLE, "
                    + "scenario VARCHAR(40), control_mode VARCHAR(20), direction VARCHAR(10), waiting INT, "
                    + "density DOUBLE, level VARCHAR(12), avg_speed_kmh DOUBLE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS traffic_event ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, ts DATETIME NOT NULL, sim_time DOUBLE, "
                    + "event_type VARCHAR(20), description VARCHAR(255))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS run_summary ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, ts DATETIME NOT NULL, scenario VARCHAR(40), "
                    + "control_mode VARCHAR(20), duration_sec DOUBLE, passed INT, avg_delay_sec DOUBLE, "
                    + "max_queue INT, fuel_litres DOUBLE, co2_kg DOUBLE, avg_ambulance_delay_sec DOUBLE)");
        }
    }

    @Override
    public synchronized void saveReadings(List<TrafficReading> readings) throws SQLException {
        String sql = "INSERT INTO traffic_reading (ts, sim_time, scenario, control_mode, direction, waiting, "
                + "density, level, avg_speed_kmh) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TrafficReading r : readings) {
                ps.setTimestamp(1, Timestamp.valueOf(r.timestamp()));
                ps.setDouble(2, r.simTime());
                ps.setString(3, r.scenario());
                ps.setString(4, r.mode());
                ps.setString(5, r.direction());
                ps.setInt(6, r.waiting());
                ps.setDouble(7, r.density());
                ps.setString(8, r.level());
                ps.setDouble(9, r.avgSpeedKmh());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public synchronized void saveEvent(TrafficEvent e) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO traffic_event (ts, sim_time, event_type, description) VALUES (?,?,?,?)")) {
            ps.setTimestamp(1, Timestamp.valueOf(e.timestamp()));
            ps.setDouble(2, e.simTime());
            ps.setString(3, e.type());
            ps.setString(4, e.description().length() > 250 ? e.description().substring(0, 250) : e.description());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void saveRun(RunSummary r) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO run_summary (ts, scenario, control_mode, duration_sec, passed, avg_delay_sec, max_queue, "
                        + "fuel_litres, co2_kg, avg_ambulance_delay_sec) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setTimestamp(1, Timestamp.valueOf(r.timestamp()));
            ps.setString(2, r.scenario());
            ps.setString(3, r.mode());
            ps.setDouble(4, r.durationSec());
            ps.setInt(5, r.passed());
            ps.setDouble(6, r.avgDelaySec());
            ps.setInt(7, r.maxQueue());
            ps.setDouble(8, r.fuelLitres());
            ps.setDouble(9, r.co2Kg());
            ps.setDouble(10, r.avgAmbulanceDelaySec());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized List<TrafficReading> loadReadings(int limit) throws SQLException {
        List<TrafficReading> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ts, sim_time, scenario, control_mode, direction, waiting, density, level, avg_speed_kmh "
                        + "FROM traffic_reading ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TrafficReading(rs.getTimestamp(1).toLocalDateTime(), rs.getDouble(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getInt(6), rs.getDouble(7), rs.getString(8),
                            rs.getDouble(9)));
                }
            }
        }
        return out;
    }

    @Override
    public synchronized List<TrafficEvent> loadEvents(int limit) throws SQLException {
        List<TrafficEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ts, sim_time, event_type, description FROM traffic_event ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TrafficEvent(rs.getTimestamp(1).toLocalDateTime(), rs.getDouble(2), rs.getString(3),
                            rs.getString(4)));
                }
            }
        }
        return out;
    }

    @Override
    public synchronized List<RunSummary> loadRuns(int limit) throws SQLException {
        List<RunSummary> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ts, scenario, control_mode, duration_sec, passed, avg_delay_sec, max_queue, fuel_litres, "
                        + "co2_kg, avg_ambulance_delay_sec FROM run_summary ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new RunSummary(rs.getTimestamp(1).toLocalDateTime(), rs.getString(2), rs.getString(3),
                            rs.getDouble(4), rs.getInt(5), rs.getDouble(6), rs.getInt(7), rs.getDouble(8),
                            rs.getDouble(9), rs.getDouble(10)));
                }
            }
        }
        return out;
    }

    @Override
    public synchronized void clearAll() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM traffic_reading");
            st.executeUpdate("DELETE FROM traffic_event");
            st.executeUpdate("DELETE FROM run_summary");
        }
    }

    @Override
    public String describe() {
        return description;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
            // closing
        }
    }
}
