package bd.ac.kuet.swms.plc;

public record CraneConfig(Side loadSide, Side unloadSide, Side rackSide, int restPosition,
                          boolean conveyorSensorsInverted, int stepTimeoutSeconds) {

    // 55 is the rest position in front of the conveyors
    public static CraneConfig defaults() {
        return new CraneConfig(Side.RIGHT, Side.LEFT, Side.LEFT, 55, true, 30);
    }
}
