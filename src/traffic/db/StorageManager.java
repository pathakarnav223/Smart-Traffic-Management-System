package traffic.db;

import traffic.model.RunSummary;
import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the active DataStore. Writes happen on a background thread so the UI never freezes.
 * If MySQL is unavailable the manager falls back to CSV files and remembers why.
 */
public class StorageManager {
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "storage-writer");
        t.setDaemon(true);
        return t;
    });

    private volatile DataStore store;
    private volatile String statusNote = "";
    private volatile String lastWriteError = "";
    private AppConfig config;

    public StorageManager(AppConfig config) {
        apply(config);
    }

    /** (Re)connect according to the given settings. Returns a status line for the UI. */
    public synchronized String apply(AppConfig cfg) {
        this.config = cfg.copy();
        DataStore old = store;
        DataStore next = null;
        String note = "";
        if (cfg.mode != AppConfig.StorageMode.CSV) {
            try {
                next = new MySqlDataStore(cfg.url, cfg.user, cfg.password);
            } catch (Exception e) {
                note = "MySQL unavailable (" + shorten(e.getMessage()) + ") - using CSV files instead.";
            }
        }
        if (next == null) {
            try {
                next = new CsvDataStore(Path.of("data"));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create data folder: " + e.getMessage(), e);
            }
            if (cfg.mode == AppConfig.StorageMode.CSV) note = "CSV mode selected.";
        }
        store = next;
        statusNote = note;
        lastWriteError = "";
        if (old != null) old.close();
        return status();
    }

    private static String shorten(String m) {
        if (m == null) return "unknown error";
        m = m.replace('\n', ' ');
        return m.length() > 90 ? m.substring(0, 90) + "..." : m;
    }

    public DataStore store() {
        return store;
    }

    public AppConfig config() {
        return config.copy();
    }

    public String status() {
        String s = "Storage: " + store.describe();
        if (!statusNote.isEmpty()) s += "  |  " + statusNote;
        if (!lastWriteError.isEmpty()) s += "  |  last write error: " + lastWriteError;
        return s;
    }

    // ------------------------------------------------------------------ async writes

    public void logReadings(List<TrafficReading> readings) {
        submit(() -> store.saveReadings(readings));
    }

    public void logEvent(TrafficEvent event) {
        submit(() -> store.saveEvent(event));
    }

    public void logRun(RunSummary run) {
        submit(() -> store.saveRun(run));
    }

    private interface Job {
        void run() throws Exception;
    }

    private void submit(Job job) {
        writer.submit(() -> {
            try {
                job.run();
            } catch (Exception e) {
                lastWriteError = shorten(e.getMessage());
            }
        });
    }

    public void flushAndClose() {
        writer.shutdown();
        try {
            writer.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        store.close();
    }
}
