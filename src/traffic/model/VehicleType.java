package traffic.model;

import java.awt.Color;

/** Vehicle classes with length (m), free-flow speed (m/s) and drawing colour. */
public enum VehicleType {
    CAR("Car", 4.5, 13.9, new Color(0x5B9BD5)),
    BIKE("Two-wheeler", 2.0, 13.9, new Color(0xE0B84C)),
    BUS("Bus", 10.5, 11.1, new Color(0xE2733D)),
    TRUCK("Truck", 8.0, 11.1, new Color(0x8D8D96)),
    AMBULANCE("Ambulance", 5.5, 16.7, new Color(0xF4F4F4));

    public final String label;
    public final double length;
    public final double freeSpeed;
    public final Color color;

    VehicleType(String label, double length, double freeSpeed, Color color) {
        this.label = label;
        this.length = length;
        this.freeSpeed = freeSpeed;
        this.color = color;
    }
}
