package traffic.model;

/** Traffic demand presets. Rates are vehicles per minute for NORTH, SOUTH, EAST, WEST. */
public enum Scenario {
    NIGHT("Late night", 3, 3, 3, 3),
    NORMAL("Normal day", 8, 8, 8, 8),
    MORNING_RUSH("Morning rush (N-S heavy)", 17, 15, 6, 6),
    EVENING_RUSH("Evening rush (E-W heavy)", 6, 7, 16, 17),
    EVENT("Stadium event (East heavy)", 5, 4, 22, 6),
    CUSTOM("Custom (use sliders)", 8, 8, 8, 8);

    private final String label;
    private final double[] rates;

    Scenario(String label, double n, double s, double e, double w) {
        this.label = label;
        this.rates = new double[]{n, s, e, w};
    }

    public double rate(Direction d) {
        return rates[d.ordinal()];
    }

    @Override
    public String toString() {
        return label;
    }
}
