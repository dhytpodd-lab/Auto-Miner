package dev.danik.autominer.targeting;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.mining.ToolSelector;
import dev.danik.autominer.util.RegistryUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class TargetSearchService {
    private final ToolSelector toolSelector = new ToolSelector();
    private final TargetCache cache = new TargetCache();
    private final Map<BlockPos, Long> rejectedUntilTick = new HashMap<>();

    public List<TargetCandidate> findTargets(MinecraftClient client, AutoMinerConfig config, BlockPos startAnchor, long currentTick, boolean forceRefresh) {
        if (!forceRefresh && !cache.shouldRefresh(currentTick, config.mining.refreshIntervalTicks)) {
            return cache.cachedTargets();
        }
        if (client.player == null || client.world == null) {
            return List.of();
        }

        ClientPlayerEntity player = client.player;
        BlockPos origin = switch (config.mining.areaMode) {
            case START_RADIUS -> startAnchor == null ? player.getBlockPos() : startAnchor;
            case PLAYER_RADIUS, CUSTOM_AREA -> player.getBlockPos();
        };
        int radius = config.mining.searchRadius;
        List<TargetCandidate> results = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -radius, -radius), origin.add(radius, radius, radius))) {
            if (isRejected(pos.toImmutable(), currentTick)) {
                continue;
            }
            if (!isInsideWorkArea(config, startAnchor, player.getBlockPos(), pos.toImmutable())) {
                continue;
            }
            BlockState state = client.world.getBlockState(pos);
            if (!isValidTarget(state, config, player, pos.toImmutable())) {
                continue;
            }

            double distance = player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
            if (distance > config.mining.maxTargetDistance) {
                continue;
            }
            if (!config.mining.mineAbovePlayer && pos.getY() > player.getBlockY() + 1) {
                continue;
            }
            if (!config.mining.mineBelowPlayer && pos.getY() < player.getBlockY() - 1) {
                continue;
            }
            if (config.mining.avoidSupportBlock && isSupportBlock(player, pos.toImmutable())) {
                continue;
            }

            boolean visible = isVisible(client, player, pos.toImmutable());
            if (config.mining.requireVisibility && !visible) {
                continue;
            }

            String blockId = RegistryUtil.blockIdString(state.getBlock());
            double speed = toolSelector.bestMiningSpeed(player, state, config);
            int priority = priorityWeight(config, blockId);
            int verticalDelta = Math.abs(pos.getY() - player.getBlockY());
            boolean belowPlayer = pos.getY() < player.getBlockY();
            double score = score(config, distance, visible, speed, priority, verticalDelta, belowPlayer);
            results.add(new TargetCandidate(pos.toImmutable(), blockId, score, distance, visible, speed, priority));
        }

        results.sort(Comparator.comparingDouble(TargetCandidate::score));
        if (results.size() > 96) {
            results = new ArrayList<>(results.subList(0, 96));
        }
        cache.update(currentTick, results);
        return results;
    }

    public void markRejected(BlockPos pos, long currentTick) {
        rejectedUntilTick.put(pos.toImmutable(), currentTick + 80L);
    }

    public void invalidate() {
        cache.invalidate();
    }

    private boolean isRejected(BlockPos pos, long currentTick) {
        return rejectedUntilTick.getOrDefault(pos, Long.MIN_VALUE) > currentTick;
    }

    private boolean isInsideWorkArea(AutoMinerConfig config, BlockPos startAnchor, BlockPos playerPos, BlockPos pos) {
        if (config.mining.areaMode == AutoMinerConfig.AreaMode.START_RADIUS && startAnchor != null) {
            return pos.getSquaredDistance(startAnchor) <= config.mining.startRadius * config.mining.startRadius;
        }
        if (config.mining.areaMode == AutoMinerConfig.AreaMode.PLAYER_RADIUS) {
            return pos.getSquaredDistance(playerPos) <= config.mining.playerRadius * config.mining.playerRadius;
        }
        if (config.mining.areaMode == AutoMinerConfig.AreaMode.CUSTOM_AREA) {
            return config.zones.customArea.contains(pos);
        }
        return true;
    }

    private boolean isValidTarget(BlockState state, AutoMinerConfig config, ClientPlayerEntity player, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        if (state.getHardness(player.getWorld(), pos) < 0.0F) {
            return false;
        }

        String blockId = RegistryUtil.blockIdString(state.getBlock());
        if (config.blocks.ignored.contains(blockId)) {
            return false;
        }
        if (!config.zones.blacklist.isEmpty() && config.zones.blacklist.stream().anyMatch(zone -> zone.contains(pos))) {
            return false;
        }
        if (!config.zones.whitelist.isEmpty() && config.zones.whitelist.stream().noneMatch(zone -> zone.contains(pos))) {
            return false;
        }

        boolean explicitMatch = config.blocks.targets.contains(blockId) || RegistryUtil.matchesAnyGroup(state, config.blocks.groups);
        return switch (config.mining.targetMode) {
            case SINGLE -> !config.blocks.targets.isEmpty() && blockId.equals(config.blocks.targets.getFirst());
            case LIST -> explicitMatch;
            case ALL_MATCHING -> explicitMatch || (config.blocks.targets.isEmpty() && config.blocks.groups.isEmpty()) || blockId.toLowerCase(Locale.ROOT).contains("ore");
        };
    }

    private boolean isVisible(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        Vec3d from = player.getEyePos();
        Vec3d to = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = client.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        return hitResult.getType() == Type.MISS || hitResult.getBlockPos().equals(pos);
    }

    private int priorityWeight(AutoMinerConfig config, String blockId) {
        int index = config.blocks.priority.indexOf(blockId);
        if (index >= 0) {
            return config.blocks.priority.size() - index;
        }
        return 0;
    }

    private double score(AutoMinerConfig config, double distance, boolean visible, double speed, int priorityWeight, int verticalDelta, boolean belowPlayer) {
        double visibilityPenalty = visible ? 0.0D : 3.5D;
        double heightPenalty = config.mining.considerHeight ? verticalDelta * 0.35D : 0.0D;
        double horizontalPenalty = config.mining.preferHorizontalTargets
            ? verticalDelta * 1.1D + (belowPlayer ? 1.75D : 0.0D)
            : 0.0D;
        return switch (config.mining.sortMode) {
            case DISTANCE -> distance + horizontalPenalty;
            case VISIBILITY -> distance + visibilityPenalty * 3.0D + horizontalPenalty;
            case PRIORITY -> distance - priorityWeight * 2.5D + visibilityPenalty + horizontalPenalty;
            case MINING_SPEED -> distance - speed * 0.45D + visibilityPenalty + horizontalPenalty;
            case SMART -> distance + heightPenalty + visibilityPenalty + horizontalPenalty - speed * 0.3D - priorityWeight * 1.75D;
        };
    }

    private boolean isSupportBlock(ClientPlayerEntity player, BlockPos pos) {
        Box box = player.getBoundingBox();
        int supportY = (int) Math.floor(box.minY - 0.05D);
        if (pos.getY() != supportY) {
            return false;
        }

        BlockPos northWest = BlockPos.ofFloored(box.minX + 0.001D, supportY, box.minZ + 0.001D);
        BlockPos northEast = BlockPos.ofFloored(box.maxX - 0.001D, supportY, box.minZ + 0.001D);
        BlockPos southWest = BlockPos.ofFloored(box.minX + 0.001D, supportY, box.maxZ - 0.001D);
        BlockPos southEast = BlockPos.ofFloored(box.maxX - 0.001D, supportY, box.maxZ - 0.001D);
        return pos.equals(northWest) || pos.equals(northEast) || pos.equals(southWest) || pos.equals(southEast);
    }
}




