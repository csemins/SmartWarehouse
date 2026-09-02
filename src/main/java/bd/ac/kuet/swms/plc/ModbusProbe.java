package bd.ac.kuet.swms.plc;

import java.util.Arrays;

// quick test against factory io, NOT part of the app. delete when the driver works
public class ModbusProbe {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "192.168.0.111";
        try (ModbusTcpDriver d = new ModbusTcpDriver(host, 502, 1)) {
            d.connect();
            System.out.println("connected " + d.describe());
            for (int round = 0; round < 20; round++) {
                boolean[] in = d.readDiscreteInputs(0, Sensor.COUNT);
                StringBuilder sb = new StringBuilder();
                for (Sensor s : Sensor.values()) if (in[s.address()]) sb.append(s.label()).append(", ");
                System.out.println(round + ": " + sb);
                // toggle entry conveyor every second round
                boolean[] coils = new boolean[10];
                coils[0] = round % 2 == 0;
                long t0 = System.currentTimeMillis();
                d.writeCoils(0, coils);
                System.out.println("  write took " + (System.currentTimeMillis() - t0) + " ms, coils " + Arrays.toString(d.readCoils(0, 10)));
                Thread.sleep(1000);
            }
            d.writeRegister(0, 55);
        }
    }
}
