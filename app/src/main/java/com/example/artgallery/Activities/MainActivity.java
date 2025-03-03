package com.example.artgallery.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.artgallery.R;
import com.example.artgallery.Utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private Button buttonLogin;
    private static final String TAG = "FirebaseKeyStore";

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_CHAT_UNLOCKED = "chat_unlocked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loadMKey();
        findViews();

        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
            } else {
                // Move to the ChatActivity and pass the username
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().remove(KEY_CHAT_UNLOCKED).apply();
                Intent intent = new Intent(MainActivity.this, HubActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
            }
        });
    }

    /**
     * Loads the master key from Firebase Realtime Database.
     * <p>
     * This method retrieves the master key from the Firebase Realtime Database. It
     * fetches the value stored under the "master_key" node.
     * </p>
     * <p>
     * The method uses {@link DatabaseReference#addListenerForSingleValueEvent(ValueEventListener)}
     * to retrieve the data once.
     * </p>
     * <p>
     * If the master key is successfully retrieved:
     * <ul>
     *   <li>The key is expected to be a Base64-encoded string.</li>
     *   <li>The key is set using {@link Utils#setRemoteMasterKey(String)}.</li>
     * </ul>
     * </p>
     * <p>
     * If no master key is found or an error occurs:
     * <ul>
     *   <li>An error message is logged to the console.</li>
     * </ul>
     * </p>
     * <p>
     * This method should be called during the application's initialization process to
     * ensure the master key is available for encryption and decryption operations.
     * </p>
     */
    private static void loadMKey() {
        FirebaseDatabase.getInstance().getReference("master_key")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String encodedKey = snapshot.getValue(String.class);
                        if (encodedKey != null) {
                            Utils.setRemoteMasterKey(encodedKey);
                        } else {
                            Log.e("FirebaseKey", "No master key found in Firebase!");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseKey", "Error retrieving master key: " + error.getMessage());
                    }
                });
    }

    private void findViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        buttonLogin = findViewById(R.id.buttonLogin);
    }
}