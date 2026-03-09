package dev.danik.autominer.gui;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.core.AutoMinerBootstrap;
import dev.danik.autominer.gui.widget.ValueSliderWidget;
import dev.danik.autominer.input.KeybindManager;
import dev.danik.autominer.input.ModKeyAction;
import dev.danik.autominer.mining.AutoMinerController;
import dev.danik.autominer.preset.AutoMinerPreset;
import dev.danik.autominer.preset.PresetManager;
import dev.danik.autominer.util.RegistryUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class AutoMinerScreen extends Screen {
    private static final int BACKGROUND_TOP = 0xD1141820;
    private static final int BACKGROUND_BOTTOM = 0xDE0B0F16;
    private static final int PANEL_OUTLINE = 0x995AD1B8;
    private static final int PANEL_FILL = 0x7A11161E;
    private final AutoMinerBootstrap bootstrap = AutoMinerBootstrap.get();
    private final ConfigManager configManager = bootstrap.configManager();
    private final PresetManager presetManager = bootstrap.presetManager();
    private final KeybindManager keybindManager = bootstrap.keybindManager();
    private final AutoMinerController controller = bootstrap.controller();

    private AutoMinerTab currentTab;
    private TextFieldWidget blockSearchField;
    private TextFieldWidget presetNameField;
    private ModKeyAction awaitingKeyAction;
    private int blockPage;
    private int presetPage;

    public AutoMinerScreen() {
        super(Text.literal("Auto Miner"));
        this.currentTab = AutoMinerTab.byId(configManager.config().ui.lastTab);
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        AutoMinerConfig config = configManager.config();
        currentTab = AutoMinerTab.byId(config.ui.lastTab);
        int left = 16;
        int top = 24;
        int tabWidth = 110;
        int contentX = left + tabWidth + 12;
        int contentWidth = width - contentX - 16;

        int tabY = top;
        for (AutoMinerTab tab : AutoMinerTab.values()) {
            AutoMinerTab targetTab = tab;
            addDrawableChild(ButtonWidget.builder(Text.literal(tab.title()), button -> {
                currentTab = targetTab;
                config.ui.lastTab = targetTab.id();
                configManager.save();
                rebuild();
            }).dimensions(left, tabY, tabWidth, 20).build());
            tabY += 24;
        }

        addCommonButtons(width - 420, height - 28);
        switch (currentTab) {
            case DASHBOARD -> addDashboardTab(contentX, top, contentWidth);
            case TARGETS -> addTargetsTab(contentX, top, contentWidth);
            case BEHAVIOR -> addBehaviorTab(contentX, top, contentWidth);
            case ROUTING -> addRoutingTab(contentX, top, contentWidth);
            case SAFETY -> addSafetyTab(contentX, top, contentWidth);
            case HUD -> addHudTab(contentX, top, contentWidth);
            case KEYBINDS -> addKeybindTab(contentX, top, contentWidth);
            case PRESETS -> addPresetsTab(contentX, top, contentWidth);
        }
    }

    private void addCommonButtons(int startX, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        addDrawableChild(ButtonWidget.builder(Text.literal("Start"), button -> controller.toggle(client)).dimensions(startX, y, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), button -> controller.emergencyStop("Stopped from GUI")).dimensions(startX + 64, y, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pause"), button -> controller.togglePause(client)).dimensions(startX + 128, y, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> resetConfig()).dimensions(startX + 192, y, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save Preset"), button -> quickSavePreset()).dimensions(startX + 256, y, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load Preset"), button -> loadActivePreset()).dimensions(startX + 334, y, 74, 20).build());
    }

    private void addDashboardTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        addDrawableChild(cycleButton(x, y, 160, "Target Mode", config.mining.targetMode.name(), button -> {
            config.mining.targetMode = nextEnum(config.mining.targetMode, AutoMinerConfig.TargetMode.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(cycleButton(x + 170, y, 160, "Area Mode", config.mining.areaMode.name(), button -> {
            config.mining.areaMode = nextEnum(config.mining.areaMode, AutoMinerConfig.AreaMode.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(cycleButton(x + 340, y, 180, "Completion", config.mining.completionMode.name(), button -> {
            config.mining.completionMode = nextEnum(config.mining.completionMode, AutoMinerConfig.CompletionMode.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(cycleButton(x, y + 28, 160, "Sort", config.mining.sortMode.name(), button -> {
            config.mining.sortMode = nextEnum(config.mining.sortMode, AutoMinerConfig.TargetSortMode.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(toggleButton(x + 170, y + 28, 160, "HUD", config.hud.enabled, value -> config.hud.enabled = value));
        addDrawableChild(toggleButton(x + 340, y + 28, 180, "Highlights", config.visuals.showCandidateHighlights, value -> config.visuals.showCandidateHighlights = value));
        addDrawableChild(toggleButton(x, y + 56, 160, "Current Target", config.visuals.showCurrentTarget, value -> config.visuals.showCurrentTarget = value));
        addDrawableChild(toggleButton(x + 170, y + 56, 160, "Tracer", config.visuals.showTracer, value -> config.visuals.showTracer = value));
        addDrawableChild(toggleButton(x + 340, y + 56, 180, "Work Area", config.visuals.showWorkArea, value -> config.visuals.showWorkArea = value));
    }

    private void addTargetsTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        blockSearchField = addDrawableChild(new TextFieldWidget(textRenderer, x, y, 220, 20, Text.literal("Block Search")));
        blockSearchField.setText(config.ui.blockSearch);
        blockSearchField.setChangedListener(value -> {
            config.ui.blockSearch = value;
            blockPage = 0;
            configManager.save();
        });

        addDrawableChild(ButtonWidget.builder(Text.literal("Group: Ores"), button -> addGroup("ores")).dimensions(x + 228, y, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Group: Logs"), button -> addGroup("logs")).dimensions(x + 322, y, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Group: Stone"), button -> addGroup("stone")).dimensions(x + 416, y, 90, 20).build());

        List<String> searchResults = RegistryUtil.allBlockIds(config.ui.blockSearch);
        int from = Math.min(blockPage * 6, searchResults.size());
        int to = Math.min(searchResults.size(), from + 6);
        int rowY = y + 32;
        for (int i = from; i < to; i++) {
            String blockId = searchResults.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("+ " + blockId), button -> {
                controller.addTargetBlock(blockId);
                rebuild();
            }).dimensions(x, rowY, 250, 20).build());
            rowY += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Prev"), button -> {
            blockPage = Math.max(0, blockPage - 1);
            rebuild();
        }).dimensions(x + 260, y + 32, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Next"), button -> {
            if ((blockPage + 1) * 6 < searchResults.size()) {
                blockPage++;
                rebuild();
            }
        }).dimensions(x + 314, y + 32, 50, 20).build());

        int targetY = y + 32;
        for (String target : config.blocks.targets.stream().limit(6).toList()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("- " + target), button -> {
                controller.removeTargetBlock(target);
                rebuild();
            }).dimensions(x + 380, targetY, 250, 20).build());
            targetY += 24;
        }

        int recentY = y + 184;
        for (String recent : config.blocks.recent.stream().limit(4).toList()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Recent: " + recent), button -> {
                controller.addTargetBlock(recent);
                rebuild();
            }).dimensions(x, recentY, 250, 20).build());
            recentY += 24;
        }
    }

    private void addBehaviorTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        addDrawableChild(doubleSlider(x, y, 250, 1, 24, 1, config.mining.searchRadius, value -> Text.literal("Search Radius: " + value.intValue()), value -> config.mining.searchRadius = value.intValue()));
        addDrawableChild(doubleSlider(x + 260, y, 250, 4, 32, 0.5, config.mining.maxTargetDistance, value -> Text.literal("Max Distance: " + format(value)), value -> config.mining.maxTargetDistance = value));
        addDrawableChild(doubleSlider(x, y + 28, 250, 0, 10, 1, config.mining.reactionDelayTicks, value -> Text.literal("Reaction Delay: " + value.intValue()), value -> config.mining.reactionDelayTicks = value.intValue()));
        addDrawableChild(doubleSlider(x + 260, y + 28, 250, 0, 10, 1, config.mining.actionDelayTicks, value -> Text.literal("Action Delay: " + value.intValue()), value -> config.mining.actionDelayTicks = value.intValue()));
        addDrawableChild(doubleSlider(x, y + 56, 250, 5, 120, 1, config.mining.refreshIntervalTicks, value -> Text.literal("Refresh Interval: " + value.intValue()), value -> config.mining.refreshIntervalTicks = value.intValue()));
        addDrawableChild(doubleSlider(x + 260, y + 56, 250, 0, 8, 1, config.mining.maxIntermediateBlocks, value -> Text.literal("Intermediate Blocks: " + value.intValue()), value -> config.mining.maxIntermediateBlocks = value.intValue()));

        addDrawableChild(toggleButton(x, y + 92, 160, "Visibility", config.mining.requireVisibility, value -> config.mining.requireVisibility = value));
        addDrawableChild(toggleButton(x + 170, y + 92, 160, "Consider Height", config.mining.considerHeight, value -> config.mining.considerHeight = value));
        addDrawableChild(toggleButton(x + 340, y + 92, 170, "Mine Above", config.mining.mineAbovePlayer, value -> config.mining.mineAbovePlayer = value));
        addDrawableChild(toggleButton(x, y + 120, 160, "Mine Below", config.mining.mineBelowPlayer, value -> config.mining.mineBelowPlayer = value));
        addDrawableChild(toggleButton(x + 170, y + 120, 160, "Break Intermediates", config.mining.allowBreakIntermediates, value -> config.mining.allowBreakIntermediates = value));
        addDrawableChild(toggleButton(x + 340, y + 120, 170, "Quick Select Replaces", config.mining.quickSelectReplacesTargets, value -> config.mining.quickSelectReplacesTargets = value));
        addDrawableChild(toggleButton(x, y + 148, 250, "Avoid Support Block", config.mining.avoidSupportBlock, value -> config.mining.avoidSupportBlock = value));
        addDrawableChild(toggleButton(x + 260, y + 148, 250, "Prefer Horizontal Targets", config.mining.preferHorizontalTargets, value -> config.mining.preferHorizontalTargets = value));
    }
    private void addRoutingTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        addDrawableChild(cycleButton(x, y, 180, "Area Mode", config.mining.areaMode.name(), button -> {
            config.mining.areaMode = nextEnum(config.mining.areaMode, AutoMinerConfig.AreaMode.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(doubleSlider(x + 190, y, 220, 4, 32, 0.5, config.mining.startRadius, value -> Text.literal("Start Radius: " + format(value)), value -> config.mining.startRadius = value));
        addDrawableChild(doubleSlider(x + 420, y, 220, 4, 32, 0.5, config.mining.playerRadius, value -> Text.literal("Player Radius: " + format(value)), value -> config.mining.playerRadius = value));

        addDrawableChild(doubleSlider(x, y + 28, 220, 32, 1024, 16, config.routing.maxVisitedNodes, value -> Text.literal("Max Nodes: " + value.intValue()), value -> config.routing.maxVisitedNodes = value.intValue()));
        addDrawableChild(doubleSlider(x + 230, y + 28, 220, 8, 128, 1, config.routing.maxPathLength, value -> Text.literal("Max Path: " + value.intValue()), value -> config.routing.maxPathLength = value.intValue()));
        addDrawableChild(doubleSlider(x + 460, y + 28, 180, 0, 6, 1, config.routing.maxDrop, value -> Text.literal("Max Drop: " + value.intValue()), value -> config.routing.maxDrop = value.intValue()));

        addDrawableChild(toggleButton(x, y + 64, 150, "Avoid Lava", config.routing.avoidLava, value -> config.routing.avoidLava = value));
        addDrawableChild(toggleButton(x + 160, y + 64, 150, "Avoid Water", config.routing.avoidWater, value -> config.routing.avoidWater = value));
        addDrawableChild(toggleButton(x + 320, y + 64, 150, "Avoid Void", config.routing.avoidVoid, value -> config.routing.avoidVoid = value));
        addDrawableChild(toggleButton(x + 480, y + 64, 160, "Keep Near Start", config.routing.keepNearStart, value -> config.routing.keepNearStart = value));

        addDrawableChild(ButtonWidget.builder(Text.literal("Set Custom Min = Player"), button -> captureAreaCorner(true)).dimensions(x, y + 96, 190, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Set Custom Max = Player"), button -> captureAreaCorner(false)).dimensions(x + 200, y + 96, 190, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Center Area Around Player"), button -> centerAreaAroundPlayer()).dimensions(x + 400, y + 96, 220, 20).build());
    }

    private void addSafetyTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        addDrawableChild(toggleButton(x, y, 180, "Stop Low Durability", config.safety.stopOnLowDurability, value -> config.safety.stopOnLowDurability = value));
        addDrawableChild(toggleButton(x + 190, y, 180, "Stop Full Inventory", config.safety.stopOnFullInventory, value -> config.safety.stopOnFullInventory = value));
        addDrawableChild(toggleButton(x + 380, y, 180, "Stop Low Health", config.safety.stopOnLowHealth, value -> config.safety.stopOnLowHealth = value));

        addDrawableChild(doubleSlider(x, y + 28, 220, 1, 100, 1, config.safety.minToolDurability, value -> Text.literal("Min Durability: " + value.intValue()), value -> config.safety.minToolDurability = value.intValue()));
        addDrawableChild(doubleSlider(x + 230, y + 28, 220, 1, 20, 1, config.safety.minHealth, value -> Text.literal("Min Health: " + format(value)), value -> config.safety.minHealth = value.floatValue()));
        addDrawableChild(doubleSlider(x + 460, y + 28, 180, 1, 20, 1, config.safety.minHunger, value -> Text.literal("Min Hunger: " + value.intValue()), value -> config.safety.minHunger = value.intValue()));

        addDrawableChild(toggleButton(x, y + 64, 180, "Stop Low Hunger", config.safety.stopOnLowHunger, value -> config.safety.stopOnLowHunger = value));
        addDrawableChild(toggleButton(x + 190, y + 64, 180, "Pause On Screen", config.safety.pauseOnOpenScreen, value -> config.safety.pauseOnOpenScreen = value));
        addDrawableChild(toggleButton(x + 380, y + 64, 180, "Pause On Damage", config.safety.pauseOnDamage, value -> config.safety.pauseOnDamage = value));
        addDrawableChild(toggleButton(x, y + 92, 180, "Pause On Lost Target", config.safety.pauseOnLostTarget, value -> config.safety.pauseOnLostTarget = value));
        addDrawableChild(toggleButton(x + 190, y + 92, 180, "Emergency Hazard Stop", config.safety.emergencyStopOnHazard, value -> config.safety.emergencyStopOnHazard = value));
        addDrawableChild(toggleButton(x + 380, y + 92, 180, "Prevent Forbidden", config.safety.preventForbiddenBlockBreak, value -> config.safety.preventForbiddenBlockBreak = value));

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Custom Area To Blacklist"), button -> addZone(true)).dimensions(x, y + 128, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Custom Area To Whitelist"), button -> addZone(false)).dimensions(x + 220, y + 128, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Zone Lists"), button -> {
            config.zones.blacklist.clear();
            config.zones.whitelist.clear();
            configManager.save();
            rebuild();
        }).dimensions(x + 440, y + 128, 160, 20).build());
    }

    private void addHudTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        addDrawableChild(toggleButton(x, y, 160, "HUD Enabled", config.hud.enabled, value -> config.hud.enabled = value));
        addDrawableChild(cycleButton(x + 170, y, 160, "Style", config.hud.style.name(), button -> {
            config.hud.style = nextEnum(config.hud.style, AutoMinerConfig.HudStyle.values());
            configManager.save();
            rebuild();
        }));
        addDrawableChild(ButtonWidget.builder(Text.literal("Cycle Color"), button -> {
            config.hud.color = nextHudColor(config.hud.color);
            configManager.save();
            rebuild();
        }).dimensions(x + 340, y, 160, 20).build());

        addDrawableChild(doubleSlider(x, y + 28, 220, 0, 400, 1, config.hud.x, value -> Text.literal("HUD X: " + value.intValue()), value -> config.hud.x = value.intValue()));
        addDrawableChild(doubleSlider(x + 230, y + 28, 220, 0, 300, 1, config.hud.y, value -> Text.literal("HUD Y: " + value.intValue()), value -> config.hud.y = value.intValue()));
        addDrawableChild(doubleSlider(x + 460, y + 28, 180, 0.5, 2.5, 0.05, config.hud.scale, value -> Text.literal("Scale: " + format(value)), value -> config.hud.scale = value.floatValue()));
        addDrawableChild(doubleSlider(x, y + 56, 220, 0.1, 1.0, 0.05, config.hud.alpha, value -> Text.literal("Alpha: " + format(value)), value -> config.hud.alpha = value.floatValue()));

        addDrawableChild(toggleButton(x, y + 92, 140, "Show Mode", config.hud.showMode, value -> config.hud.showMode = value));
        addDrawableChild(toggleButton(x + 150, y + 92, 140, "Show Target", config.hud.showTarget, value -> config.hud.showTarget = value));
        addDrawableChild(toggleButton(x + 300, y + 92, 160, "Show Found Count", config.hud.showFoundCount, value -> config.hud.showFoundCount = value));
        addDrawableChild(toggleButton(x + 470, y + 92, 150, "Show Mined Count", config.hud.showMinedCount, value -> config.hud.showMinedCount = value));
        addDrawableChild(toggleButton(x, y + 120, 140, "Show State", config.hud.showState, value -> config.hud.showState = value));
        addDrawableChild(toggleButton(x + 150, y + 120, 140, "Show Warnings", config.hud.showWarnings, value -> config.hud.showWarnings = value));
        addDrawableChild(toggleButton(x + 300, y + 120, 160, "Show Restrictions", config.hud.showRestrictions, value -> config.hud.showRestrictions = value));
        addDrawableChild(toggleButton(x + 470, y + 120, 150, "Show Preset", config.hud.showPreset, value -> config.hud.showPreset = value));
    }

    private void addKeybindTab(int x, int y, int width) {
        int index = 0;
        for (ModKeyAction action : ModKeyAction.values()) {
            int column = index / 5;
            int row = index % 5;
            int buttonX = x + column * 320;
            int buttonY = y + row * 28;
            String suffix = awaitingKeyAction == action ? " [press key]" : "";
            addDrawableChild(ButtonWidget.builder(Text.literal(action.displayName() + ": " + keybindManager.binding(action).getBoundKeyLocalizedText().getString() + suffix), button -> {
                awaitingKeyAction = action;
                rebuild();
            }).dimensions(buttonX, buttonY, 300, 20).build());
            index++;
        }
    }

    private void addPresetsTab(int x, int y, int width) {
        AutoMinerConfig config = configManager.config();
        presetNameField = addDrawableChild(new TextFieldWidget(textRenderer, x, y, 220, 20, Text.literal("Preset Name")));
        presetNameField.setText(config.activePreset);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save Current"), button -> {
            if (presetManager.saveCurrentAsPreset(presetNameField.getText(), "Saved from GUI")) {
                config.activePreset = presetNameField.getText();
                configManager.save();
                rebuild();
            }
        }).dimensions(x + 228, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Export"), button -> presetManager.exportPreset(presetNameField.getText())).dimensions(x + 332, y, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Import"), button -> {
            presetManager.importPreset(presetNameField.getText());
            rebuild();
        }).dimensions(x + 416, y, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load"), button -> {
            config.activePreset = presetNameField.getText();
            loadActivePreset();
        }).dimensions(x + 500, y, 80, 20).build());

        List<AutoMinerPreset> presets = presetManager.presets().stream().sorted(Comparator.comparing(AutoMinerPreset::name)).toList();
        int from = Math.min(presetPage * 8, presets.size());
        int to = Math.min(presets.size(), from + 8);
        int rowY = y + 32;
        for (int i = from; i < to; i++) {
            AutoMinerPreset preset = presets.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal((preset.builtIn() ? "[Built-in] " : "") + preset.name()), button -> {
                config.activePreset = preset.name();
                configManager.save();
                rebuild();
            }).dimensions(x, rowY, 340, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Load"), button -> {
                config.activePreset = preset.name();
                loadActivePreset();
            }).dimensions(x + 348, rowY, 60, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Export"), button -> presetManager.exportPreset(preset.name())).dimensions(x + 412, rowY, 70, 20).build());
            rowY += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Prev"), button -> {
            presetPage = Math.max(0, presetPage - 1);
            rebuild();
        }).dimensions(x + 500, y + 32, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Next"), button -> {
            if ((presetPage + 1) * 8 < presets.size()) {
                presetPage++;
                rebuild();
            }
        }).dimensions(x + 554, y + 32, 50, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (awaitingKeyAction != null) {
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            keybindManager.setBinding(awaitingKeyAction, key);
            awaitingKeyAction = null;
            rebuild();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        configManager.save();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderSafeBackground(context);
        drawPanels(context);
        super.render(context, mouseX, mouseY, delta);

        int contentX = 138;
        context.drawText(textRenderer, title.copy().formatted(Formatting.AQUA), contentX, 8, 0xFFFFFF, true);
        context.drawText(textRenderer, Text.literal("State: " + controller.state().name()), contentX, height - 74, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Status: " + controller.stats().statusMessage()), contentX, height - 62, 0xCFCFCF, false);
        context.drawText(textRenderer, Text.literal("Target: " + (controller.stats().currentTarget() == null ? "-" : controller.stats().currentTarget().blockId())), contentX, height - 50, 0xCFCFCF, false);
        context.drawText(textRenderer, Text.literal("Found: " + controller.stats().foundBlocks() + "   Mined: " + controller.stats().minedBlocks()), contentX, height - 38, 0xCFCFCF, false);
        context.drawText(textRenderer, Text.literal("Restrictions: " + controller.stats().restrictionSummary()), contentX, height - 26, 0xCFCFCF, false);

        renderTabInfo(context, contentX, 170);
    }

    private void renderSafeBackground(DrawContext context) {
        context.fillGradient(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);
    }

    private void drawPanels(DrawContext context) {
        int left = 12;
        int top = 20;
        int tabsRight = 130;
        int contentLeft = 136;
        int bottom = height - 82;
        int footerTop = height - 84;

        context.fill(left, top, tabsRight, bottom, PANEL_FILL);
        context.fill(tabsRight, top, tabsRight + 1, bottom, PANEL_OUTLINE);
        context.fill(contentLeft, top, width - 12, bottom, PANEL_FILL);
        context.fill(contentLeft - 1, top, contentLeft, bottom, PANEL_OUTLINE);
        context.fill(contentLeft, footerTop, width - 12, height - 12, PANEL_FILL);
        context.fill(contentLeft, footerTop, width - 12, footerTop + 1, PANEL_OUTLINE);
    }

    private void renderTabInfo(DrawContext context, int x, int y) {
        AutoMinerConfig config = configManager.config();
        List<String> lines = new ArrayList<>();
        switch (currentTab) {
            case DASHBOARD -> {
                lines.add("Preset: " + config.activePreset);
                lines.add("Mode: " + config.mining.targetMode + " / " + config.mining.areaMode);
                lines.add("Completion: " + config.mining.completionMode + " / " + config.mining.sortMode);
                lines.add("Current candidates: " + controller.visibleCandidates().size());
            }
            case TARGETS -> {
                lines.add("Targets: " + config.blocks.targets.size());
                lines.add("Ignored: " + config.blocks.ignored.size());
                lines.add("Priority: " + config.blocks.priority.size());
                lines.add("Groups: " + config.blocks.groups);
            }
            case BEHAVIOR -> {
                lines.add("Search radius: " + config.mining.searchRadius);
                lines.add("Max distance: " + format(config.mining.maxTargetDistance));
                lines.add("Delay: reaction " + config.mining.reactionDelayTicks + ", action " + config.mining.actionDelayTicks);
                lines.add("Visibility: " + config.mining.requireVisibility + ", height: " + config.mining.considerHeight);
                lines.add("Support block: " + config.mining.avoidSupportBlock + ", horizontal bias: " + config.mining.preferHorizontalTargets);
            }
            case ROUTING -> {
                lines.add("Max nodes: " + config.routing.maxVisitedNodes + ", max path: " + config.routing.maxPathLength);
                lines.add("Drop: " + config.routing.maxDrop + ", step: " + config.routing.maxStepHeight);
                lines.add("Custom area min: " + posText(config.zones.customArea.minX, config.zones.customArea.minY, config.zones.customArea.minZ));
                lines.add("Custom area max: " + posText(config.zones.customArea.maxX, config.zones.customArea.maxY, config.zones.customArea.maxZ));
            }
            case SAFETY -> {
                lines.add("Blacklist zones: " + config.zones.blacklist.size());
                lines.add("Whitelist zones: " + config.zones.whitelist.size());
                lines.add("Health floor: " + format(config.safety.minHealth) + ", hunger floor: " + config.safety.minHunger);
                lines.add("Warnings: " + controller.stats().warnings());
            }
            case HUD -> {
                lines.add("HUD pos: " + config.hud.x + ", " + config.hud.y);
                lines.add("Scale: " + format(config.hud.scale) + ", alpha: " + format(config.hud.alpha));
                lines.add("Style: " + config.hud.style + ", color: #" + Integer.toHexString(config.hud.color).toUpperCase());
            }
            case KEYBINDS -> lines.addAll(keybindManager.findConflicts(MinecraftClient.getInstance()));
            case PRESETS -> {
                lines.add("Recent presets: " + config.recentPresets);
                lines.add("Last loaded preset: " + config.session.lastLoadedPreset);
                lines.add("Available presets: " + presetManager.presetNames().size());
            }
        }

        int rowY = y;
        for (String line : lines.stream().limit(8).toList()) {
            context.drawText(textRenderer, Text.literal(line), x, rowY, 0xD9D9D9, false);
            rowY += 12;
        }
    }

    private ButtonWidget toggleButton(int x, int y, int width, String label, boolean value, java.util.function.Consumer<Boolean> consumer) {
        return ButtonWidget.builder(Text.literal(label + ": " + (value ? "ON" : "OFF")), button -> {
            consumer.accept(!value);
            configManager.save();
            rebuild();
        }).dimensions(x, y, width, 20).build();
    }

    private ButtonWidget cycleButton(int x, int y, int width, String label, String value, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(Text.literal(label + ": " + value), action).dimensions(x, y, width, 20).build();
    }

    private ValueSliderWidget doubleSlider(int x, int y, int width, double min, double max, double step, double current,
                                           java.util.function.Function<Double, Text> labelFactory, java.util.function.Consumer<Double> consumer) {
        return new ValueSliderWidget(x, y, width, 20, min, max, step, current, labelFactory, value -> {
            consumer.accept(value);
            configManager.save();
        });
    }

    private void addGroup(String group) {
        AutoMinerConfig config = configManager.config();
        if (!config.blocks.groups.contains(group)) {
            config.blocks.groups.add(group);
        }
        configManager.save();
        rebuild();
    }

    private void captureAreaCorner(boolean minCorner) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        BlockPos pos = client.player.getBlockPos();
        if (minCorner) {
            configManager.config().zones.customArea.minX = pos.getX();
            configManager.config().zones.customArea.minY = pos.getY();
            configManager.config().zones.customArea.minZ = pos.getZ();
        } else {
            configManager.config().zones.customArea.maxX = pos.getX();
            configManager.config().zones.customArea.maxY = pos.getY();
            configManager.config().zones.customArea.maxZ = pos.getZ();
        }
        configManager.save();
        rebuild();
    }

    private void centerAreaAroundPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        BlockPos pos = client.player.getBlockPos();
        int radius = (int) Math.round(configManager.config().mining.playerRadius);
        configManager.config().zones.customArea.minX = pos.getX() - radius;
        configManager.config().zones.customArea.maxX = pos.getX() + radius;
        configManager.config().zones.customArea.minY = pos.getY() - 4;
        configManager.config().zones.customArea.maxY = pos.getY() + 4;
        configManager.config().zones.customArea.minZ = pos.getZ() - radius;
        configManager.config().zones.customArea.maxZ = pos.getZ() + radius;
        configManager.save();
        rebuild();
    }

    private void addZone(boolean blacklist) {
        AutoMinerConfig.ZoneArea source = configManager.config().zones.customArea;
        AutoMinerConfig.ZoneArea copy = new AutoMinerConfig.ZoneArea();
        copy.name = blacklist ? "Blacklisted Area" : "Whitelisted Area";
        copy.minX = source.minX;
        copy.minY = source.minY;
        copy.minZ = source.minZ;
        copy.maxX = source.maxX;
        copy.maxY = source.maxY;
        copy.maxZ = source.maxZ;
        if (blacklist) {
            configManager.config().zones.blacklist.add(copy);
        } else {
            configManager.config().zones.whitelist.add(copy);
        }
        configManager.save();
        rebuild();
    }

    private void quickSavePreset() {
        String name = "quick_gui_" + System.currentTimeMillis();
        if (presetManager.saveCurrentAsPreset(name, "Quick save from GUI")) {
            configManager.config().activePreset = name;
            configManager.save();
            rebuild();
        }
    }

    private void loadActivePreset() {
        String name = configManager.config().activePreset;
        if (presetNameField != null && !presetNameField.getText().isBlank()) {
            name = presetNameField.getText();
        }
        if (presetManager.loadIntoConfig(name)) {
            configManager.config().activePreset = name;
            keybindManager.reloadFromConfig();
            configManager.save();
            rebuild();
        }
    }

    private void resetConfig() {
        AutoMinerConfig current = configManager.copyConfig();
        AutoMinerConfig reset = new AutoMinerConfig();
        reset.keybinds = current.keybinds;
        reset.sanitize();
        configManager.replace(reset);
        keybindManager.reloadFromConfig();
        rebuild();
    }

    private int nextHudColor(int currentColor) {
        int[] palette = new int[] {0x5AD1B8, 0xF4B942, 0xFF6B6B, 0x6FA8FF, 0xD9D9D9};
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == currentColor) {
                return palette[(i + 1) % palette.length];
            }
        }
        return palette[0];
    }

    private <E extends Enum<E>> E nextEnum(E current, E[] values) {
        return values[(current.ordinal() + 1) % values.length];
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String posText(int x, int y, int z) {
        return x + ", " + y + ", " + z;
    }
}




