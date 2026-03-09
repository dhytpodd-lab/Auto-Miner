package dev.danik.autominer.util;

import java.util.List;
import java.util.StringJoiner;

public final class TextUtil {
    private TextUtil() {
    }

    public static String joinLimited(List<String> values, int limit) {
        StringJoiner joiner = new StringJoiner(", ");
        int count = Math.min(values.size(), limit);
        for (int i = 0; i < count; i++) {
            joiner.add(values.get(i));
        }
        if (values.size() > limit) {
            joiner.add("+" + (values.size() - limit));
        }
        return joiner.toString();
    }
}
