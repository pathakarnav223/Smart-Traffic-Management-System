package traffic.model;

import java.awt.Color;

/** Congestion level of an approach, derived from its density (0..1). */
public enum Level {
    LOW("Low", new Color(0x3DDC84)),
    MODERATE("Moderate", new Color(0xFFC83D)),
    HIGH("High", new Color(0xFF9A3D)),
    SEVERE("Severe", new Color(0xFF5A52));

    public final String label;
    public final Color color;

    Level(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public static Level fromDensity(double density) {
        if (density < 0.25) return LOW;
        if (density < 0.50) return MODERATE;
        if (density < 0.75) return HIGH;
        return SEVERE;
    }

    @Override
    public String toString() {
        return label;
    }
}
