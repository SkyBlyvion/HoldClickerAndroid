package com.nicolas.holdclicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class FloatingControlsService extends Service {
    private static final String TAG = "FloatingControls";
    private static final float TARGET_TOUCHABLE_ALPHA = 1.0f;
    private static final float TARGET_PASSTHROUGH_ALPHA = 0.65f;
    private static final long PASSTHROUGH_APPLY_DELAY_MS = 80L;
    private static final long TARGET_RESTORE_DELAY_MS = 250L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View controlsView;
    private View targetView;
    private WindowManager.LayoutParams controlsParams;
    private WindowManager.LayoutParams targetParams;
    private final Runnable restoreTargetTouchRunnable = () -> setTargetTouchable(true);

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        try {
            createTargetOverlay();
            createControlsOverlay();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to create floating controls", exception);
            Toast.makeText(this, R.string.toast_overlay_failed, Toast.LENGTH_SHORT).show();
            removeAllOverlays();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        removeAllOverlays();
        HoldAccessibilityService.stopCurrentGesture();
        super.onDestroy();
    }

    private void createTargetOverlay() {
        FrameLayout container = new FrameLayout(this);

        TextView target = new TextView(this);
        target.setText("+");
        target.setTextSize(28f);
        target.setGravity(Gravity.CENTER);
        target.setBackgroundResource(R.drawable.bg_target);
        container.addView(target, new FrameLayout.LayoutParams(dp(64), dp(64)));

        targetParams = baseParams(dp(64), dp(64));
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = dp(160);
        targetParams.y = dp(300);

        targetView = container;
        makeDraggable(container, container, targetParams, null);
        windowManager.addView(targetView, targetParams);
    }

    private void createControlsOverlay() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(8), dp(8), dp(8), dp(8));

        Button start = new Button(this);
        start.setText(R.string.btn_start);
        start.setAllCaps(false);
        start.setTextColor(0xFFFFFFFF);
        start.setBackgroundResource(R.drawable.bg_button);

        Button stop = new Button(this);
        stop.setText(R.string.btn_stop);
        stop.setAllCaps(false);
        stop.setTextColor(0xFFFFFFFF);
        stop.setBackgroundResource(R.drawable.bg_button_stop);

        layout.addView(start, new LinearLayout.LayoutParams(dp(110), dp(54)));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(dp(110), dp(54));
        stopLp.leftMargin = dp(8);
        layout.addView(stop, stopLp);

        controlsParams = baseParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        controlsParams.gravity = Gravity.TOP | Gravity.START;
        controlsParams.x = dp(24);
        controlsParams.y = dp(120);

        makeDraggable(layout, layout, controlsParams, null);
        configureOverlayButton(start, layout, controlsParams, this::startHold);
        configureOverlayButton(stop, layout, controlsParams, this::stopHold);

        controlsView = layout;
        windowManager.addView(controlsView, controlsParams);
    }

    private void startHold() {
        if (targetView == null || targetParams == null || !targetView.isAttachedToWindow()) {
            Toast.makeText(this, R.string.toast_overlay_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        int[] location = new int[2];
        targetView.getLocationOnScreen(location);
        int width = targetView.getWidth() > 0 ? targetView.getWidth() : targetParams.width;
        int height = targetView.getHeight() > 0 ? targetView.getHeight() : targetParams.height;
        float x = location[0] + width / 2f;
        float y = location[1] + height / 2f;
        long durationMs = HoldConfig.getHoldDurationMs(this);

        mainHandler.removeCallbacksAndMessages(null);
        setTargetTouchable(false);
        mainHandler.postDelayed(
                () -> dispatchHoldAtTarget(x, y, durationMs),
                PASSTHROUGH_APPLY_DELAY_MS);
    }

    private void dispatchHoldAtTarget(float x, float y, long durationMs) {
        HoldAccessibilityService.HoldStartResult result =
                HoldAccessibilityService.startHold(x, y, durationMs);
        switch (result) {
            case STARTED:
                Toast.makeText(this,
                        getString(R.string.toast_hold_started, durationMs),
                        Toast.LENGTH_SHORT).show();
                mainHandler.removeCallbacks(restoreTargetTouchRunnable);
                mainHandler.postDelayed(
                        restoreTargetTouchRunnable,
                        durationMs + TARGET_RESTORE_DELAY_MS);
                break;
            case SERVICE_UNAVAILABLE:
                setTargetTouchable(true);
                Toast.makeText(this, R.string.toast_need_accessibility, Toast.LENGTH_LONG).show();
                break;
            case ALREADY_RUNNING:
                setTargetTouchable(true);
                Toast.makeText(this, R.string.toast_hold_already_running, Toast.LENGTH_SHORT).show();
                break;
            case FAILED:
            default:
                setTargetTouchable(true);
                Toast.makeText(this, R.string.toast_hold_failed, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void stopHold() {
        mainHandler.removeCallbacksAndMessages(null);
        setTargetTouchable(true);
        boolean stopped = HoldAccessibilityService.stopCurrentGesture();
        Toast.makeText(this,
                stopped ? R.string.toast_hold_stopped : R.string.toast_no_hold_running,
                Toast.LENGTH_SHORT).show();
    }

    private WindowManager.LayoutParams baseParams(int width, int height) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        return new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
    }

    private void configureOverlayButton(
            Button button,
            View movingView,
            WindowManager.LayoutParams params,
            Runnable clickAction
    ) {
        button.setOnClickListener(view -> clickAction.run());
        makeDraggable(button, movingView, params, button::performClick);
    }

    private void setTargetTouchable(boolean touchable) {
        if (targetView == null || targetParams == null) {
            return;
        }

        if (touchable) {
            targetParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            targetParams.alpha = TARGET_TOUCHABLE_ALPHA;
        } else {
            targetParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            targetParams.alpha = TARGET_PASSTHROUGH_ALPHA;
        }
        updateOverlayLayout(targetView, targetParams);
    }

    private void makeDraggable(
            View touchView,
            View movingView,
            WindowManager.LayoutParams params,
            Runnable clickAction
    ) {
        int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        touchView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean dragging;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = Math.round(event.getRawX() - initialTouchX);
                        int deltaY = Math.round(event.getRawY() - initialTouchY);
                        if (!dragging
                                && Math.abs(deltaX) < touchSlop
                                && Math.abs(deltaY) < touchSlop) {
                            return true;
                        }
                        dragging = true;
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        updateOverlayLayout(movingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            if (clickAction != null) {
                                clickAction.run();
                            } else {
                                view.performClick();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void updateOverlayLayout(View view, WindowManager.LayoutParams params) {
        if (windowManager == null || view == null || !view.isAttachedToWindow()) {
            return;
        }

        try {
            windowManager.updateViewLayout(view, params);
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Overlay was detached before it could be moved", exception);
        }
    }

    private void removeAllOverlays() {
        removeOverlay(controlsView);
        removeOverlay(targetView);
        controlsView = null;
        targetView = null;
    }

    private void removeOverlay(View view) {
        if (view != null && windowManager != null) {
            try {
                windowManager.removeView(view);
            } catch (IllegalArgumentException exception) {
                Log.d(TAG, "Overlay was already removed", exception);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
