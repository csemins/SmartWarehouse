package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.plc.CraneConfig;
import bd.ac.kuet.swms.plc.Side;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String factoryIoHost = "192.168.0.111";
    public int factoryIoPort = 502;
    public int slaveId = 1;
    public Side loadSide = Side.LEFT;
    public Side unloadSide = Side.RIGHT;
    public Side rackSide = Side.RIGHT;
    // factory io conveyor sensors read TRUE when nothing is in front of them (!!)
    public boolean conveyorSensorsInverted = true;
    public int stepTimeoutSeconds = 60;

    public CraneConfig craneConfig() {
        return new CraneConfig(loadSide, unloadSide, rackSide, 55, conveyorSensorsInverted, stepTimeoutSeconds);
    }

    // broken / missing json -> just fall back to defaults
    public static AppSettings load(Path file) {
        if (!Files.exists(file)) return new AppSettings();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            AppSettings settings = GSON.fromJson(reader, AppSettings.class);
            return settings == null ? new AppSettings() : settings;
        } catch (IOException | RuntimeException e) {
            return new AppSettings();
        }
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            throw new BusinessException("Cannot save settings: " + e.getMessage());
        }
    }
}
