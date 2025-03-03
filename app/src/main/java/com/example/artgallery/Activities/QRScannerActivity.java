package com.example.artgallery.Activities;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRScannerActivity extends AppCompatActivity {

    private static final String EXPECTED_CODE = "ArtGallery";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_CHAT_UNLOCKED = "chat_unlocked";
    private ScanOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("QR", result.getContents());
                if (result.getContents().equals(EXPECTED_CODE)) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putBoolean(KEY_CHAT_UNLOCKED, true).apply();
                    Toast.makeText(this, "Chat features unlocked!", Toast.LENGTH_LONG).show();
                    finish();

                } else {
                    Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show();
                    finish();

                }
            }
            finish();
        });

        configureOptions();

        barcodeLauncher.launch(options);
    }

    private void configureOptions() {
        options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan QR code to unlock chat features");
        options.setOrientationLocked(true);
    }
}