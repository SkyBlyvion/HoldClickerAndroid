package com.nicolas.holdclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public final class HoldAccessibilityService extends AccessibilityService {
    private static final String TAG = "HoldAccessibility";
    private static volatile HoldAccessibilityService instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean running;
    private float lastX;
    private float lastY;

    public enum HoldStartResult {
        STARTED,
        SERVICE_UNAVAILABLE,
        ALREADY_RUNNING,
        FAILED
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static HoldStartResult startHold(float x, float y, long durationMs) {
        HoldAccessibilityService service = instance;
        if (service == null) {
            return HoldStartResult.SERVICE_UNAVAILABLE;
        }
        return service.dispatchHold(x, y, durationMs);
    }

    public static boolean stopCurrentGesture() {
        HoldAccessibilityService service = instance;
        return service != null && service.cancelCurrentGesture();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        running = false;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Le service n'observe pas l'ecran, il sert uniquement a envoyer un geste.
    }

    @Override
    public void onInterrupt() {
        cancelCurrentGesture();
    }

    private HoldStartResult dispatchHold(float x, float y, long durationMs) {
        if (running) {
            return HoldStartResult.ALREADY_RUNNING;
        }
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || Float.isInfinite(y)) {
            return HoldStartResult.FAILED;
        }

        running = true;
        lastX = x;
        lastY = y;

        try {
            Path path = new Path();
            path.moveTo(x, y);

            long safeDurationMs = HoldConfig.clampDurationMs(durationMs);
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, safeDurationMs);
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();

            boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    running = false;
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    running = false;
                    super.onCancelled(gestureDescription);
                }
            }, mainHandler);

            if (!dispatched) {
                running = false;
                return HoldStartResult.FAILED;
            }
            return HoldStartResult.STARTED;
        } catch (RuntimeException exception) {
            running = false;
            Log.e(TAG, "Unable to dispatch hold gesture", exception);
            return HoldStartResult.FAILED;
        }
    }

    private boolean cancelCurrentGesture() {
        boolean wasRunning = running;
        running = false;
        if (wasRunning) {
            dispatchCancellationGesture();
        }
        return wasRunning;
    }

    private void dispatchCancellationGesture() {
        try {
            Path path = new Path();
            path.moveTo(lastX, lastY);
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 1);
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            dispatchGesture(gesture, null, null);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to dispatch cancellation gesture", exception);
        }
    }
}
