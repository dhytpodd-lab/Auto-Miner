package dev.danik.autominer.input;

public enum ModKeyAction {
    TOGGLE("toggle", "Toggle Mod", "key.keyboard.grave.accent"),
    PAUSE("pause", "Pause / Resume", "key.keyboard.p"),
    OPEN_GUI("open_gui", "Open GUI", "key.keyboard.right.shift"),
    SELECT_LOOKED_BLOCK("select_looked_block", "Select Block Under Crosshair", "key.keyboard.v"),
    ADD_LOOKED_BLOCK("add_looked_block", "Add Block Under Crosshair", "key.keyboard.b"),
    REMOVE_LOOKED_BLOCK("remove_looked_block", "Remove Block From Targets", "key.keyboard.n"),
    TOGGLE_HUD("toggle_hud", "Toggle HUD", "key.keyboard.h"),
    SAVE_PRESET("save_preset", "Save Preset", "key.keyboard.k"),
    LOAD_PRESET("load_preset", "Load Preset", "key.keyboard.l"),
    EMERGENCY_STOP("emergency_stop", "Emergency Stop", "key.keyboard.delete");

    private final String id;
    private final String displayName;
    private final String defaultTranslationKey;

    ModKeyAction(String id, String displayName, String defaultTranslationKey) {
        this.id = id;
        this.displayName = displayName;
        this.defaultTranslationKey = defaultTranslationKey;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultTranslationKey() {
        return defaultTranslationKey;
    }
}
