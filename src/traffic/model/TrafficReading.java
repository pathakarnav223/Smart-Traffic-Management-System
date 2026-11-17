package traffic.model;

import java.time.LocalDateTime;

/** One periodic sensor-style reading for a single approach. */
public record TrafficReading(LocalDateTime timestamp, double simTime, String scenario, String mode,
                             String direction, int waiting, double density, String level,
                             double avgSpeedKmh) {
}
