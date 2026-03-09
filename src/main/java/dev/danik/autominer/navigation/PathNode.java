package dev.danik.autominer.navigation;

import net.minecraft.util.math.BlockPos;

record PathNode(BlockPos pos, double gScore, double fScore) {
}
