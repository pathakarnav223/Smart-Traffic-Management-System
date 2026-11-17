package traffic.engine;

import traffic.model.TrafficEvent;
import traffic.model.TrafficReading;

import java.util.List;

/** Receives events and periodic readings from a running simulation. */
public interface SimListener {
    void onEvent(TrafficEvent event);

    void onSnapshot(List<TrafficReading> readings);
}
