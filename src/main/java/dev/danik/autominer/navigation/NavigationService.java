package dev.danik.autominer.navigation;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.util.MathUtil;
import dev.danik.autominer.util.RotationUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class NavigationService {
    private Vec3d lastPlayerPos;
    private int stuckTicks;

    public NavigationPath planPath(MinecraftClient client, BlockPos targetPos, AutoMinerConfig config) {
        if (client.player == null || client.world == null) {
            return null;
        }

        BlockPos start = client.player.getBlockPos();
        List<BlockPos> miningSpots = collectMiningSpots(client.world, targetPos, config);
        NavigationPath best = null;
        for (BlockPos candidate : miningSpots) {
            NavigationPath path = findPath(client.world, start, candidate, config);
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (best == null || path.waypoints().size() < best.waypoints().size()) {
                best = path;
            }
        }
        return best;
    }

    public NavigationResult tick(MinecraftClient client, NavigationPath path, AutoMinerConfig config) {
        if (client.player == null || path == null || path.isEmpty()) {
            stopMovement(client);
            return NavigationResult.INVALID;
        }

        ClientPlayerEntity player = client.player;
        if (path.isComplete()) {
            stopMovement(client);
            return NavigationResult.COMPLETE;
        }

        BlockPos currentWaypoint = path.current();
        Vec3d waypointCenter = Vec3d.ofCenter(currentWaypoint);
        double distance = player.getPos().distanceTo(waypointCenter);
        if (distance <= config.routing.waypointTolerance) {
            path.advance();
            if (path.isComplete()) {
                stopMovement(client);
                return NavigationResult.COMPLETE;
            }
            currentWaypoint = path.current();
            waypointCenter = Vec3d.ofCenter(currentWaypoint);
        }

        float targetYaw = RotationUtil.targetYaw(player, waypointCenter);
        player.setYaw(RotationUtil.stepAngle(player.getYaw(), targetYaw, 12.0F));
        double yawDelta = MathUtil.normalizeAngle(targetYaw - player.getYaw());

        client.options.forwardKey.setPressed(Math.abs(yawDelta) < 70.0D);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(yawDelta < -10.0D);
        client.options.rightKey.setPressed(yawDelta > 10.0D);
        client.options.sneakKey.setPressed(false);
        client.options.jumpKey.setPressed(waypointCenter.y > player.getY() + 0.55D);

        updateStuckState(player, config);
        if (stuckTicks >= config.routing.stuckTicksBeforeReplan) {
            stopMovement(client);
            return NavigationResult.STUCK;
        }
        return NavigationResult.MOVING;
    }

    public void stopMovement(MinecraftClient client) {
        if (client == null) {
            return;
        }
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        lastPlayerPos = null;
        stuckTicks = 0;
    }

    private void updateStuckState(ClientPlayerEntity player, AutoMinerConfig config) {
        if (lastPlayerPos == null) {
            lastPlayerPos = player.getPos();
            stuckTicks = 0;
            return;
        }

        if (player.getPos().distanceTo(lastPlayerPos) <= config.routing.stuckDistanceThreshold) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPlayerPos = player.getPos();
    }

    private NavigationPath findPath(World world, BlockPos start, BlockPos goal, AutoMinerConfig config) {
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PathNode::fScore));
        Map<BlockPos, Double> gScores = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        open.add(new PathNode(start, 0.0D, heuristic(start, goal)));
        gScores.put(start, 0.0D);
        int visited = 0;

        while (!open.isEmpty() && visited < config.routing.maxVisitedNodes) {
            visited++;
            PathNode current = open.poll();
            if (current.pos().equals(goal)) {
                return reconstructPath(cameFrom, current.pos());
            }
            if (!closed.add(current.pos())) {
                continue;
            }

            for (BlockPos neighbor : neighbors(world, current.pos(), config)) {
                if (closed.contains(neighbor)) {
                    continue;
                }
                double tentative = gScores.getOrDefault(current.pos(), Double.POSITIVE_INFINITY) + current.pos().getSquaredDistance(neighbor);
                if (tentative >= gScores.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }
                if (tentative > config.routing.maxPathLength * config.routing.maxPathLength) {
                    continue;
                }
                cameFrom.put(neighbor, current.pos());
                gScores.put(neighbor, tentative);
                open.add(new PathNode(neighbor, tentative, tentative + heuristic(neighbor, goal)));
            }
        }
        return null;
    }

    private NavigationPath reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos end) {
        List<BlockPos> waypoints = new ArrayList<>();
        BlockPos cursor = end;
        while (cursor != null) {
            waypoints.add(0, cursor);
            cursor = cameFrom.get(cursor);
        }
        return new NavigationPath(waypoints);
    }

    private List<BlockPos> collectMiningSpots(World world, BlockPos targetPos, AutoMinerConfig config) {
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -2, -2), targetPos.add(2, 2, 2))) {
            if (!canStandAt(world, pos.toImmutable(), config)) {
                continue;
            }
            if (Vec3d.ofCenter(pos).distanceTo(Vec3d.ofCenter(targetPos)) <= 4.6D) {
                spots.add(pos.toImmutable());
            }
        }
        spots.sort(Comparator.comparingDouble(candidate -> candidate.getSquaredDistance(targetPos)));
        if (spots.size() > 16) {
            return new ArrayList<>(spots.subList(0, 16));
        }
        return spots;
    }

    private List<BlockPos> neighbors(World world, BlockPos pos, AutoMinerConfig config) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)) {
            BlockPos sameLevel = pos.offset(direction);
            if (canStandAt(world, sameLevel, config)) {
                neighbors.add(sameLevel);
                continue;
            }
            for (int step = 1; step <= config.routing.maxStepHeight; step++) {
                BlockPos elevated = sameLevel.up(step);
                if (canStandAt(world, elevated, config)) {
                    neighbors.add(elevated);
                    break;
                }
            }
            for (int drop = 1; drop <= config.routing.maxDrop; drop++) {
                BlockPos lowered = sameLevel.down(drop);
                if (canStandAt(world, lowered, config)) {
                    neighbors.add(lowered);
                    break;
                }
            }
        }
        return neighbors;
    }

    private boolean canStandAt(World world, BlockPos pos, AutoMinerConfig config) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState below = world.getBlockState(pos.down());

        if (!feet.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        if (!head.getCollisionShape(world, pos.up()).isEmpty()) {
            return false;
        }
        if (below.getCollisionShape(world, pos.down()).isEmpty()) {
            return false;
        }
        if (config.routing.avoidLava && (below.getFluidState().isOf(Fluids.LAVA) || feet.getFluidState().isOf(Fluids.LAVA))) {
            return false;
        }
        if (config.routing.avoidWater && (below.getFluidState().isOf(Fluids.WATER) || feet.getFluidState().isOf(Fluids.WATER))) {
            return false;
        }
        if (config.routing.avoidVoid && pos.getY() <= world.getBottomY() + 1) {
            return false;
        }
        return true;
    }

    private double heuristic(BlockPos from, BlockPos to) {
        return from.getSquaredDistance(to);
    }

    public enum NavigationResult {
        MOVING,
        COMPLETE,
        STUCK,
        INVALID
    }
}
