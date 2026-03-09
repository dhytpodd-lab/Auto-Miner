package dev.danik.autominer.mining;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.targeting.TargetCandidate;
import dev.danik.autominer.util.RotationUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class MiningExecutor {
    private final ToolSelector toolSelector = new ToolSelector();
    private TargetCandidate lastTarget;

    public MiningResult tick(MinecraftClient client, TargetCandidate target, AutoMinerConfig config) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return MiningResult.BLOCKED;
        }

        BlockState state = client.world.getBlockState(target.pos());
        if (state.isAir()) {
            return MiningResult.BROKEN;
        }

        ClientPlayerEntity player = client.player;
        if (player.getEyePos().distanceTo(target.center()) > 4.7D) {
            return MiningResult.OUT_OF_RANGE;
        }

        int bestSlot = toolSelector.selectBestTool(player, state, config);
        player.getInventory().setSelectedSlot(bestSlot);
        ItemStack selected = player.getInventory().getStack(bestSlot);
        if (config.safety.stopOnLowDurability && selected.isDamageable() && toolSelector.remainingDurability(selected) <= config.safety.minToolDurability) {
            return MiningResult.NO_TOOL;
        }
        if (!toolSelector.hasUsableTool(player, state, config) && config.safety.stopOnLowDurability) {
            return MiningResult.NO_TOOL;
        }

        Vec3d center = target.center();
        player.setYaw(RotationUtil.targetYaw(player, center));
        player.setPitch(RotationUtil.targetPitch(player, center));

        Direction side = Direction.getFacing(center.x - player.getX(), center.y - player.getEyeY(), center.z - player.getZ());
        if (lastTarget == null || !lastTarget.pos().equals(target.pos())) {
            client.interactionManager.attackBlock(target.pos(), side);
        }
        boolean progressed = client.interactionManager.updateBlockBreakingProgress(target.pos(), side);
        player.swingHand(Hand.MAIN_HAND);
        lastTarget = target;
        return progressed ? MiningResult.MINING : MiningResult.BLOCKED;
    }

    public void cancel(MinecraftClient client) {
        if (client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        lastTarget = null;
    }

    public enum MiningResult {
        MINING,
        BROKEN,
        OUT_OF_RANGE,
        NO_TOOL,
        BLOCKED
    }
}
