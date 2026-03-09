package dev.danik.autominer.preset;

import dev.danik.autominer.config.AutoMinerConfig;

public record AutoMinerPreset(String name, String description, boolean builtIn, AutoMinerConfig snapshot) {
}
