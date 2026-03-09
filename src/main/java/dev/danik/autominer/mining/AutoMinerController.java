package dev.danik.autominer.mining;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.navigation.NavigationPath;
import dev.danik.autominer.navigation.NavigationService;
import dev.danik.autominer.preset.PresetManager;
import dev.danik.autominer.safety.SafetyCheckResult;
import dev.danik.autominer.safety.SafetyMonitor;
import dev.danik.autominer.targeting.TargetCandidate;
import dev.danik.autominer.targeting.TargetSearchService;
import dev.danik.autominer.util.MathUtil;
import dev.danik.autominer.util.RegistryUtil;
import dev.danik.autominer.util.RotationUtil;
import dev.danik.autominer.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

public final class AutoMinerController {
    private final ConfigManager configManager;
    private final PresetManager presetManager;
    private final TargetSearchService targetSearchService = new TargetSearchService();
    private final NavigationService navigationService = new NavigationService();
    private final SafetyMonitor safetyMonitor = new SafetyMonitor();
    private final MiningExecutor miningExecutor = new MiningExecutor();
    private final SessionStats sessionStats = new SessionStats();

    private final List<TargetCandidate> visibleCandidates = new ArrayList<>();
    private AutoMinerState state = AutoMinerState.IDLE;
    private NavigationPath currentPath;
    private BlockPos sessionStartPos;
    private boolean runtimePaused;
    private String runtimePauseReason = "";
    private int actionCooldown;
    private int reactionCooldown;

    public AutoMinerController(ConfigManager configManager, PresetManager presetManager) {
        this.configManager = configManager;
        this.presetManager = presetManager;
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> onBlockBroken(pos, state));
    }

    public void tick(MinecraftClient client) {
        AutoMinerConfig config = configManager.config();
        if (client.player == null || client.world == null) {
            setState(AutoMinerState.IDLE, "Waiting for world");
            return;
        }

        sessionStats.setRestrictionSummary(buildRestrictionSummary(config));

        if (!config.enabled) {
            if (state != AutoMinerState.IDLE && state != AutoMinerState.ERROR && state != AutoMinerState.FINISHED) {
                stopLocomotion(client);
                setState(AutoMinerState.IDLE, "Disabled");
            }
            return;
        }

        if (sessionStartPos == null) {
            beginSession(client);
        }

        SafetyCheckResult safety = safetyMonitor.evaluate(client, config, sessionStats.currentTarget());
        sessionStats.setWarnings(safety.warnings());
        if (safety.shouldStop()) {
            stopWithState(client, AutoMinerState.ERROR, safety.reason());
            return;
        }
        if (safety.shouldPause()) {
            runtimePaused = true;
            runtimePauseReason = safety.reason();
        }
        if (config.paused || runtimePaused) {
            stopLocomotion(client);
            setState(AutoMinerState.PAUSED, runtimePaused ? runtimePauseReason : "Paused by user");
            return;
        }
        if (state == AutoMinerState.PAUSED) {
            setState(AutoMinerState.SEARCHING, "Resumed");
        }
        if (completionReached(config)) {
            stopWithState(client, AutoMinerState.FINISHED, "Completion condition reached");
            return;
        }

        reactionCooldown = Math.max(0, reactionCooldown - 1);
        actionCooldown = Math.max(0, actionCooldown - 1);

        switch (state) {
            case IDLE, ERROR, FINISHED -> setState(AutoMinerState.SEARCHING, "Searching for target");
            case SEARCHING -> searchForTarget(client);
            case MOVING -> moveToTarget(client);
            case AIMING -> aimAtTarget(client);
            case MINING -> mineTarget(client);
            case PAUSED -> setState(AutoMinerState.SEARCHING, "Resumed");
        }
    }

    public void toggle(MinecraftClient client) {
        if (configManager.config().enabled) {
            stopWithState(client, AutoMinerState.IDLE, "Disabled by user");
        } else {
            configManager.config().enabled = true;
            configManager.config().paused = false;
            runtimePaused = false;
            runtimePauseReason = "";
            beginSession(client);
            configManager.save();
        }
    }

    public void togglePause(MinecraftClient client) {
        AutoMinerConfig config = configManager.config();
        if (!config.enabled) {
            return;
        }
        if (runtimePaused || config.paused) {
            runtimePaused = false;
            runtimePauseReason = "";
            config.paused = false;
            setState(AutoMinerState.SEARCHING, "Resumed");
        } else {
            config.paused = true;
            setState(AutoMinerState.PAUSED, "Paused by user");
            stopLocomotion(client);
        }
        configManager.save();
    }

    public void emergencyStop(String reason) {
        MinecraftClient client = MinecraftClient.getInstance();
        stopWithState(client, AutoMinerState.ERROR, reason);
    }

    public void replaceTargetsWith(String blockId) {
        AutoMinerConfig config = configManager.config();
        if (config.mining.quickSelectReplacesTargets) {
            config.blocks.targets.clear();
        }
        if (!config.blocks.targets.contains(blockId)) {
            config.blocks.targets.add(blockId);
        }
        rememberRecentBlock(config, blockId);
        config.mining.targetMode = AutoMinerConfig.TargetMode.SINGLE;
        targetSearchService.invalidate();
        configManager.save();
    }

    public void addTargetBlock(String blockId) {
        AutoMinerConfig config = configManager.config();
        if (!config.blocks.targets.contains(blockId)) {
            config.blocks.targets.add(blockId);
        }
        rememberRecentBlock(config, blockId);
        if (config.blocks.targets.size() > 1) {
            config.mining.targetMode = AutoMinerConfig.TargetMode.LIST;
        }
        targetSearchService.invalidate();
        configManager.save();
    }

    public void removeTargetBlock(String blockId) {
        AutoMinerConfig config = configManager.config();
        config.blocks.targets.remove(blockId);
        targetSearchService.invalidate();
        configManager.save();
    }

    public AutoMinerState state() {
        return state;
    }

    public SessionStats stats() {
        return sessionStats;
    }

    public List<TargetCandidate> visibleCandidates() {
        return visibleCandidates;
    }

    public NavigationPath currentPath() {
        return currentPath;
    }

    public boolean isEnabled() {
        return configManager.config().enabled;
    }

    public boolean isPaused() {
        return configManager.config().paused || runtimePaused;
    }

    public String activePreset() {
        return configManager.config().activePreset;
    }

    public double distanceToTarget(MinecraftClient client) {
        TargetCandidate target = sessionStats.currentTarget();
        if (client.player == null || target == null) {
            return 0.0D;
        }
        return client.player.getEyePos().distanceTo(target.center());
    }

    public PresetManager presetManager() {
        return presetManager;
    }

    private void beginSession(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        sessionStartPos = client.player.getBlockPos();
        sessionStats.begin(sessionStartPos);
        visibleCandidates.clear();
        currentPath = null;
        actionCooldown = 0;
        reactionCooldown = 0;
        configManager.config().enabled = true;
        setState(AutoMinerState.SEARCHING, "Searching for target");
        targetSearchService.invalidate();
    }

    private void searchForTarget(MinecraftClient client) {
        AutoMinerConfig config = configManager.config();
        long currentTick = client.world == null ? 0L : client.world.getTime();
        visibleCandidates.clear();
        visibleCandidates.addAll(targetSearchService.findTargets(client, config, sessionStartPos, currentTick, reactionCooldown == 0));
        sessionStats.setFoundBlocks(visibleCandidates.size());

        if (visibleCandidates.isEmpty()) {
            sessionStats.setCurrentTarget(null);
            currentPath = null;
            setState(AutoMinerState.SEARCHING, "No matching targets found");
            return;
        }

        int attempts = Math.min(10, visibleCandidates.size());
        for (int i = 0; i < attempts; i++) {
            TargetCandidate candidate = visibleCandidates.get(i);
            if (!isTargetStillValid(client, candidate)) {
                targetSearchService.markRejected(candidate.pos(), currentTick);
                continue;
            }
            if (canMineFromHere(client, candidate)) {
                sessionStats.setCurrentTarget(candidate);
                currentPath = null;
                setState(AutoMinerState.AIMING, "Target locked: " + candidate.blockId());
                reactionCooldown = config.mining.reactionDelayTicks;
                return;
            }
            NavigationPath path = navigationService.planPath(client, candidate.pos(), config);
            if (path != null && !path.isEmpty()) {
                sessionStats.setCurrentTarget(candidate);
                currentPath = path;
                setState(AutoMinerState.MOVING, "Moving to " + candidate.blockId());
                reactionCooldown = config.mining.reactionDelayTicks;
                return;
            }
            targetSearchService.markRejected(candidate.pos(), currentTick);
        }

        sessionStats.setCurrentTarget(null);
        setState(AutoMinerState.SEARCHING, "No reachable targets available");
    }

    private void moveToTarget(MinecraftClient client) {
        TargetCandidate target = sessionStats.currentTarget();
        if (target == null || !isTargetStillValid(client, target)) {
            handleTargetLost(client, "Target lost while moving");
            return;
        }
        if (canMineFromHere(client, target)) {
            navigationService.stopMovement(client);
            setState(AutoMinerState.AIMING, "Reached mining range");
            return;
        }
        if (currentPath == null) {
            setState(AutoMinerState.SEARCHING, "Rebuilding route");
            return;
        }

        NavigationService.NavigationResult result = navigationService.tick(client, currentPath, configManager.config());
        switch (result) {
            case MOVING -> sessionStats.setStatusMessage("Moving to target");
            case COMPLETE -> setState(AutoMinerState.AIMING, "Arrived at target");
            case STUCK, INVALID -> {
                targetSearchService.markRejected(target.pos(), client.world == null ? 0L : client.world.getTime());
                currentPath = null;
                setState(AutoMinerState.SEARCHING, "Route invalidated, searching again");
            }
        }
    }

    private void aimAtTarget(MinecraftClient client) {
        TargetCandidate target = sessionStats.currentTarget();
        if (target == null || !isTargetStillValid(client, target)) {
            handleTargetLost(client, "Target lost while aiming");
            return;
        }
        if (client.player == null) {
            return;
        }
        if (!canMineFromHere(client, target)) {
            setState(AutoMinerState.MOVING, "Stepping back into range");
            currentPath = navigationService.planPath(client, target.pos(), configManager.config());
            return;
        }

        ClientPlayerEntity player = client.player;
        float targetYaw = RotationUtil.targetYaw(player, target.center());
        float targetPitch = RotationUtil.targetPitch(player, target.center());
        player.setYaw(RotationUtil.stepAngle(player.getYaw(), targetYaw, 10.0F));
        player.setPitch(RotationUtil.stepAngle(player.getPitch(), targetPitch, 10.0F));
        double yawDelta = Math.abs(MathUtil.normalizeAngle(targetYaw - player.getYaw()));
        double pitchDelta = Math.abs(targetPitch - player.getPitch());

        if (yawDelta <= 4.0D && pitchDelta <= 4.0D) {
            setState(AutoMinerState.MINING, "Mining started");
        } else {
            sessionStats.setStatusMessage("Aiming at target");
        }
    }

    private void mineTarget(MinecraftClient client) {
        TargetCandidate target = sessionStats.currentTarget();
        if (target == null || !isTargetStillValid(client, target)) {
            handleTargetLost(client, "Target lost while mining");
            return;
        }
        if (actionCooldown > 0) {
            return;
        }
        if (!canMineFromHere(client, target)) {
            setState(AutoMinerState.MOVING, "Target left reach, repositioning");
            currentPath = navigationService.planPath(client, target.pos(), configManager.config());
            return;
        }

        MiningExecutor.MiningResult result = miningExecutor.tick(client, target, configManager.config());
        actionCooldown = configManager.config().mining.actionDelayTicks;
        switch (result) {
            case MINING -> sessionStats.setStatusMessage("Mining " + target.blockId());
            case BROKEN -> {
                sessionStats.setCurrentTarget(null);
                currentPath = null;
                targetSearchService.invalidate();
                setState(AutoMinerState.SEARCHING, "Target mined");
            }
            case OUT_OF_RANGE -> {
                currentPath = navigationService.planPath(client, target.pos(), configManager.config());
                setState(AutoMinerState.MOVING, "Target out of range");
            }
            case NO_TOOL -> stopWithState(client, AutoMinerState.ERROR, "No safe tool available for target");
            case BLOCKED -> {
                targetSearchService.markRejected(target.pos(), client.world == null ? 0L : client.world.getTime());
                sessionStats.warnings().add("Target became blocked");
                setState(AutoMinerState.SEARCHING, "Searching for a new target");
            }
        }
    }

    private boolean completionReached(AutoMinerConfig config) {
        return switch (config.mining.completionMode) {
            case UNTIL_STOPPED -> false;
            case BLOCK_LIMIT -> sessionStats.minedBlocks() >= config.mining.maxBlocksToMine;
            case TIME_LIMIT -> config.mining.maxRuntimeSeconds > 0 && sessionStats.elapsedSeconds() >= config.mining.maxRuntimeSeconds;
        };
    }

    private boolean canMineFromHere(MinecraftClient client, TargetCandidate target) {
        if (client.player == null) {
            return false;
        }
        double distance = client.player.getEyePos().distanceTo(target.center());
        return distance <= 4.7D && (!configManager.config().mining.requireVisibility || target.visible());
    }

    private boolean isTargetStillValid(MinecraftClient client, TargetCandidate candidate) {
        if (client.world == null) {
            return false;
        }
        BlockState state = client.world.getBlockState(candidate.pos());
        if (state.isAir()) {
            return false;
        }
        return Registries.BLOCK.getId(state.getBlock()).toString().equals(candidate.blockId());
    }

    private void handleTargetLost(MinecraftClient client, String reason) {
        sessionStats.setCurrentTarget(null);
        currentPath = null;
        targetSearchService.invalidate();
        if (configManager.config().safety.pauseOnLostTarget) {
            runtimePaused = true;
            runtimePauseReason = reason;
            stopLocomotion(client);
            setState(AutoMinerState.PAUSED, reason);
        } else {
            setState(AutoMinerState.SEARCHING, reason);
        }
    }

    private void stopWithState(MinecraftClient client, AutoMinerState newState, String reason) {
        configManager.config().enabled = false;
        configManager.config().paused = false;
        runtimePaused = false;
        runtimePauseReason = "";
        stopLocomotion(client);
        sessionStats.setStopReason(reason);
        setState(newState, reason);
        configManager.save();
    }

    private void stopLocomotion(MinecraftClient client) {
        navigationService.stopMovement(client);
        miningExecutor.cancel(client);
        currentPath = null;
    }

    private void rememberRecentBlock(AutoMinerConfig config, String blockId) {
        config.blocks.recent.remove(blockId);
        config.blocks.recent.add(0, blockId);
        while (config.blocks.recent.size() > 12) {
            config.blocks.recent.remove(config.blocks.recent.size() - 1);
        }
    }

    private void onBlockBroken(BlockPos pos, BlockState state) {
        String blockId = RegistryUtil.blockIdString(state.getBlock());
        sessionStats.recordMined(blockId);
        if (sessionStats.currentTarget() != null && sessionStats.currentTarget().pos().equals(pos)) {
            sessionStats.setCurrentTarget(null);
            currentPath = null;
            targetSearchService.invalidate();
            if (configManager.config().enabled && !isPaused()) {
                setState(AutoMinerState.SEARCHING, "Target mined");
            }
        }
    }

    private void setState(AutoMinerState newState, String message) {
        this.state = newState;
        sessionStats.setStatusMessage(message);
    }

    private String buildRestrictionSummary(AutoMinerConfig config) {
        List<String> restrictions = new ArrayList<>();
        restrictions.add(config.mining.areaMode.name());
        restrictions.add(config.mining.completionMode.name());
        if (config.safety.stopOnFullInventory) {
            restrictions.add("Full inventory stop");
        }
        if (config.safety.stopOnLowDurability) {
            restrictions.add("Durability guard");
        }
        if (config.routing.keepNearStart) {
            restrictions.add("Stay near start");
        }
        if (config.mining.requireVisibility) {
            restrictions.add("Visible targets only");
        }
        if (config.mining.avoidSupportBlock) {
            restrictions.add("Keep support block");
        }
        if (config.mining.preferHorizontalTargets) {
            restrictions.add("Prefer horizontal");
        }
        return TextUtil.joinLimited(restrictions, 4);
    }
}

