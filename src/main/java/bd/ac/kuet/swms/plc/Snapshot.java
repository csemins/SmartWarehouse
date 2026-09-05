package bd.ac.kuet.swms.plc;

import java.util.List;

public record Snapshot(ControllerState state, String endpoint, boolean[] inputs, boolean[] coils, int targetPosition,
                       WarehouseJob currentJob, List<WarehouseJob> queue, String message, long scans) {

    public boolean input(Sensor s) { return inputs != null && inputs[s.address()]; }

    public boolean coil(Actuator a) { return coils != null && coils[a.address()]; }
}
