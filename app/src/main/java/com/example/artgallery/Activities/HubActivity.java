package com.example.artgallery.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.artgallery.R;

public class HubActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_CHAT_UNLOCKED = "chat_unlocked";

    private RecyclerView recyclerViewChats;
    private String username;

    // Containers for UI elements.
    private View dummyInfoContainer;    // Shown when the QR hasn't been scanned
    private View chatFeatureContainer;  // Shown when unlocked (QR scanned)
    // Buttons inside the unlocked container.
    private Button buttonChat, buttonEmbedImage;
    private final ActivityResultLauncher<String> requestAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("Permissions","Audio record granted");
                } else {
                    Log.d("Permissions","Audio record refused");
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        username = getIntent().getStringExtra("USERNAME");

        Toolbar toolbar = findViewById(R.id.toolbarHub);
        setSupportActionBar(toolbar);

        findViews();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);

        initViews();
    }

    private void initViews() {
        // Set click listeners for the unlocked features.
        buttonChat.setOnClickListener(v -> {
            Intent intent = new Intent(HubActivity.this, ChatActivity.class);
            intent.putExtra("CHAT_ROOM", "General Chat");
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });
        buttonEmbedImage.setOnClickListener(v -> {
            Intent intent = new Intent(HubActivity.this, ImageEmbedActivity.class);
            startActivity(intent);
        });
    }

    private void findViews() {
        dummyInfoContainer = findViewById(R.id.dummyInfoContainer);
        chatFeatureContainer = findViewById(R.id.chatFeatureContainer);
        buttonChat = findViewById(R.id.buttonChat);
        buttonEmbedImage = findViewById(R.id.buttonEmbedImage);
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        invalidateOptionsMenu();
    }

    /**
     * Initializes the contents of the Activity's standard options menu.
     * <p>
     * This method is called to create the options menu for the activity. It inflates
     * the menu resource {@code R.menu.menu_hub}, adding the menu items to the options menu.
     * </p>
     * <p>
     * It also checks if the chat feature is unlocked by reading a value from the
     * {@link SharedPreferences}. If the chat is unlocked ({@code KEY_CHAT_UNLOCKED} is
     * {@code true}), the "Scan QR" menu item ({@code R.id.action_scan_qr}) is removed
     * from the menu.
     * </p>
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hub, menu);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean unlocked = prefs.getBoolean(KEY_CHAT_UNLOCKED, false);
        if (unlocked) {
            menu.removeItem(R.id.action_scan_qr);
        }
        return true;
    }

/**
 * This hook is called whenever an item in your options menu is selected.
 * <p>
 * This method handles the selection of items in the options menu. It handles the
 * following menu items:
 * </p>
 * <ul>
 *   <li><b>Scan QR ({@code R.id.action_scan_qr}):</b> Launches the
 *       {@code QRScannerActivity} to scan a QR code.</li>
 *   <li><b>Logout ({@code R.id.action_logout}):</b> Clears the chat unlocked flag
 *       from {@link SharedPreferences}, displays a "Logged out" toast, and navigates
 *       to the {@code MainActivity}. It also clears the activity stack to prevent
 *       the user from returning to this activity after logout.</li>
 * </ul>
 * <p>
 * If a different menu item is selected, the superclass's implementation is called to handle it.
 * </p>
 *
 * @param item The menu item that was selected.
 * @return {@code false} to allow normal menu processing to proceed, {@code true} to consume it here.
 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_scan_qr) {
            // Launch the QR scanner activity.
            Intent intent = new Intent(HubActivity.this, com.example.artgallery.Activities.QRScannerActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            // Clear the flag on logout.
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(KEY_CHAT_UNLOCKED).apply();
            Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HubActivity.this, com.example.artgallery.Activities.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * Updates the UI based on the chat unlock status.
     * <p>
     * This method checks the {@link SharedPreferences} to determine if the chat feature
     * has been unlocked (i.e., if a QR code has been successfully scanned). Based on
     * this status, it updates the visibility of the UI elements.
     * </p>
     * <p>
     * If the chat is unlocked ({@code KEY_CHAT_UNLOCKED} is {@code true}):
     * <ul>
     *   <li>The {@link #chatFeatureContainer} is set to {@link View#VISIBLE}.</li>
     *   <li>The {@link #dummyInfoContainer} is set to {@link View#GONE}.</li>
     * </ul>
     * </p>
     * <p>
     * If the chat is not unlocked ({@code KEY_CHAT_UNLOCKED} is {@code false}):
     * <ul>
     *   <li>The {@link #chatFeatureContainer} is set to {@link View#GONE}.</li>
     *   <li>The {@link #dummyInfoContainer} is set to {@link View#VISIBLE}.</li>
     * </ul>
     * </p>
     * <p>
     * This method is typically called after the QR code scanning process or when the
     * activity is created to ensure the UI reflects the correct state.
     * </p>
     */
    private void updateUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean unlocked = prefs.getBoolean(KEY_CHAT_UNLOCKED, false);
        if (unlocked) {
            chatFeatureContainer.setVisibility(View.VISIBLE);
            dummyInfoContainer.setVisibility(View.GONE);
        } else {
            chatFeatureContainer.setVisibility(View.GONE);
            dummyInfoContainer.setVisibility(View.VISIBLE);
        }
    }
}