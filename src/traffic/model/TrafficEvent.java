package traffic.model;

import java.time.LocalDateTime;

/** A noteworthy event: signal change, congestion alert, emergency vehicle, mode change... */
public record TrafficEvent(LocalDateTime timestamp, double simTime, String type, String description) {
}
