package dev.danik.autominer.mining;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class InventoryMonitor {
    public boolean isFull(ClientPlayerEntity player) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isNearFull(ClientPlayerEntity player, int threshold) {
        int filled = 0;
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (!stack.isEmpty()) {
                filled++;
            }
        }
        return filled >= threshold;
    }
}
