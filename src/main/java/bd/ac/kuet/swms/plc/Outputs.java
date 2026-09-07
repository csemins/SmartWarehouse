package bd.ac.kuet.swms.plc;

import java.util.Arrays;

public class Outputs {
    private final boolean[] coils = new boolean[Actuator.COUNT];
    private int targetPosition;

    public void set(Actuator a, boolean on) { coils[a.address()] = on; }

    public boolean get(Actuator a) { return coils[a.address()]; }

    public int getTargetPosition() { return targetPosition; }

    public void setTargetPosition(int target) { this.targetPosition = target; }

    public void forks(Side side) {
        set(Actuator.FORKS_LEFT, side == Side.LEFT);
        set(Actuator.FORKS_RIGHT, side == Side.RIGHT);
    }

    public void stopMotion() {
        for (Actuator a : Actuator.values()) {
            if (a != Actuator.START_LIGHT && a != Actuator.STOP_LIGHT && a != Actuator.RESET_LIGHT) set(a, false);
        }
    }

    public void conveyorsOff() {
        set(Actuator.ENTRY_CONVEYOR, false);
        set(Actuator.LOAD_CONVEYOR, false);
        set(Actuator.UNLOAD_CONVEYOR, false);
        set(Actuator.EXIT_CONVEYOR, false);
    }

    public boolean[] coils() { return Arrays.copyOf(coils, coils.length); }
}
