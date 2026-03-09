package dev.danik.autominer.util;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int withAlpha(int rgb, float alpha) {
        int clamped = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }
}
