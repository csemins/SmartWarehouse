package bd.ac.kuet.swms.plc;

public record IoImage(boolean[] inputs, boolean conveyorSensorsInverted) {

    public boolean get(Sensor s) { return inputs[s.address()]; }

    public boolean box(Sensor s) { return get(s); }

    public boolean craneMoving() { return get(Sensor.MOVING_X) || get(Sensor.MOVING_Z); }

    public boolean forksAt(Side side) {
        return switch (side) {
            case LEFT -> get(Sensor.AT_LEFT);
            case RIGHT -> get(Sensor.AT_RIGHT);
            case MIDDLE -> get(Sensor.AT_MIDDLE);
        };
    }
}
