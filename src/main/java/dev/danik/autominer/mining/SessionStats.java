package dev.danik.autominer.mining;

import dev.danik.autominer.targeting.TargetCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;

public final class SessionStats {
    private BlockPos startPos;
    private long startedAtMs;
    private int minedBlocks;
    private int foundBlocks;
    private final Map<String, Integer> minedByBlock = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private TargetCandidate currentTarget;
    private String statusMessage = "Idle";
    private String stopReason = "";
    private String restrictionSummary = "None";

    public void begin(BlockPos startPos) {
        this.startPos = startPos;
        this.startedAtMs = System.currentTimeMillis();
        this.minedBlocks = 0;
        this.foundBlocks = 0;
        this.minedByBlock.clear();
        this.warnings.clear();
        this.currentTarget = null;
        this.statusMessage = "Started";
        this.stopReason = "";
    }

    public BlockPos startPos() {
        return startPos;
    }

    public long startedAtMs() {
        return startedAtMs;
    }

    public int minedBlocks() {
        return minedBlocks;
    }

    public int foundBlocks() {
        return foundBlocks;
    }

    public void setFoundBlocks(int foundBlocks) {
        this.foundBlocks = foundBlocks;
    }

    public void recordMined(String blockId) {
        minedBlocks++;
        minedByBlock.merge(blockId, 1, Integer::sum);
    }

    public Map<String, Integer> minedByBlock() {
        return minedByBlock;
    }

    public List<String> warnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings.clear();
        this.warnings.addAll(warnings);
    }

    public TargetCandidate currentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(TargetCandidate currentTarget) {
        this.currentTarget = currentTarget;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String stopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public String restrictionSummary() {
        return restrictionSummary;
    }

    public void setRestrictionSummary(String restrictionSummary) {
        this.restrictionSummary = restrictionSummary;
    }

    public long elapsedSeconds() {
        return startedAtMs == 0L ? 0L : Math.max(0L, (System.currentTimeMillis() - startedAtMs) / 1000L);
    }
}
