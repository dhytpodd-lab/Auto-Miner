package dev.danik.autominer.gui;

public enum AutoMinerTab {
    DASHBOARD("dashboard", "Dashboard"),
    TARGETS("targets", "Targets"),
    BEHAVIOR("behavior", "Behavior"),
    ROUTING("routing", "Routing"),
    SAFETY("safety", "Safety"),
    HUD("hud", "HUD"),
    KEYBINDS("keybinds", "Keybinds"),
    PRESETS("presets", "Presets");

    private final String id;
    private final String title;

    AutoMinerTab(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public static AutoMinerTab byId(String id) {
        for (AutoMinerTab tab : values()) {
            if (tab.id.equalsIgnoreCase(id)) {
                return tab;
            }
        }
        return DASHBOARD;
    }
}
