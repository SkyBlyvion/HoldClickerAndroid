package com.nicolas.holdclicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String TAG = "HoldClicker";

    private EditText durationInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (durationInput != null) {
            durationInput.setText(String.valueOf(HoldConfig.getHoldDurationMs(this)));
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));

        LinearLayout.LayoutParams matchWrap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap);

        TextView help = new TextView(this);
        help.setText(R.string.main_help);
        help.setTextSize(16f);
        help.setPadding(0, dp(18), 0, dp(18));
        root.addView(help, matchWrap);

        durationInput = new EditText(this);
        durationInput.setHint(R.string.duration_hint);
        durationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        durationInput.setText(String.valueOf(HoldConfig.getHoldDurationMs(this)));
        root.addView(durationInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));

        addButton(root, R.string.btn_save, v -> saveDuration(true));
        addButton(root, R.string.btn_overlay_permission, v -> openOverlaySettings());
        addButton(root, R.string.btn_accessibility_settings, v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (RuntimeException exception) {
                Log.e(TAG, "Unable to open accessibility settings", exception);
                Toast.makeText(this, "Impossible d'ouvrir les réglages d'accessibilité", Toast.LENGTH_SHORT).show();
            }
        });
        addButton(root, R.string.btn_start_floating, v -> startFloatingService());
        addButton(root, R.string.btn_stop_floating, v -> stopService(new Intent(this, FloatingControlsService.class)));

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void addButton(LinearLayout parent, int textResId, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(textResId);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
        lp.topMargin = dp(12);
        parent.addView(button, lp);
    }

    private boolean saveDuration(boolean showToast) {
        if (durationInput == null) {
            return false;
        }

        String raw = durationInput.getText().toString().trim();
        if (raw.isEmpty()) {
            if (showToast) {
                Toast.makeText(this, R.string.toast_enter_duration, Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        try {
            long value = Long.parseLong(raw);
            long safeValue = HoldConfig.setHoldDurationMs(this, value);
            durationInput.setText(String.valueOf(safeValue));
            if (showToast) {
                int messageId = safeValue == value
                        ? R.string.toast_duration_saved
                        : R.string.toast_duration_clamped;
                Toast.makeText(this, getString(messageId, safeValue), Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (NumberFormatException exception) {
            if (showToast) {
                Toast.makeText(this,
                        getString(R.string.toast_invalid_duration,
                                HoldConfig.getMinDurationMs(),
                                HoldConfig.getMaxDurationMs()),
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void startFloatingService() {
        if (!saveDuration(false)) {
            Toast.makeText(this,
                    getString(R.string.toast_invalid_duration,
                            HoldConfig.getMinDurationMs(),
                            HoldConfig.getMaxDurationMs()),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_need_overlay_permission, Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }

        try {
            startService(new Intent(this, FloatingControlsService.class));
            Toast.makeText(this, R.string.toast_floating_shown, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to start floating service", exception);
            Toast.makeText(this, R.string.toast_overlay_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to open package overlay settings", exception);
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            } catch (RuntimeException fallbackException) {
                Log.e(TAG, "Unable to open overlay settings", fallbackException);
                Toast.makeText(this, "Réglage introuvable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
