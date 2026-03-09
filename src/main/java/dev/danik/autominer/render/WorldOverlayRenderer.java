package dev.danik.autominer.render;

import dev.danik.autominer.config.AutoMinerConfig;
import dev.danik.autominer.config.ConfigManager;
import dev.danik.autominer.mining.AutoMinerController;
import dev.danik.autominer.targeting.TargetCandidate;
import dev.danik.autominer.util.ColorUtil;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Vec3d;

public final class WorldOverlayRenderer {
    private final ConfigManager configManager;
    private final AutoMinerController controller;

    public WorldOverlayRenderer(ConfigManager configManager, AutoMinerController controller) {
        this.configManager = configManager;
        this.controller = controller;
    }

    public void render(WorldRenderContext context) {
        AutoMinerConfig config = configManager.config();
        if (context.matrixStack() == null || context.consumers() == null || context.camera() == null) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        context.matrixStack().push();
        context.matrixStack().translate(-camera.x, -camera.y, -camera.z);

        if (config.visuals.showCandidateHighlights) {
            List<TargetCandidate> candidates = controller.visibleCandidates().stream().limit(12).toList();
            for (TargetCandidate candidate : candidates) {
                drawBox(context, candidate.pos().getX(), candidate.pos().getY(), candidate.pos().getZ(), 1.0F, 1.0F, 1.0F, 0.25F);
            }
        }

        if (config.visuals.showCurrentTarget && controller.stats().currentTarget() != null) {
            TargetCandidate target = controller.stats().currentTarget();
            float red = ColorUtil.red(config.hud.color);
            float green = ColorUtil.green(config.hud.color);
            float blue = ColorUtil.blue(config.hud.color);
            drawBox(context, target.pos().getX(), target.pos().getY(), target.pos().getZ(), red, green, blue, 0.95F);

            if (config.visuals.showTracer) {
                drawTracer(context, context.camera().getPos(), target.center(), red, green, blue);
            }
        }

        if (config.visuals.showWorkArea && config.mining.areaMode == AutoMinerConfig.AreaMode.CUSTOM_AREA) {
            AutoMinerConfig.ZoneArea area = config.zones.customArea;
            DebugRenderer.drawBox(context.matrixStack(), context.consumers(), area.minX, area.minY, area.minZ, area.maxX + 1.0D, area.maxY + 1.0D, area.maxZ + 1.0D, 0.4F, 0.9F, 1.0F, 0.65F);
        }

        context.matrixStack().pop();
    }

    private void drawBox(WorldRenderContext context, double x, double y, double z, float r, float g, float b, float a) {
        DebugRenderer.drawBox(context.matrixStack(), context.consumers(), x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, r, g, b, a);
    }

    private void drawTracer(WorldRenderContext context, Vec3d from, Vec3d to, float r, float g, float b) {
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / (double) steps;
            Vec3d point = from.lerp(to, t);
            DebugRenderer.drawBox(context.matrixStack(), context.consumers(), point.x - 0.03D, point.y - 0.03D, point.z - 0.03D, point.x + 0.03D, point.y + 0.03D, point.z + 0.03D, r, g, b, 0.75F);
        }
    }
}
