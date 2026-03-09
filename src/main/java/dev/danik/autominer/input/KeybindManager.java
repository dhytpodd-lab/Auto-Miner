package dev.danik.autominer.input;

import dev.danik.autominer.AutoMinerConstants;
import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.core.AutoMinerBootstrap;
import dev.danik.autominer.gui.AutoMinerScreen;
import dev.danik.autominer.mining.AutoMinerController;
import dev.danik.autominer.preset.PresetManager;
import dev.danik.autominer.util.BlockSelectionHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class KeybindManager {
    private static final DateTimeFormatter QUICK_PRESET_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String CATEGORY = "category.autominer";

    private final ConfigManager configManager;
    private final PresetManager presetManager;
    private final AutoMinerController controller;
    private final Map<ModKeyAction, KeyBinding> bindings = new EnumMap<>(ModKeyAction.class);

    public KeybindManager(ConfigManager configManager, PresetManager presetManager, AutoMinerController controller) {
        this.configManager = configManager;
        this.presetManager = presetManager;
        this.controller = controller;
    }

    public void registerAll() {
        bindings.clear();
        configManager.config().keybinds.ensureDefaults();
        for (ModKeyAction action : ModKeyAction.values()) {
            InputUtil.Key input = InputUtil.fromTranslationKey(configManager.config().keybinds.bindings.get(action.id()));
            KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + AutoMinerConstants.MOD_ID + "." + action.id(),
                input.getCategory(),
                input.getCode(),
                CATEGORY
            ));
            bindings.put(action, binding);
        }
    }

    public void reloadFromConfig() {
        configManager.config().keybinds.ensureDefaults();
        for (Map.Entry<ModKeyAction, KeyBinding> entry : bindings.entrySet()) {
            String keyName = configManager.config().keybinds.bindings.get(entry.getKey().id());
            entry.getValue().setBoundKey(InputUtil.fromTranslationKey(keyName));
        }
        KeyBinding.updateKeysByCode();
    }

    public void handleTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        for (Map.Entry<ModKeyAction, KeyBinding> entry : bindings.entrySet()) {
            while (entry.getValue().wasPressed()) {
                handleAction(client, entry.getKey());
            }
        }
    }

    public void setBinding(ModKeyAction action, InputUtil.Key key) {
        configManager.config().keybinds.bindings.put(action.id(), key.getTranslationKey());
        KeyBinding binding = bindings.get(action);
        if (binding != null) {
            binding.setBoundKey(key);
            KeyBinding.updateKeysByCode();
        }
        configManager.save();
    }

    public KeyBinding binding(ModKeyAction action) {
        return bindings.get(action);
    }

    public List<String> findConflicts(MinecraftClient client) {
        List<String> conflicts = new ArrayList<>();
        for (Map.Entry<ModKeyAction, KeyBinding> first : bindings.entrySet()) {
            for (Map.Entry<ModKeyAction, KeyBinding> second : bindings.entrySet()) {
                if (first.getKey().ordinal() >= second.getKey().ordinal()) {
                    continue;
                }
                if (sameKey(first.getValue(), second.getValue())) {
                    conflicts.add(first.getKey().displayName() + " conflicts with " + second.getKey().displayName());
                }
            }
        }

        if (client != null) {
            List<KeyBinding> important = List.of(
                client.options.attackKey,
                client.options.useKey,
                client.options.forwardKey,
                client.options.backKey,
                client.options.leftKey,
                client.options.rightKey,
                client.options.jumpKey,
                client.options.inventoryKey,
                client.options.chatKey
            );
            for (Map.Entry<ModKeyAction, KeyBinding> entry : bindings.entrySet()) {
                for (KeyBinding vanilla : important) {
                    if (sameKey(entry.getValue(), vanilla)) {
                        conflicts.add(entry.getKey().displayName() + " conflicts with " + vanilla.getBoundKeyLocalizedText().getString());
                    }
                }
            }
        }
        return conflicts;
    }

    private void handleAction(MinecraftClient client, ModKeyAction action) {
        switch (action) {
            case TOGGLE -> controller.toggle(client);
            case PAUSE -> controller.togglePause(client);
            case OPEN_GUI -> client.setScreen(new AutoMinerScreen());
            case SELECT_LOOKED_BLOCK -> {
                Identifier blockId = BlockSelectionHelper.getCrosshairBlockId(client);
                if (blockId != null) {
                    controller.replaceTargetsWith(blockId.toString());
                }
            }
            case ADD_LOOKED_BLOCK -> {
                Identifier blockId = BlockSelectionHelper.getCrosshairBlockId(client);
                if (blockId != null) {
                    controller.addTargetBlock(blockId.toString());
                }
            }
            case REMOVE_LOOKED_BLOCK -> {
                Identifier blockId = BlockSelectionHelper.getCrosshairBlockId(client);
                if (blockId != null) {
                    controller.removeTargetBlock(blockId.toString());
                }
            }
            case TOGGLE_HUD -> {
                configManager.config().hud.enabled = !configManager.config().hud.enabled;
                configManager.save();
            }
            case SAVE_PRESET -> presetManager.saveCurrentAsPreset("quick_" + QUICK_PRESET_FORMAT.format(LocalDateTime.now()), "Quick saved preset");
            case LOAD_PRESET -> presetManager.loadIntoConfig(configManager.config().session.lastLoadedPreset);
            case EMERGENCY_STOP -> controller.emergencyStop("Emergency stop key pressed");
        }
    }

    private boolean sameKey(KeyBinding first, KeyBinding second) {
        return first != null && second != null && first.getBoundKeyTranslationKey().equals(second.getBoundKeyTranslationKey());
    }
}
