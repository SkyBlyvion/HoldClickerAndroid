package com.nicolas.holdclicker;

import android.content.Context;
import android.content.SharedPreferences;

public final class HoldConfig {
    private static final String PREFS = "hold_clicker_prefs";
    private static final String HOLD_DURATION_MS = "hold_duration_ms";
    private static final long DEFAULT_DURATION_MS = 3000L;
    private static final long MIN_DURATION_MS = 50L;
    private static final long MAX_DURATION_MS = 600_000L;

    private HoldConfig() {}

    public static long getHoldDurationMs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(HOLD_DURATION_MS, DEFAULT_DURATION_MS);
    }

    public static void setHoldDurationMs(Context context, long durationMs) {
        long safeValue = Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, durationMs));
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putLong(HOLD_DURATION_MS, safeValue);
        editor.apply();
    }
}
