package dev.danik.autominer.safety;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.gui.AutoMinerScreen;
import dev.danik.autominer.mining.InventoryMonitor;
import dev.danik.autominer.mining.ToolSelector;
import dev.danik.autominer.targeting.TargetCandidate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public final class SafetyMonitor {
    private final InventoryMonitor inventoryMonitor = new InventoryMonitor();
    private final ToolSelector toolSelector = new ToolSelector();
    private float lastHealth = -1.0F;

    public SafetyCheckResult evaluate(MinecraftClient client, AutoMinerConfig config, TargetCandidate currentTarget) {
        if (client.player == null || client.world == null) {
            return new SafetyCheckResult(false, false, "", List.of());
        }

        ClientPlayerEntity player = client.player;
        List<String> warnings = new ArrayList<>();

        if (config.safety.stopOnLowHealth && player.getHealth() <= config.safety.minHealth) {
            return new SafetyCheckResult(false, true, "Low health threshold reached", warnings);
        }
        if (config.safety.stopOnLowHunger && player.getHungerManager().getFoodLevel() <= config.safety.minHunger) {
            return new SafetyCheckResult(false, true, "Low hunger threshold reached", warnings);
        }
        if (config.safety.stopOnFullInventory && inventoryMonitor.isFull(player)) {
            return new SafetyCheckResult(false, true, "Inventory is full", warnings);
        }
        if (inventoryMonitor.isNearFull(player, config.safety.inventoryWarningThreshold)) {
            warnings.add("Inventory almost full");
        }
        if (config.safety.pauseOnOpenScreen && client.currentScreen != null && !(client.currentScreen instanceof AutoMinerScreen)) {
            return new SafetyCheckResult(true, false, "Paused while a screen is open", warnings);
        }
        if (config.safety.pauseOnDamage && lastHealth >= 0.0F && player.getHealth() < lastHealth) {
            warnings.add("Damage taken");
            lastHealth = player.getHealth();
            return new SafetyCheckResult(true, false, "Paused after taking damage", warnings);
        }
        if (config.safety.emergencyStopOnHazard && isHazardNearby(client, player.getBlockPos())) {
            return new SafetyCheckResult(false, true, "Hazard detected near player", warnings);
        }
        if (currentTarget != null) {
            ItemStack currentStack = player.getMainHandStack();
            if (config.safety.stopOnLowDurability && currentStack.isDamageable() && toolSelector.remainingDurability(currentStack) <= config.safety.minToolDurability) {
                return new SafetyCheckResult(false, true, "Current tool durability is too low", warnings);
            }
        }

        lastHealth = player.getHealth();
        return new SafetyCheckResult(false, false, "", warnings);
    }

    private boolean isHazardNearby(MinecraftClient client, BlockPos center) {
        for (BlockPos pos : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
            if (client.world == null) {
                continue;
            }
            if (client.world.getBlockState(pos).getBlock().getTranslationKey().contains("lava")
                || client.world.getBlockState(pos).getBlock().getTranslationKey().contains("fire")
                || client.world.getBlockState(pos).getBlock().getTranslationKey().contains("cactus")) {
                return true;
            }
        }
        return false;
    }
}
