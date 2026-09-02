package bd.ac.kuet.swms.plc;

import java.io.IOException;

public class ModbusException extends IOException {
    private final int code;

    public ModbusException(int function, int code) {
        super("Modbus exception " + code + " for function " + function
                // code 2 = my I/O points count in factory io was wrong, happened twice
                + (code == 2 ? " (illegal address - check the I/O Points count in Factory IO)" : ""));
        this.code = code;
    }

    public int getCode() { return code; }
}
