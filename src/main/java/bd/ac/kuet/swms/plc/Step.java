package bd.ac.kuet.swms.plc;

public abstract class Step {
    private final String description;

    protected Step(String description) { this.description = description; }

    public String description() { return description; }

    public abstract void start(Outputs out);

    public abstract boolean isDone(IoImage in, long elapsedMs);

    public void finish(Outputs out) { }

    @Override public String toString() { return description; }

    public static class MoveCrane extends Step {
        private final int target;
        private long stillSince;

        public MoveCrane(int target) {
            super(target == 55 ? "Crane to rest position" : "Crane to cell " + target);
            this.target = target;
        }

        @Override public void start(Outputs out) {
            out.setTargetPosition(target);
            stillSince = -1;
        }

        @Override public boolean isDone(IoImage in, long elapsedMs) {
            if (in.craneMoving()) {
                stillSince = -1;
                return false;
            }
            // moving flag flickers at the start of a move, wait until still for 800ms
            if (stillSince < 0) stillSince = elapsedMs;
            return elapsedMs > 500 && elapsedMs - stillSince >= 800;
        }
    }

    public static class Forks extends Step {
        private final Side side;

        public Forks(Side side) {
            super(side == Side.MIDDLE ? "Forks to middle" : "Forks " + side.name().toLowerCase());
            this.side = side;
        }

        @Override public void start(Outputs out) { out.forks(side); }

        @Override public boolean isDone(IoImage in, long elapsedMs) {
            return elapsedMs > 200 && in.forksAt(side);
        }
    }

    public static class Lift extends Step {
        private final boolean up;

        public Lift(boolean up) {
            super(up ? "Lift box" : "Lower box");
            this.up = up;
        }

        @Override public void start(Outputs out) {
            out.set(Actuator.LIFT, up);
            stillSince = -1;
        }

        private long stillSince = -1;

        @Override public boolean isDone(IoImage in, long elapsedMs) {
            if (in.get(Sensor.MOVING_Z)) {
                stillSince = -1;
                return false;
            }
            if (stillSince < 0) stillSince = elapsedMs;
            return elapsedMs > 800 && elapsedMs - stillSince >= 300;
        }
    }

    public static class Conveyor extends Step {
        private final Actuator[] conveyors;
        private final Sensor sensor;
        private final boolean waitForBox;
        private final long extraMs;

        public Conveyor(String description, Sensor sensor, boolean waitForBox, long extraMs, Actuator... conveyors) {
            super(description);
            this.sensor = sensor;
            this.waitForBox = waitForBox;
            this.extraMs = extraMs;
            this.conveyors = conveyors;
        }

        private long metAt = -1;

        @Override public void start(Outputs out) {
            metAt = -1;
            for (Actuator a : conveyors) out.set(a, true);
        }

        @Override public boolean isDone(IoImage in, long elapsedMs) {
            if (metAt < 0 && in.box(sensor) == waitForBox) metAt = elapsedMs;
            return metAt >= 0 && elapsedMs - metAt >= extraMs;
        }

        @Override public void finish(Outputs out) {
            for (Actuator a : conveyors) out.set(a, false);
        }
    }

    // exit sensor: wait until the box passed it, maxMs as safety net
    public static class ConveyorThrough extends Step {
        private final Sensor sensor;
        private final long maxMs;
        private final Actuator[] conveyors;
        private boolean seen;
        private long clearSince = -1;

        public ConveyorThrough(String description, Sensor sensor, long maxMs, Actuator... conveyors) {
            super(description);
            this.sensor = sensor;
            this.maxMs = maxMs;
            this.conveyors = conveyors;
        }

        @Override public void start(Outputs out) {
            clearSince = -1;
            for (Actuator a : conveyors) out.set(a, true);
        }

        @Override public boolean isDone(IoImage in, long elapsedMs) {
            if (elapsedMs >= maxMs) return true;
            if (in.box(sensor)) {
                seen = true;
                clearSince = -1;
                return false;
            }
            if (!seen) return false;
            if (clearSince < 0) clearSince = elapsedMs;
            return elapsedMs - clearSince >= 500;
        }

        @Override public void finish(Outputs out) {
            for (Actuator a : conveyors) out.set(a, false);
        }
    }
}
