package dev.danik.autominer.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;

public final class RegistryUtil {
    private RegistryUtil() {
    }

    public static Identifier blockId(Block block) {
        return Registries.BLOCK.getId(block);
    }

    public static String blockIdString(Block block) {
        return blockId(block).toString();
    }

    public static List<String> allBlockIds(String search) {
        String normalized = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
        return Registries.BLOCK.stream()
            .map(RegistryUtil::blockIdString)
            .filter(id -> normalized.isEmpty() || id.contains(normalized))
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean matchesAnyGroup(BlockState state, List<String> groups) {
        for (String group : groups) {
            if (matchesGroup(state, group)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesGroup(BlockState state, String group) {
        String normalized = group == null ? "" : group.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ores", "ore" -> state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES);
            case "logs", "wood" -> state.isIn(BlockTags.LOGS);
            case "stone" -> state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.STONE_ORE_REPLACEABLES);
            default -> false;
        };
    }

    public static boolean isValidBlockId(String blockId) {
        Identifier id = Identifier.tryParse(blockId);
        return id != null && Registries.BLOCK.containsId(id);
    }
}
