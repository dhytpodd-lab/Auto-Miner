package dev.danik.autominer.safety;

import java.util.List;

public record SafetyCheckResult(boolean shouldPause, boolean shouldStop, String reason, List<String> warnings) {
}
