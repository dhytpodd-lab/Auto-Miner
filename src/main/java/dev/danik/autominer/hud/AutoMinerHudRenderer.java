package dev.danik.autominer.hud;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.mining.AutoMinerController;
import dev.danik.autominer.targeting.TargetCandidate;
import dev.danik.autominer.util.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class AutoMinerHudRenderer {
    private final ConfigManager configManager;
    private final AutoMinerController controller;

    public AutoMinerHudRenderer(ConfigManager configManager, AutoMinerController controller) {
        this.configManager = configManager;
        this.controller = controller;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        AutoMinerConfig config = configManager.config();
        if (!config.hud.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        List<Text> lines = buildLines(client, config);
        if (lines.isEmpty()) {
            return;
        }

        int maxWidth = 0;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }

        int x = config.hud.x;
        int y = config.hud.y;
        int color = ColorUtil.withAlpha(config.hud.color, config.hud.alpha);
        int background = ColorUtil.withAlpha(0x101418, Math.min(0.95F, config.hud.alpha * 0.7F));
        int rowHeight = 10;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) x, (float) y);
        context.getMatrices().scale(config.hud.scale, config.hud.scale);

        if (config.hud.style != AutoMinerConfig.HudStyle.MINIMAL) {
            int padding = config.hud.style == AutoMinerConfig.HudStyle.COMPACT ? 2 : 4;
            context.fill(-padding, -padding, maxWidth + padding * 2, lines.size() * rowHeight + padding * 2, background);
        }

        int rowY = 0;
        for (Text line : lines) {
            context.drawText(textRenderer, line, 0, rowY, color, true);
            rowY += rowHeight;
        }
        context.getMatrices().popMatrix();
    }

    private List<Text> buildLines(MinecraftClient client, AutoMinerConfig config) {
        List<Text> lines = new ArrayList<>();
        TargetCandidate target = controller.stats().currentTarget();

        if (config.hud.showEnabled) {
            lines.add(Text.literal("AutoMiner: " + (controller.isEnabled() ? "ON" : "OFF")));
        }
        if (config.hud.showMode) {
            lines.add(Text.literal("Mode: " + config.mining.targetMode + " / " + config.mining.areaMode));
        }
        if (config.hud.showTarget) {
            lines.add(Text.literal("Target: " + (target == null ? "-" : target.blockId())));
        }
        if (config.hud.showFoundCount) {
            lines.add(Text.literal("Found: " + controller.stats().foundBlocks()));
        }
        if (config.hud.showMinedCount) {
            lines.add(Text.literal("Mined: " + controller.stats().minedBlocks()));
        }
        if (config.hud.showDistance) {
            lines.add(Text.literal("Distance: " + String.format(java.util.Locale.ROOT, "%.2f", controller.distanceToTarget(client))));
        }
        if (config.hud.showState) {
            lines.add(Text.literal("State: " + controller.state()));
        }
        if (config.hud.showRestrictions) {
            lines.add(Text.literal("Limits: " + controller.stats().restrictionSummary()));
        }
        if (config.hud.showPreset) {
            lines.add(Text.literal("Preset: " + controller.activePreset()));
        }
        if (config.hud.showWarnings) {
            controller.stats().warnings().stream().limit(3).forEach(warning -> lines.add(Text.literal("Warn: " + warning)));
            if (!controller.stats().stopReason().isBlank()) {
                lines.add(Text.literal("Reason: " + controller.stats().stopReason()));
            }
        }
        return lines;
    }
}
