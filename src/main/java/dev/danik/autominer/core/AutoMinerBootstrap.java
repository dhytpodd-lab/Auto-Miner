package dev.danik.autominer.core;

import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.hud.AutoMinerHudRenderer;
import dev.danik.autominer.input.KeybindManager;
import dev.danik.autominer.mining.AutoMinerController;
import dev.danik.autominer.preset.PresetManager;
import dev.danik.autominer.render.WorldOverlayRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class AutoMinerBootstrap {
    private static AutoMinerBootstrap instance;

    private final ConfigManager configManager;
    private final PresetManager presetManager;
    private final KeybindManager keybindManager;
    private final AutoMinerController controller;
    private final AutoMinerHudRenderer hudRenderer;
    private final WorldOverlayRenderer overlayRenderer;

    private AutoMinerBootstrap() {
        this.configManager = new ConfigManager();
        this.presetManager = new PresetManager(configManager);
        this.controller = new AutoMinerController(configManager, presetManager);
        this.keybindManager = new KeybindManager(configManager, presetManager, controller);
        this.hudRenderer = new AutoMinerHudRenderer(configManager, controller);
        this.overlayRenderer = new WorldOverlayRenderer(configManager, controller);
    }

    public static void initialize() {
        if (instance != null) {
            return;
        }

        instance = new AutoMinerBootstrap();
        instance.configManager.load();
        instance.presetManager.initialize();
        instance.keybindManager.registerAll();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            instance.keybindManager.handleTick(client);
            instance.controller.tick(client);
        });
        HudRenderCallback.EVENT.register(instance.hudRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(instance.overlayRenderer::render);
    }

    public static AutoMinerBootstrap get() {
        return instance;
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public PresetManager presetManager() {
        return presetManager;
    }

    public KeybindManager keybindManager() {
        return keybindManager;
    }

    public AutoMinerController controller() {
        return controller;
    }
}
