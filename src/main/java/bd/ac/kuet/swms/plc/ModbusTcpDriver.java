package bd.ac.kuet.swms.plc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ModbusTcpDriver implements PlcDriver {
    private final String host;
    private final int port;
    private final int unitId;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int transactionId;

    public ModbusTcpDriver(String host, int port, int unitId) {
        this.host = host;
        this.port = port;
        this.unitId = unitId;
    }

    @Override
    public synchronized void connect() throws IOException {
        disconnect();
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 2000);
        s.setSoTimeout(2000);
        socket = s;
        in = new DataInputStream(s.getInputStream());
        out = new DataOutputStream(s.getOutputStream());
    }

    @Override
    public synchronized void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    @Override
    public synchronized boolean isConnected() { return socket != null && !socket.isClosed(); }

    @Override
    public String describe() { return host + ":" + port + " (slave " + unitId + ")"; }

    @Override
    public boolean[] readDiscreteInputs(int start, int count) throws IOException {
        return bits(request(new byte[]{0x02, hi(start), lo(start), hi(count), lo(count)}), count);
    }

    @Override
    public boolean[] readCoils(int start, int count) throws IOException {
        return bits(request(new byte[]{0x01, hi(start), lo(start), hi(count), lo(count)}), count);
    }

    @Override
    public void writeCoils(int start, boolean[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            int address = start + i;
            request(new byte[]{0x05, hi(address), lo(address), (byte) (values[i] ? 0xFF : 0x00), 0x00});
        }
    }

    @Override
    public void writeRegister(int address, int value) throws IOException {
        request(new byte[]{0x06, hi(address), lo(address), hi(value), lo(value)});
    }

    // MBAP header: tid(2) pid(2)=0 len(2) unit(1), then the pdu
    private synchronized byte[] request(byte[] pdu) throws IOException {
        if (!isConnected()) throw new IOException("Not connected");
        int tid = ++transactionId & 0xFFFF;
        try {
            byte[] frame = new byte[7 + pdu.length];
            frame[0] = hi(tid);
            frame[1] = lo(tid);
            frame[4] = hi(pdu.length + 1);
            frame[5] = lo(pdu.length + 1);
            frame[6] = (byte) unitId;
            System.arraycopy(pdu, 0, frame, 7, pdu.length);
            out.write(frame);
            out.flush();

            in.readUnsignedShort();
            in.readUnsignedShort();
            int length = in.readUnsignedShort();
            in.readUnsignedByte();
            byte[] body = new byte[length - 1];
            in.readFully(body);

            int function = body[0] & 0xFF;
            if (function == ((pdu[0] & 0xFF) | 0x80)) throw new ModbusException(pdu[0], body[1] & 0xFF);
            byte[] data = new byte[body.length - 1];
            System.arraycopy(body, 1, data, 0, data.length);
            return data;
        } catch (ModbusException e) {
            throw e;
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    private static boolean[] bits(byte[] data, int count) {
        boolean[] result = new boolean[count];
        for (int i = 0; i < count; i++) result[i] = ((data[i / 8] >> (i % 8)) & 1) == 1;
        return result;
    }

    private static byte hi(int v) { return (byte) (v >> 8); }

    private static byte lo(int v) { return (byte) v; }
}
