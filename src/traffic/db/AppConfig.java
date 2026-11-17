package traffic.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Database settings, stored in config.properties next to the application. */
public class AppConfig {
    public enum StorageMode { AUTO, MYSQL, CSV }

    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/smart_traffic?createDatabaseIfNotExist=true&useSSL=false"
                    + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final Path FILE = Path.of("config.properties");

    public StorageMode mode = StorageMode.AUTO;
    public String url = DEFAULT_URL;
    public String user = "root";
    public String password = "";

    public static AppConfig load() {
        AppConfig c = new AppConfig();
        if (Files.exists(FILE)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(FILE)) {
                p.load(in);
                c.mode = StorageMode.valueOf(p.getProperty("storage.mode", "AUTO").trim().toUpperCase());
                c.url = p.getProperty("db.url", DEFAULT_URL);
                c.user = p.getProperty("db.user", "root");
                c.password = p.getProperty("db.password", "");
            } catch (IOException | IllegalArgumentException ignored) {
                // fall back to defaults
            }
        }
        return c;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("storage.mode", mode.name());
        p.setProperty("db.url", url);
        p.setProperty("db.user", user);
        p.setProperty("db.password", password);
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "Smart Traffic Management System - storage settings");
        } catch (IOException ignored) {
            // not critical
        }
    }

    public AppConfig copy() {
        AppConfig c = new AppConfig();
        c.mode = mode;
        c.url = url;
        c.user = user;
        c.password = password;
        return c;
    }
}
