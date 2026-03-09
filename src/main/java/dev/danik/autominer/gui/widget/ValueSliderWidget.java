package dev.danik.autominer.gui.widget;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class ValueSliderWidget extends SliderWidget {
    private final double min;
    private final double max;
    private final double step;
    private final Consumer<Double> onChange;
    private final Function<Double, Text> labelFactory;

    public ValueSliderWidget(int x, int y, int width, int height, double min, double max, double step, double value,
                             Function<Double, Text> labelFactory, Consumer<Double> onChange) {
        super(x, y, width, height, Text.empty(), normalize(min, max, value));
        this.min = min;
        this.max = max;
        this.step = step;
        this.labelFactory = labelFactory;
        this.onChange = onChange;
        updateMessage();
    }

    public double actualValue() {
        double raw = min + (max - min) * value;
        double stepped = Math.round(raw / step) * step;
        return Math.max(min, Math.min(max, stepped));
    }

    @Override
    protected void updateMessage() {
        setMessage(labelFactory.apply(actualValue()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(actualValue());
    }

    private static double normalize(double min, double max, double value) {
        if (max <= min) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
    }
}
