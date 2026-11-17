package traffic.model;

/** The signal control strategy used by the junction. */
public enum ControlMode {
    FIXED("Fixed-time (25 s / 25 s)"),
    ADAPTIVE("Adaptive (sensor-actuated)"),
    WEBSTER("Webster auto-timing");

    private final String label;

    ControlMode(String label) {
        this.label = label;
    }

    public String shortName() {
        return switch (this) {
            case FIXED -> "Fixed-time";
            case ADAPTIVE -> "Adaptive";
            case WEBSTER -> "Webster";
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
