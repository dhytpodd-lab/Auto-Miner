package dev.danik.autominer;

import dev.danik.autominer.core.AutoMinerBootstrap;
import net.fabricmc.api.ClientModInitializer;

public final class AutoMinerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoMinerBootstrap.initialize();
    }
}
