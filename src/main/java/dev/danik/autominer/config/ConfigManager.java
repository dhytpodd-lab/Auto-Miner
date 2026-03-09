package dev.danik.autominer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Path configFile;
    private AutoMinerConfig config = new AutoMinerConfig();

    public ConfigManager() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("autominer");
        this.configFile = configDir.resolve("client.json");
    }

    public AutoMinerConfig config() {
        return config;
    }

    public void load() {
        try {
            Files.createDirectories(configDir);
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile)) {
                    AutoMinerConfig loaded = GSON.fromJson(reader, AutoMinerConfig.class);
                    config = loaded == null ? new AutoMinerConfig() : loaded;
                }
            }
        } catch (IOException ignored) {
            config = new AutoMinerConfig();
        }

        config.sanitize();
        save();
    }

    public void save() {
        config.sanitize();
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public AutoMinerConfig copyConfig() {
        return GSON.fromJson(GSON.toJson(config), AutoMinerConfig.class);
    }

    public void replace(AutoMinerConfig replacement) {
        this.config = replacement == null ? new AutoMinerConfig() : replacement;
        this.config.sanitize();
        save();
    }

    public Path configDir() {
        return configDir;
    }

    public Path configFile() {
        return configFile;
    }

    public Gson gson() {
        return GSON;
    }
}
