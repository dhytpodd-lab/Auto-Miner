package dev.danik.autominer.util;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public final class BlockSelectionHelper {
    private BlockSelectionHelper() {
    }

    public static Identifier getCrosshairBlockId(MinecraftClient client) {
        if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && client.world != null) {
            Block block = client.world.getBlockState(hit.getBlockPos()).getBlock();
            return Registries.BLOCK.getId(block);
        }
        return null;
    }
}
