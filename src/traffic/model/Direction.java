package traffic.model;

/** The four approaches of the junction. NORTH means traffic arriving from the north (heading south). */
public enum Direction {
    NORTH("North"), SOUTH("South"), EAST("East"), WEST("West");

    private final String label;

    Direction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isNorthSouth() {
        return this == NORTH || this == SOUTH;
    }

    public Phase phase() {
        return isNorthSouth() ? Phase.NS : Phase.EW;
    }

    @Override
    public String toString() {
        return label;
    }
}
