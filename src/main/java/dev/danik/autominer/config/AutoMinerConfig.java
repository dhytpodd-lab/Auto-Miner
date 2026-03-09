package dev.danik.autominer.config;

import dev.danik.autominer.input.ModKeyAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;

public final class AutoMinerConfig {
    public boolean enabled = false;
    public boolean paused = false;
    public String activePreset = "rudokop";
    public MiningSettings mining = new MiningSettings();
    public RoutingSettings routing = new RoutingSettings();
    public SafetySettings safety = new SafetySettings();
    public HudSettings hud = new HudSettings();
    public VisualSettings visuals = new VisualSettings();
    public BlockSettings blocks = new BlockSettings();
    public KeybindSettings keybinds = new KeybindSettings();
    public UiSettings ui = new UiSettings();
    public ZoneSettings zones = new ZoneSettings();
    public SessionSettings session = new SessionSettings();
    public List<String> recentPresets = new ArrayList<>();

    public void sanitize() {
        if (mining == null) {
            mining = new MiningSettings();
        }
        if (routing == null) {
            routing = new RoutingSettings();
        }
        if (safety == null) {
            safety = new SafetySettings();
        }
        if (hud == null) {
            hud = new HudSettings();
        }
        if (visuals == null) {
            visuals = new VisualSettings();
        }
        if (blocks == null) {
            blocks = new BlockSettings();
        }
        if (keybinds == null) {
            keybinds = new KeybindSettings();
        }
        if (ui == null) {
            ui = new UiSettings();
        }
        if (zones == null) {
            zones = new ZoneSettings();
        }
        if (session == null) {
            session = new SessionSettings();
        }

        blocks.sanitize();
        keybinds.ensureDefaults();
        zones.sanitize();

        if (recentPresets == null) {
            recentPresets = new ArrayList<>();
        }
    }

    public enum TargetMode {
        SINGLE,
        LIST,
        ALL_MATCHING
    }

    public enum AreaMode {
        START_RADIUS,
        PLAYER_RADIUS,
        CUSTOM_AREA
    }

    public enum CompletionMode {
        UNTIL_STOPPED,
        BLOCK_LIMIT,
        TIME_LIMIT
    }

    public enum TargetSortMode {
        DISTANCE,
        VISIBILITY,
        PRIORITY,
        MINING_SPEED,
        SMART
    }

    public enum HudStyle {
        PANEL,
        MINIMAL,
        COMPACT
    }

    public static final class MiningSettings {
        public TargetMode targetMode = TargetMode.LIST;
        public AreaMode areaMode = AreaMode.START_RADIUS;
        public CompletionMode completionMode = CompletionMode.UNTIL_STOPPED;
        public TargetSortMode sortMode = TargetSortMode.SMART;
        public int searchRadius = 10;
        public double maxTargetDistance = 20.0D;
        public int reactionDelayTicks = 2;
        public int actionDelayTicks = 2;
        public int refreshIntervalTicks = 20;
        public boolean requireVisibility = false;
        public boolean considerHeight = true;
        public boolean mineAbovePlayer = true;
        public boolean mineBelowPlayer = false;
        public boolean avoidSupportBlock = true;
        public boolean preferHorizontalTargets = true;
        public boolean allowBreakIntermediates = false;
        public int maxIntermediateBlocks = 1;
        public double startRadius = 12.0D;
        public double playerRadius = 12.0D;
        public int maxBlocksToMine = 256;
        public int maxRuntimeSeconds = 0;
        public boolean quickSelectReplacesTargets = true;
    }

    public static final class RoutingSettings {
        public int maxVisitedNodes = 384;
        public int maxPathLength = 64;
        public int maxDrop = 2;
        public int maxStepHeight = 1;
        public boolean avoidLava = true;
        public boolean avoidVoid = true;
        public boolean avoidWater = false;
        public boolean avoidDangerBlocks = true;
        public boolean replanWhenStuck = true;
        public int stuckTicksBeforeReplan = 25;
        public double stuckDistanceThreshold = 0.04D;
        public boolean keepNearStart = true;
        public double waypointTolerance = 0.65D;
    }

    public static final class SafetySettings {
        public boolean stopOnLowDurability = true;
        public int minToolDurability = 15;
        public boolean stopOnFullInventory = true;
        public int inventoryWarningThreshold = 32;
        public boolean ignoreJunkBlocks = false;
        public boolean stopOnLowHealth = true;
        public float minHealth = 8.0F;
        public boolean stopOnLowHunger = false;
        public int minHunger = 6;
        public boolean pauseOnOpenScreen = true;
        public boolean pauseOnDamage = true;
        public boolean pauseOnLostTarget = true;
        public boolean emergencyStopOnHazard = true;
        public boolean preventForbiddenBlockBreak = true;
    }

    public static final class HudSettings {
        public boolean enabled = true;
        public int x = 12;
        public int y = 12;
        public float scale = 1.0F;
        public float alpha = 0.85F;
        public int color = 0x5AD1B8;
        public HudStyle style = HudStyle.PANEL;
        public boolean showEnabled = true;
        public boolean showMode = true;
        public boolean showTarget = true;
        public boolean showFoundCount = true;
        public boolean showMinedCount = true;
        public boolean showDistance = true;
        public boolean showState = true;
        public boolean showRestrictions = true;
        public boolean showPreset = true;
        public boolean showWarnings = true;
    }

    public static final class VisualSettings {
        public boolean showCurrentTarget = true;
        public boolean showCandidateHighlights = true;
        public boolean showTracer = true;
        public boolean showWorkArea = true;
        public boolean showIgnoredZones = false;
    }

    public static final class BlockSettings {
        public List<String> targets = new ArrayList<>();
        public List<String> ignored = new ArrayList<>();
        public List<String> priority = new ArrayList<>();
        public List<String> groups = new ArrayList<>();
        public List<String> recent = new ArrayList<>();

        public void sanitize() {
            if (targets == null) {
                targets = new ArrayList<>();
            }
            if (ignored == null) {
                ignored = new ArrayList<>();
            }
            if (priority == null) {
                priority = new ArrayList<>();
            }
            if (groups == null) {
                groups = new ArrayList<>();
            }
            if (recent == null) {
                recent = new ArrayList<>();
            }
        }
    }

    public static final class KeybindSettings {
        public Map<String, String> bindings = new LinkedHashMap<>();

        public void ensureDefaults() {
            if (bindings == null) {
                bindings = new LinkedHashMap<>();
            }
            for (ModKeyAction action : ModKeyAction.values()) {
                bindings.putIfAbsent(action.id(), action.defaultTranslationKey());
            }
        }
    }

    public static final class UiSettings {
        public boolean compactMode = false;
        public String lastTab = "dashboard";
        public String blockSearch = "";
    }

    public static final class ZoneSettings {
        public ZoneArea customArea = ZoneArea.defaultArea();
        public List<ZoneArea> blacklist = new ArrayList<>();
        public List<ZoneArea> whitelist = new ArrayList<>();

        public void sanitize() {
            if (customArea == null) {
                customArea = ZoneArea.defaultArea();
            }
            if (blacklist == null) {
                blacklist = new ArrayList<>();
            }
            if (whitelist == null) {
                whitelist = new ArrayList<>();
            }
        }
    }

    public static final class SessionSettings {
        public String lastLoadedPreset = "rudokop";
        public long lastExportTimestamp = 0L;
    }

    public static final class ZoneArea {
        public String name = "Area";
        public int minX = -8;
        public int minY = -64;
        public int minZ = -8;
        public int maxX = 8;
        public int maxY = 320;
        public int maxZ = 8;
        public boolean enabled = true;

        public static ZoneArea defaultArea() {
            return new ZoneArea();
        }

        public boolean contains(BlockPos pos) {
            return enabled
                && pos.getX() >= Math.min(minX, maxX)
                && pos.getX() <= Math.max(minX, maxX)
                && pos.getY() >= Math.min(minY, maxY)
                && pos.getY() <= Math.max(minY, maxY)
                && pos.getZ() >= Math.min(minZ, maxZ)
                && pos.getZ() <= Math.max(minZ, maxZ);
        }
    }
}

