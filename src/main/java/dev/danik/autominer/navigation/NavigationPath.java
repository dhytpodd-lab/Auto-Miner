package dev.danik.autominer.navigation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public final class NavigationPath {
    private final List<BlockPos> waypoints;
    private int index;

    public NavigationPath(List<BlockPos> waypoints) {
        this.waypoints = new ArrayList<>(waypoints);
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public boolean isComplete() {
        return index >= waypoints.size();
    }

    public BlockPos current() {
        return isComplete() ? null : waypoints.get(index);
    }

    public void advance() {
        if (!isComplete()) {
            index++;
        }
    }

    public List<BlockPos> waypoints() {
        return waypoints;
    }

    public int remainingWaypoints() {
        return Math.max(0, waypoints.size() - index);
    }
}
