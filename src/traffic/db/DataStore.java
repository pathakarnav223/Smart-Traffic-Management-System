package traffic.db;

import traffic.model.RunSummary;
import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import java.util.List;

/** Persistence contract used by the application (MySQL via JDBC, or CSV files as a fallback). */
public interface DataStore extends AutoCloseable {
    void saveReadings(List<TrafficReading> readings) throws Exception;

    void saveEvent(TrafficEvent event) throws Exception;

    void saveRun(RunSummary run) throws Exception;

    /** Newest first. */
    List<TrafficReading> loadReadings(int limit) throws Exception;

    List<TrafficEvent> loadEvents(int limit) throws Exception;

    List<RunSummary> loadRuns(int limit) throws Exception;

    void clearAll() throws Exception;

    /** Human-readable description, e.g. "MySQL @ localhost:3306/smart_traffic". */
    String describe();

    boolean isDatabase();

    @Override
    void close();
}
