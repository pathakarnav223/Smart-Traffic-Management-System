package traffic.model;

/** A signal phase: the two approaches that get green together. */
public enum Phase {
    NS("North-South"), EW("East-West");

    private final String label;

    Phase(String label) {
        this.label = label;
    }

    public Phase other() {
        return this == NS ? EW : NS;
    }

    public Direction[] directions() {
        return this == NS
                ? new Direction[]{Direction.NORTH, Direction.SOUTH}
                : new Direction[]{Direction.EAST, Direction.WEST};
    }

    @Override
    public String toString() {
        return label;
    }
}
