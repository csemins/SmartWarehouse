package bd.ac.kuet.swms.plc;

import java.io.IOException;

public interface PlcDriver extends AutoCloseable {
    void connect() throws IOException;

    void disconnect();

    boolean isConnected();

    String describe();

    boolean[] readDiscreteInputs(int start, int count) throws IOException;

    boolean[] readCoils(int start, int count) throws IOException;

    void writeCoils(int start, boolean[] values) throws IOException;

    void writeRegister(int address, int value) throws IOException;

    @Override
    default void close() { disconnect(); }
}
