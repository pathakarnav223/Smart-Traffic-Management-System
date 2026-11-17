package traffic.model;

/**
 * One vehicle on an approach lane. Position {@code s} is the distance of the vehicle's front bumper
 * from the stop line in metres (negative = still approaching, positive = already through).
 */
public class Vehicle {
    private static int counter = 0;

    public final int id = ++counter;
    public final Direction dir;
    public final VehicleType type;
    public final double arrivalTime;

    public double s;
    public double v;
    public double stoppedTime;
    public boolean crossed;
    /** Set when the vehicle is too close to stop for a yellow light and goes through. */
    public boolean committed;
    public boolean braking;

    public Vehicle(Direction dir, VehicleType type, double arrivalTime) {
        this.dir = dir;
        this.type = type;
        this.arrivalTime = arrivalTime;
    }

    public boolean isEmergency() {
        return type == VehicleType.AMBULANCE;
    }
}
