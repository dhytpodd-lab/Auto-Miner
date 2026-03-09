package dev.danik.autominer.preset;

import dev.danik.autominer.config.AutoMinerConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class BuiltInPresets {
    private BuiltInPresets() {
    }

    public static Map<String, AutoMinerPreset> createDefaults() {
        Map<String, AutoMinerPreset> presets = new LinkedHashMap<>();
        presets.put("rudokop", preset("rudokop", "Ore-oriented profile with visibility and safety checks.", config -> {
            config.mining.targetMode = AutoMinerConfig.TargetMode.LIST;
            config.blocks.groups.add("ores");
            config.mining.requireVisibility = true;
            config.mining.maxTargetDistance = 18.0D;
            config.routing.avoidLava = true;
            config.safety.stopOnLowDurability = true;
            config.safety.pauseOnLostTarget = true;
            config.visuals.showCandidateHighlights = true;
        }));
        presets.put("tree", preset("tree", "Targets wood blocks with a tighter vertical work area.", config -> {
            config.blocks.groups.add("logs");
            config.mining.mineAbovePlayer = true;
            config.mining.mineBelowPlayer = false;
            config.mining.requireVisibility = false;
            config.routing.keepNearStart = true;
            config.mining.startRadius = 10.0D;
        }));
        presets.put("stone", preset("stone", "Broad mining for stone-like blocks inside a fixed radius.", config -> {
            config.blocks.groups.add("stone");
            config.mining.targetMode = AutoMinerConfig.TargetMode.ALL_MATCHING;
            config.mining.sortMode = AutoMinerConfig.TargetSortMode.MINING_SPEED;
            config.mining.playerRadius = 14.0D;
        }));
        presets.put("focused_mining", preset("focused_mining", "Single-block precise mode centered around one target type.", config -> {
            config.mining.targetMode = AutoMinerConfig.TargetMode.SINGLE;
            config.mining.requireVisibility = true;
            config.mining.maxBlocksToMine = 32;
            config.mining.completionMode = AutoMinerConfig.CompletionMode.BLOCK_LIMIT;
            config.routing.maxVisitedNodes = 160;
        }));
        presets.put("safe_mode", preset("safe_mode", "Conservative preset with strict safety thresholds and short routes.", config -> {
            config.mining.requireVisibility = true;
            config.routing.maxPathLength = 32;
            config.routing.avoidLava = true;
            config.routing.avoidVoid = true;
            config.safety.stopOnLowHealth = true;
            config.safety.minHealth = 14.0F;
            config.safety.stopOnLowHunger = true;
            config.safety.minHunger = 10;
            config.safety.stopOnFullInventory = true;
            config.safety.pauseOnOpenScreen = true;
        }));
        return presets;
    }

    private static AutoMinerPreset preset(String name, String description, Consumer<AutoMinerConfig> consumer) {
        AutoMinerConfig config = new AutoMinerConfig();
        config.sanitize();
        consumer.accept(config);
        config.activePreset = name;
        config.session.lastLoadedPreset = name;
        return new AutoMinerPreset(name, description, true, config);
    }
}
