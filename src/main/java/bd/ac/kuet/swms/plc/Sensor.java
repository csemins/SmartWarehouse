package bd.ac.kuet.swms.plc;

public enum Sensor {
    AT_ENTRY("At Entry"),
    AT_LOAD("At Load"),
    AT_LEFT("At Left"),
    AT_MIDDLE("At Middle"),
    AT_RIGHT("At Right"),
    AT_UNLOAD("At Unload"),
    AT_EXIT("At Exit"),
    MOVING_X("Moving X"),
    MOVING_Z("Moving Z"),
    START("Start"),
    RESET("Reset"),
    STOP("Stop"),
    EMERGENCY_STOP("Emergency stop"),
    AUTO("Auto"),
    FACTORY_IO_RUNNING("FACTORY I/O (Running)");

    public static final int COUNT = values().length;

    private final String label;

    Sensor(String label) { this.label = label; }

    public int address() { return ordinal(); }

    public String label() { return label; }
}
