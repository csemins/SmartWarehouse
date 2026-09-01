package bd.ac.kuet.swms.plc;

import java.io.IOException;

public class ModbusException extends IOException {
    private final int code;

    public ModbusException(int function, int code) {
        super("Modbus exception " + code + " for function " + function);
        this.code = code;
    }

    public int getCode() { return code; }
}
