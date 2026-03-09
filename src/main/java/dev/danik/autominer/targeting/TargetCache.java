package dev.danik.autominer.targeting;

import java.util.ArrayList;
import java.util.List;

public final class TargetCache {
    private long lastRefreshTick = Long.MIN_VALUE;
    private List<TargetCandidate> cachedTargets = new ArrayList<>();

    public boolean shouldRefresh(long currentTick, int refreshIntervalTicks) {
        return currentTick - lastRefreshTick >= refreshIntervalTicks;
    }

    public void update(long currentTick, List<TargetCandidate> targets) {
        this.lastRefreshTick = currentTick;
        this.cachedTargets = new ArrayList<>(targets);
    }

    public List<TargetCandidate> cachedTargets() {
        return cachedTargets;
    }

    public void invalidate() {
        lastRefreshTick = Long.MIN_VALUE;
        cachedTargets = new ArrayList<>();
    }
}
