package com.nicolas.holdclicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public final class MainActivity extends android.app.Activity {
    private EditText durationInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("Hold Clicker");
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(this);
        help.setText("Réglage unique : durée du press & hold en millisecondes. Déplace la cible orange, puis utilise START/STOP depuis les boutons flottants.");
        help.setTextSize(16f);
        help.setPadding(0, dp(18), 0, dp(18));
        root.addView(help, new LinearLayout.LayoutParams(-1, -2));

        durationInput = new EditText(this);
        durationInput.setHint("Durée du hold en ms");
        durationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        durationInput.setText(String.valueOf(HoldConfig.getHoldDurationMs(this)));
        root.addView(durationInput, new LinearLayout.LayoutParams(-1, dp(58)));

        Button save = new Button(this);
        save.setText("Enregistrer la durée");
        save.setOnClickListener(v -> saveDuration());
        root.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));

        Button overlayPermission = new Button(this);
        overlayPermission.setText("Autoriser l’affichage flottant");
        overlayPermission.setOnClickListener(v -> openOverlaySettings());
        root.addView(overlayPermission, new LinearLayout.LayoutParams(-1, dp(56)));

        Button accessibility = new Button(this);
        accessibility.setText("Ouvrir les réglages d’accessibilité");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility, new LinearLayout.LayoutParams(-1, dp(56)));

        Button startFloating = new Button(this);
        startFloating.setText("Afficher les boutons flottants");
        startFloating.setOnClickListener(v -> startFloatingService());
        root.addView(startFloating, new LinearLayout.LayoutParams(-1, dp(56)));

        Button stopFloating = new Button(this);
        stopFloating.setText("Fermer les boutons flottants");
        stopFloating.setOnClickListener(v -> stopService(new Intent(this, FloatingControlsService.class)));
        root.addView(stopFloating, new LinearLayout.LayoutParams(-1, dp(56)));

        setContentView(root);
    }

    private void saveDuration() {
        String raw = durationInput.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "Entre une durée en ms", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long value = Long.parseLong(raw);
            HoldConfig.setHoldDurationMs(this, value);
            Toast.makeText(this, "Durée enregistrée", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Durée invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void startFloatingService() {
        saveDuration();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Autorise d’abord l’affichage flottant", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }
        startService(new Intent(this, FloatingControlsService.class));
        Toast.makeText(this, "Boutons flottants affichés", Toast.LENGTH_SHORT).show();
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void refreshStatus() {
        // Conservé volontairement minimal : pas d’état complexe, une seule option.
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
