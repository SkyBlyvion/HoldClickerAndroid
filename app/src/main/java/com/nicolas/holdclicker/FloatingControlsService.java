package com.nicolas.holdclicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class FloatingControlsService extends Service {
    private WindowManager windowManager;
    private View controlsView;
    private View targetView;
    private WindowManager.LayoutParams controlsParams;
    private WindowManager.LayoutParams targetParams;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        createTargetOverlay();
        createControlsOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeOverlay(controlsView);
        removeOverlay(targetView);
        HoldAccessibilityService.stopCurrentGesture();
        super.onDestroy();
    }

    private void createTargetOverlay() {
        FrameLayout container = new FrameLayout(this);
        TextView target = new TextView(this);
        target.setText("+");
        target.setTextSize(28f);
        target.setGravity(Gravity.CENTER);
        target.setBackgroundResource(com.nicolas.holdclicker.R.drawable.bg_target);
        container.addView(target, new FrameLayout.LayoutParams(dp(64), dp(64)));

        targetParams = baseParams(dp(64), dp(64));
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = dp(160);
        targetParams.y = dp(300);

        makeDraggable(container, targetParams);
        targetView = container;
        windowManager.addView(targetView, targetParams);
    }

    private void createControlsOverlay() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(8), dp(8), dp(8), dp(8));

        Button start = new Button(this);
        start.setText("START");
        start.setAllCaps(false);
        start.setTextColor(0xFFFFFFFF);
        start.setBackgroundResource(com.nicolas.holdclicker.R.drawable.bg_button);

        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setAllCaps(false);
        stop.setTextColor(0xFFFFFFFF);
        stop.setBackgroundResource(com.nicolas.holdclicker.R.drawable.bg_button_stop);

        layout.addView(start, new LinearLayout.LayoutParams(dp(110), dp(54)));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(dp(110), dp(54));
        stopLp.leftMargin = dp(8);
        layout.addView(stop, stopLp);

        controlsParams = baseParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        controlsParams.gravity = Gravity.TOP | Gravity.START;
        controlsParams.x = dp(24);
        controlsParams.y = dp(120);

        makeDraggable(layout, controlsParams);

        start.setOnClickListener(view -> startHold());
        stop.setOnClickListener(view -> {
            HoldAccessibilityService.stopCurrentGesture();
            Toast.makeText(this, "Hold stoppé", Toast.LENGTH_SHORT).show();
        });

        controlsView = layout;
        windowManager.addView(controlsView, controlsParams);
    }

    private void startHold() {
        if (!HoldAccessibilityService.isReady()) {
            Toast.makeText(this, "Active le service d’accessibilité Hold Clicker", Toast.LENGTH_LONG).show();
            return;
        }

        int[] location = new int[2];
        targetView.getLocationOnScreen(location);
        float x = location[0] + targetView.getWidth() / 2f;
        float y = location[1] + targetView.getHeight() / 2f;
        long durationMs = HoldConfig.getHoldDurationMs(this);

        HoldAccessibilityService.startHold(x, y, durationMs);
        Toast.makeText(this, "Hold lancé : " + durationMs + " ms", Toast.LENGTH_SHORT).show();
    }

    private WindowManager.LayoutParams baseParams(int width, int height) {
        return new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
    }

    private void makeDraggable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + Math.round(event.getRawX() - initialTouchX);
                        params.y = initialY + Math.round(event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(v, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        return System.currentTimeMillis() - downTime > 180L;
                    default:
                        return false;
                }
            }
        });
    }

    private void removeOverlay(View view) {
        if (view != null) {
            try {
                windowManager.removeView(view);
            } catch (IllegalArgumentException ignored) {
                // Vue déjà retirée.
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
