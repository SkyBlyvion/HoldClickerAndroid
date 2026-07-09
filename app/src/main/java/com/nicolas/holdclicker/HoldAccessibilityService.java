package com.nicolas.holdclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public final class HoldAccessibilityService extends AccessibilityService {
    private static HoldAccessibilityService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean running;

    public static boolean isReady() {
        return instance != null;
    }

    public static void startHold(float x, float y, long durationMs) {
        HoldAccessibilityService service = instance;
        if (service != null) {
            service.dispatchHold(x, y, durationMs);
        }
    }

    public static void stopCurrentGesture() {
        HoldAccessibilityService service = instance;
        if (service != null) {
            service.running = false;
            service.mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onServiceConnected() {
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
        // Pas besoin de lire le contenu écran : ce service sert uniquement à envoyer un geste.
    }

    @Override
    public void onInterrupt() {
        stopCurrentGesture();
    }

    private void dispatchHold(float x, float y, long durationMs) {
        if (running) {
            return;
        }

        running = true;
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, durationMs);
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
        }
    }
}
