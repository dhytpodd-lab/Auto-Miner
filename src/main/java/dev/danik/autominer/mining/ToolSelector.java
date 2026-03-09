package dev.danik.autominer.mining;

import dev.danik.autominer.config.AutoMinerConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class ToolSelector {
    public int selectBestTool(ClientPlayerEntity player, BlockState state, AutoMinerConfig config) {
        int bestSlot = player.getInventory().getSelectedSlot();
        double bestScore = score(player.getInventory().getStack(bestSlot), state, config);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            double score = score(stack, state, config);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public boolean hasUsableTool(ClientPlayerEntity player, BlockState state, AutoMinerConfig config) {
        return bestMiningSpeed(player, state, config) > 1.0D || !state.isToolRequired();
    }

    public double bestMiningSpeed(ClientPlayerEntity player, BlockState state, AutoMinerConfig config) {
        double best = 0.0D;
        for (int slot = 0; slot < 9; slot++) {
            best = Math.max(best, score(player.getInventory().getStack(slot), state, config));
        }
        return best;
    }

    public int remainingDurability(ItemStack stack) {
        if (!stack.isDamageable()) {
            return Integer.MAX_VALUE;
        }
        return stack.getMaxDamage() - stack.getDamage();
    }

    private double score(ItemStack stack, BlockState state, AutoMinerConfig config) {
        if (stack.isEmpty()) {
            return state.isToolRequired() ? 0.0D : 1.0D;
        }

        int durability = remainingDurability(stack);
        if (config.safety.stopOnLowDurability && durability <= config.safety.minToolDurability) {
            return 0.0D;
        }

        double speed = stack.getMiningSpeedMultiplier(state);
        if (!state.isToolRequired()) {
            speed = Math.max(speed, 1.0D);
        }
        return speed + Math.min(durability, 200) * 0.0025D;
    }
}
