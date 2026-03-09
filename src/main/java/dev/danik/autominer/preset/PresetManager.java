package dev.danik.autominer.preset;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.config.ConfigManager;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PresetManager {
    private final ConfigManager configManager;
    private final Map<String, AutoMinerPreset> presets = new LinkedHashMap<>();
    private final Path presetDir;

    public PresetManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.presetDir = configManager.configDir().resolve("presets");
    }

    public void initialize() {
        presets.clear();
        presets.putAll(BuiltInPresets.createDefaults());
        try {
            Files.createDirectories(presetDir);
            Files.list(presetDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted()
                .forEach(this::loadExternalPreset);
        } catch (IOException ignored) {
        }
    }

    public Collection<AutoMinerPreset> presets() {
        return presets.values();
    }

    public List<String> presetNames() {
        return new ArrayList<>(presets.keySet());
    }

    public Optional<AutoMinerPreset> find(String name) {
        return Optional.ofNullable(presets.get(name));
    }

    public boolean loadIntoConfig(String name) {
        AutoMinerPreset preset = presets.get(name);
        if (preset == null) {
            return false;
        }

        AutoMinerConfig current = configManager.copyConfig();
        AutoMinerConfig snapshot = configManager.gson().fromJson(configManager.gson().toJson(preset.snapshot()), AutoMinerConfig.class);
        snapshot.enabled = false;
        snapshot.paused = false;
        snapshot.keybinds = current.keybinds;
        snapshot.hud = current.hud;
        snapshot.ui = current.ui;
        snapshot.sanitize();
        snapshot.activePreset = name;
        snapshot.session.lastLoadedPreset = name;
        configManager.replace(snapshot);
        addRecentPreset(name);
        return true;
    }

    public boolean saveCurrentAsPreset(String name, String description) {
        if (name == null || name.isBlank()) {
            return false;
        }
        AutoMinerConfig snapshot = configManager.copyConfig();
        snapshot.enabled = false;
        snapshot.paused = false;
        AutoMinerPreset preset = new AutoMinerPreset(name.trim(), description == null ? "Custom preset" : description, false, snapshot);
        presets.put(preset.name(), preset);
        exportPreset(preset.name());
        addRecentPreset(preset.name());
        return true;
    }

    public boolean exportPreset(String name) {
        AutoMinerPreset preset = presets.get(name);
        if (preset == null) {
            return false;
        }
        try {
            Files.createDirectories(presetDir);
            try (Writer writer = Files.newBufferedWriter(presetDir.resolve(fileName(name)))) {
                configManager.gson().toJson(preset, writer);
            }
            configManager.config().session.lastExportTimestamp = System.currentTimeMillis();
            configManager.save();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public boolean importPreset(String name) {
        Path path = presetDir.resolve(fileName(name));
        if (!Files.exists(path)) {
            return false;
        }
        return loadExternalPreset(path);
    }

    private boolean loadExternalPreset(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            AutoMinerPreset preset = configManager.gson().fromJson(reader, AutoMinerPreset.class);
            if (preset == null || preset.name() == null || preset.name().isBlank() || preset.snapshot() == null) {
                return false;
            }
            preset.snapshot().sanitize();
            presets.put(preset.name(), preset);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void addRecentPreset(String name) {
        List<String> recent = configManager.config().recentPresets;
        recent.remove(name);
        recent.add(0, name);
        while (recent.size() > 6) {
            recent.remove(recent.size() - 1);
        }
        configManager.save();
    }

    private String fileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9 _-]", "_").trim().replace(' ', '_') + ".json";
    }
}
