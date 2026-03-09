package dev.danik.autominer.targeting;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record TargetCandidate(BlockPos pos, String blockId, double score, double distance, boolean visible, double miningSpeed, int priorityWeight) {
    public Vec3d center() {
        return Vec3d.ofCenter(pos);
    }
}
