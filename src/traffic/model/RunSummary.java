package traffic.model;

import java.time.LocalDateTime;

/** Result of one benchmark run (a control mode simulated over a fixed duration). */
public record RunSummary(LocalDateTime timestamp, String scenario, String mode, double durationSec,
                         int passed, double avgDelaySec, int maxQueue, double fuelLitres,
                         double co2Kg, double avgAmbulanceDelaySec) {
}
