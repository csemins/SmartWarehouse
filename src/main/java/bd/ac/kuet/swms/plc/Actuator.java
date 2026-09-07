package bd.ac.kuet.swms.plc;

public enum Actuator {
    ENTRY_CONVEYOR("Entry Conveyor"),
    LOAD_CONVEYOR("Load Conveyor"),
    FORKS_LEFT("Forks Left"),
    FORKS_RIGHT("Forks Right"),
    LIFT("Lift"),
    UNLOAD_CONVEYOR("Unload Conveyor"),
    EXIT_CONVEYOR("Exit Conveyor"),
    START_LIGHT("Start light"),
    RESET_LIGHT("Reset light"),
    STOP_LIGHT("Stop light");

    public static final int COUNT = values().length;
    public static final int TARGET_POSITION_REGISTER = 0;

    private final String label;

    Actuator(String label) { this.label = label; }

    public int address() { return ordinal(); }

    public String label() { return label; }
}
