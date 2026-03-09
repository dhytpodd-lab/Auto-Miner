package dev.danik.autominer.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {
    private RotationUtil() {
    }

    public static float targetYaw(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    public static float targetPitch(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    public static float stepAngle(float current, float target, float maxStep) {
        double delta = MathUtil.normalizeAngle(target - current);
        double stepped = current + Math.max(-maxStep, Math.min(maxStep, delta));
        return (float) stepped;
    }

    public static Vec3d center(BlockPos pos) {
        return Vec3d.ofCenter(pos);
    }
}
