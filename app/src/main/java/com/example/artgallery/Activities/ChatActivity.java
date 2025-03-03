package com.example.artgallery.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.artgallery.Adapters.ChatAdapter;
import com.example.artgallery.Entities.ChatMessage;
import com.example.artgallery.R;
import com.example.artgallery.Utils.SteganographyUtil;
import com.example.artgallery.Utils.Utils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.InputStream;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private TextInputLayout textInputLayoutMessage;
    private ChatAdapter adapter;
    private final ArrayList<ChatMessage> messagesList = new ArrayList<>();
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;
    private String username;
    private Bitmap baseImage;
    private ImageButton buttonSend;
    private ContentObserver brightnessObserver;

    private volatile boolean isQuiet = false;
    private volatile boolean isNoiseDetectionRunning = false;
    private Thread noiseDetectionThread;
    private View secretGestureArea;
    private BroadcastReceiver volumeReceiver;


    /**
     * An {@link ActivityResultLauncher} for handling the result of an image selection activity.
     * <p>
     * This launcher is used to start an activity that allows the user to select an image
     * from their device's storage. When the activity finishes, the launcher's callback
     * is invoked with the result.
     * </p>
     * <p>
     * If the activity result is RESULT_OK, the selected image's URI is
     * extracted from the result data. The image is then loaded as a {@link Bitmap} and
     * assigned to the {@link #baseImage} field.
     * </p>
     * <p>
     * If any errors occur during the image loading process, an error message is logged.
     * </p>
     * <p>
     * The launcher is registered using {@link #registerForActivityResult} with a
     * {@link ActivityResultContracts.StartActivityForResult} contract.
     * </p>
     * <p>
     * This launcher is used by the {@link #openImagePicker()} method to launch the image
     * selection activity.
     * </p>
     */
    private final ActivityResultLauncher<Intent> imageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if(data != null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            try {
                                InputStream is = getContentResolver().openInputStream(imageUri);
                                Bitmap selectedImage = BitmapFactory.decodeStream(is);
                                if(is != null)
                                    is.close();
                                // Set the selected image as the new baseImage.
                                baseImage = selectedImage;
                                Log.d(TAG, "Image attached successfully.");
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "Failed to load selected image.");
                            }
                        }
                    }
                }
            }
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_chat);

        username = getIntent().getStringExtra("USERNAME");
        String chatRoom = getIntent().getStringExtra("CHAT_ROOM");
        if (chatRoom == null || chatRoom.isEmpty()) {
            chatRoom = "General Chat";
        }

        setupToolbar();

        findViews();

        gestureSetup(secretGestureArea);
        baseImage = BitmapFactory.decodeResource(getResources(), R.drawable.buh);
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatRoom);

        setupChat();

        registerListeners();

        startNoiseDetection();
    }

    /**
     * Sets up gesture detection for a specific view to handle double-tap and long-press gestures.
     * <p>
     * This method configures a {@link GestureDetector} to recognize double-tap and long-press
     * gestures on the provided {@code secretGestureArea} view. When a double-tap followed by a
     * long-press is detected, it toggles the decryption mode of the chat adapter if certain
     * conditions are met.
     * </p>
     * <p>
     * The gesture detection process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Create GestureDetector:</b> Creates a new {@link GestureDetector} instance,
     *       using a custom {@link GestureDetector.SimpleOnGestureListener} to handle the
     *       gestures.</li>
     *   <li><b>Handle onDown:</b> Overrides the {@link GestureDetector.SimpleOnGestureListener#onDown(MotionEvent)}
     *       method to return {@code true}, indicating that the gesture detector should
     *       continue to process touch events.</li>
     *   <li><b>Handle onDoubleTap:</b> Overrides the {@link GestureDetector.SimpleOnGestureListener#onDoubleTap(MotionEvent)}
     *       method to set a flag {@code doubleTapPerformed} to {@code true}, indicating that a
     *       double-tap has occurred.</li>
     *   <li><b>Handle onLongPress:</b> Overrides the {@link GestureDetector.SimpleOnGestureListener#onLongPress(MotionEvent)}
     *       method to perform the following actions:
     *     <ul>
     *       <li><b>Check for Double-Tap:</b> Checks if the {@code doubleTapPerformed} flag
     *           is {@code true}, ensuring that the long-press was preceded by a double-tap.</li>
     *       <li><b>Check Toggle Condition:</b> Calls {@link Utils#shouldShowToggle(Context, boolean)}
     *           to determine if the toggle should be shown based on the current context and
     *           the {@code isQuiet} flag.</li>
     *       <li><b>Toggle Decryption Mode:</b> If the toggle should be shown, checks the current
     *           decryption mode using {@link ChatAdapter#checkDecryptionMode()} and toggles it
     *           using {@link ChatAdapter#setDecryptMode(boolean)}.</li>
     *       <li><b>Reset Double-Tap Flag:</b> Resets the {@code doubleTapPerformed} flag to
     *           {@code false}.</li>
     *     </ul>
     *   </li>
     *   <li><b>Set OnTouchListener:</b> Sets an {@link View.OnTouchListener} on the
     *       {@code secretGestureArea} view to forward touch events to the
     *       {@link GestureDetector}.</li>
     *   <li><b>Handle Touch Events:</b> In the {@link View.OnTouchListener#onTouch(View, MotionEvent)}
     *       method, calls {@link GestureDetector#onTouchEvent(MotionEvent)} to process the
     *       touch event.</li>
     *   <li><b>Perform Click:</b> If the {@link GestureDetector} handles the event, calls
     *       {@link View#performClick()} to simulate a click on the view.</li>
     * </ol>
     * <p>
     * This setup allows for a hidden gesture (double-tap followed by a long-press) to toggle
     * the decryption mode in the chat adapter, providing a way to switch between encrypted
     * and decrypted views of the chat messages.
     * </p>
     *
     * @param secretGestureArea The {@link View} on which to detect the gestures.
     * @see GestureDetector
     * @see GestureDetector.SimpleOnGestureListener
     * @see View.OnTouchListener
     * @see MotionEvent
     * @see ChatAdapter
     * @see Utils
     */
    private void gestureSetup(View secretGestureArea) {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            private boolean doubleTapPerformed = false;

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                doubleTapPerformed = true;
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (doubleTapPerformed) {
                    if (Utils.shouldShowToggle(ChatActivity.this, isQuiet)) {
                        boolean currentMode = adapter.checkDecryptionMode();
                        adapter.setDecryptMode(!currentMode);
                    }
                    doubleTapPerformed = false;
                }
            }
        });

        secretGestureArea.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            if (handled) {
                v.performClick();
                return true;
            }
            return false;
        });
    }
    private void setupChat() {
        adapter = new ChatAdapter(messagesList, username);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void findViews() {
        textInputLayoutMessage = findViewById(R.id.textInputLayoutMessage);
        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        secretGestureArea = findViewById(R.id.secretGestureArea);

        initViews();
    }

    private void initViews() {
        findViewById(R.id.buttonAttach).setOnClickListener(v -> openImagePicker());
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConditionsUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNoiseDetection();
        if (brightnessObserver != null) {
            getContentResolver().unregisterContentObserver(brightnessObserver);
        }
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
         if (volumeReceiver != null) {
            unregisterReceiver(volumeReceiver);
         }
    }


    /**
     * Registers various listeners for system events and messages.
     * <p>
     * This method sets up listeners to monitor changes in volume and brightness,
     * as well as to receive messages. It ensures that the application is responsive
     * to these system-level events and can react accordingly.
     * </p>
     * <p>
     * The following listeners are registered:
     * <ul>
     *   <li>{@link #listenForMessages()}: Listens for incoming messages.</li>
     *   <li>{@link #registerVolumeListener()}: Listens for changes in the system volume.</li>
     *   <li>{@link #registerBrightnessObserver()}: Listens for changes in the system brightness.</li>
     * </ul>
     * </p>
     */
    private void registerListeners() {
        listenForMessages();
        registerVolumeListener();
        registerBrightnessObserver();
    }

    /**
     * Registers a {@link BroadcastReceiver} to listen for system volume changes.
     * <p>
     * This method sets up a receiver that listens for the {@code android.media.VOLUME_CHANGED_ACTION}
     * broadcast intent. When the system volume changes, the {@link #checkConditionsUpdate()}
     * method is called to update the UI based on the new volume level.
     * </p>
     * <p>
     * The receiver is registered with an {@link IntentFilter} that specifies the
     * {@code android.media.VOLUME_CHANGED_ACTION} intent.
     * </p>
     * <p>
     * The registered receiver is stored in the {@link #volumeReceiver} field for later
     * unregistration in {@link #onDestroy()}.
     * </p>
     */
    private void registerVolumeListener() {
        volumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConditionsUpdate();
            }
        };
        IntentFilter volumeFilter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, volumeFilter);
    }

    /**
     * Registers a {@link ContentObserver} to listen for changes in system brightness.
     * <p>
     * This method sets up an observer that listens for changes in the system brightness setting.
     * When the brightness changes, the {@link #checkConditionsUpdate()} method is called to update
     * the UI based on the new brightness level.
     * </p>
     * <p>
     * The observer is registered with the {@link android.content.ContentResolver} using the
     * {@link Settings.System#getUriFor(String)} method to obtain the URI for the
     * {@code SCREEN_BRIGHTNESS} setting. The observer is registered with a {@code true}
     * value for the {@code notifyForDescendants} parameter, indicating that the observer should
     * also be notified of changes to descendant settings.
     * </p>
     * <p>
     * The registered observer is stored in the {@link #brightnessObserver} field for later
     * unregistration in {@link #onDestroy()}.
     * </p>
     */
    private void registerBrightnessObserver() {
        brightnessObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                checkConditionsUpdate();
            }
        };
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true,
                brightnessObserver
        );
    }

    /**
     * Checks and updates the UI based on current conditions.
     * <p>
     * This method is called when certain system conditions change, such as volume or brightness,
     * or when the noise level changes. It performs the following actions:
     * </p>
     * <ul>
     *   <li>Invalidates the options menu to potentially show or hide the toggle menu item.</li>
     *   <li>Checks if the toggle should be shown based on the current noise level ({@link #isQuiet})
     *       and the result of {@link Utils#shouldShowToggle(Context, boolean)}.</li>
     *   <li>If the toggle should not be shown, sets the adapter to decrypt mode {@code false}
     *       using {@link ChatAdapter#setDecryptMode(boolean)}.</li>
     * </ul>
     * <p>
     * The method is wrapped in a try-catch block to handle any exceptions that might occur during
     * the UI update process.
     * </p>
     * <p>
     * This method should not be called if the activity is finishing.
     * </p>
     */
    private void checkConditionsUpdate() {
        try {
            if (!isFinishing()) {
                invalidateOptionsMenu();
                if (!Utils.shouldShowToggle(this, isQuiet)) {
                    adapter.setDecryptMode(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the contents of the Activity's standard options menu.
     * <p>
     * This method is called to create the options menu for the activity. It conditionally
     * inflates the menu based on the result of {@link Utils#shouldShowToggle(Context, boolean)}.
     * </p>
     * <p>
     * If {@code Utils.shouldShowToggle(this, isQuiet)} returns {@code true}, the menu
     * resource {@code R.menu.menu_chat} is inflated, adding the toggle menu item to the
     * options menu. Otherwise, no menu items are added.
     * </p>
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Utils.shouldShowToggle(this, isQuiet)) {
            getMenuInflater().inflate(R.menu.menu_chat, menu);
        }
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * <p>
     * This method handles the selection of items in the options menu. Specifically, it
     * handles the "Toggle Mode" menu item ({@code R.id.action_toggle_mode}).
     * </p>
     * <p>
     * When the "Toggle Mode" item is selected:
     * <ul>
     *   <li>The current decryption mode is checked using {@link ChatAdapter#checkDecryptionMode()}.</li>
     *   <li>The decryption mode is toggled (inverted) using {@link ChatAdapter#setDecryptMode(boolean)}.</li>
     *   <li>{@code true} is returned to indicate that the event has been handled.</li>
     * </ul>
     * </p>
     * <p>
     * If a different menu item is selected, the superclass's implementation is called to handle it.
     * </p>
     *
     * @param item The menu item that was selected.
     * @return {@code false} to allow normal menu processing to proceed, {@code true} to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_toggle_mode) {
            boolean currentMode = adapter.checkDecryptionMode();
            adapter.setDecryptMode(!currentMode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * Opens the image picker to allow the user to select an image.
     * <p>
     * This method launches an activity that allows the user to select an image from
     * their device's external storage. The selected image will be used as the base
     * image for steganographic encoding.
     * </p>
     * <p>
     * The method creates an {@link Intent} with the {@link Intent#ACTION_PICK} action.
     * It uses {@link Intent#setDataAndType(Uri, String)} to set both the data URI
     * ({@code android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI}) and the
     * MIME type ({@code "image/*"}) correctly.
     * </p>
     * <p>
     * The intent is then launched using the {@link #imageActivityResultLauncher}, which
     * will handle the result of the image selection activity.
     * </p>
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imageActivityResultLauncher.launch(intent);
    }

    /**
     * Sends a message to the chat room.
     * <p>
     * This method handles the process of sending a message to the chat room. It performs
     * the following steps:
     * </p>
     * <ol>
     *   <li>Retrieves the message text from the {@link #editTextMessage} and trims any leading
     *       or trailing whitespace.</li>
     *   <li>Checks if the message is empty. If it is, an error is displayed in the
     *       {@link #textInputLayoutMessage}, and the {@link #editTextMessage} is briefly
     *       animated to indicate the error.</li>
     *   <li>If the message is not empty, it is encrypted using the hybrid encryption method
     *       provided by {@link Utils#encryptMessageHybrid(String)}.</li>
     *   <li>The encrypted message (payload) is embedded into the {@link #baseImage} using
     *       steganography via {@link SteganographyUtil#encodeMessage(Bitmap, String)}.</li>
     *   <li>The resulting image is converted to a Base64-encoded string using
     *       {@link Utils#bitMapToString(Bitmap)}.</li>
     *   <li>A new {@link ChatMessage} object is created with the username, the encoded image,
     *       and the current timestamp.</li>
     *   <li>The {@link #buttonSend} is disabled to prevent multiple clicks.</li>
     *   <li>The message is pushed to the Firebase Realtime Database using {@link #messagesRef}.</li>
     *   <li>If the message is sent successfully:
     *     <ul>
     *       <li>The {@link #buttonSend} is re-enabled.</li>
     *       <li>The {@link #editTextMessage} is cleared.</li>
     *       <li>A "Message sent" toast is displayed.</li>
     *       <li>The {@link #baseImage} is reset to the default image.</li>
     *     </ul>
     *   </li>
     *   <li>If the message fails to send, the {@link #buttonSend} is re-enabled, and an error
     *       message is displayed in the {@link #textInputLayoutMessage}.</li>
     *   <li>If any exception occurs during the encryption or encoding process, an error message
     *       is displayed in the {@link #textInputLayoutMessage}.</li>
     * </ol>
     */
    private void sendMessage() {
        String inputMessage = editTextMessage.getText().toString().trim();
        if (inputMessage.isEmpty()) {
            textInputLayoutMessage.setError("Message cannot be empty");
            editTextMessage.animate().translationX(10).setDuration(50)
                    .withEndAction(() -> editTextMessage.animate().translationX(0).setDuration(50).start())
                    .start();
            return;
        } else {
            textInputLayoutMessage.setError(null);
        }
        try {
            // Encrypt the input message using the hybrid encryption method.
            String payload = Utils.encryptMessageHybrid(inputMessage);
            // Embed the payload (a Base64-encoded string) into the image.
            Bitmap encodedBitmap = SteganographyUtil.encodeMessage(baseImage, payload);
            String finalEncodedImage = Utils.bitMapToString(encodedBitmap);
            ChatMessage chatMessage = new ChatMessage(username, finalEncodedImage, System.currentTimeMillis());
            buttonSend.setEnabled(false);

            messagesRef.push().setValue(chatMessage)
                    .addOnSuccessListener(aVoid -> {
                        buttonSend.setEnabled(true);

                        editTextMessage.setText("");
                        Toast.makeText(ChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                        baseImage = BitmapFactory.decodeResource(getResources(), R.drawable.buh);
                    })
                    .addOnFailureListener(e -> {
                        buttonSend.setEnabled(true);
                        textInputLayoutMessage.setError("Failed to send message. Please try again.");
                    });
        } catch (Exception e) {
            e.printStackTrace();
            textInputLayoutMessage.setError("Encryption error: " + e.getMessage());
        }
    }

    /**
     * Sets up a listener to receive new messages from the Firebase Realtime Database.
     * <p>
     * This method initializes a {@link ChildEventListener} that listens for new messages
     * added to the {@link #messagesRef} in the Firebase Realtime Database. When a new
     * message is added, the following actions are performed:
     * </p>
     * <ol>
     *   <li>The message data is retrieved from the {@link DataSnapshot} and converted to a
     *       {@link ChatMessage} object using {@link DataSnapshot#getValue(Class)}.</li>
     *   <li>If the message is not null, it is added to the {@link #messagesList}.</li>
     *   <li>The {@link #adapter} is notified that a new item has been inserted using
     *       {@link ChatAdapter#notifyItemInserted(int)}.</li>
     *   <li>The {@link #recyclerView} is scrolled to the bottom to display the new message
     *       using {@link RecyclerView#smoothScrollToPosition(int)}.</li>
     * </ol>
     * <p>
     * The listener is attached to the {@link #messagesRef} using
     * {@link DatabaseReference#addChildEventListener(ChildEventListener)}.
     * </p>
     * <p>
     * The listener also implements the other {@link ChildEventListener} methods
     * ({@code onChildChanged}, {@code onChildRemoved}, {@code onChildMoved},
     * {@code onCancelled}), but they are currently left empty as no specific actions
     * are needed for those events.
     * </p>
     */
    private void listenForMessages() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    messagesList.add(message);
                    adapter.notifyItemInserted(messagesList.size() - 1);
                    recyclerView.smoothScrollToPosition(messagesList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) { }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        messagesRef.addChildEventListener(messagesListener);
    }

    /**
     * Starts a background thread to detect ambient noise levels.
     * <p>
     * This method initializes and starts a new {@link Thread} that continuously monitors
     * the ambient noise level using the device's microphone. The thread performs the
     * following actions:
     * </p>
     * <ol>
     *   <li>Sets the {@link #isNoiseDetectionRunning} flag to {@code true}.</li>
     *   <li>Calculates the minimum buffer size required for audio recording using
     *       {@link AudioRecord#getMinBufferSize(int, int, int)}.</li>
     *   <li>Checks for the {@code android.Manifest.permission.RECORD_AUDIO} permission. If
     *       the permission is not granted, it stops the noise detection and logs an error.</li>
     *   <li>Creates an {@link AudioRecord} instance to capture audio from the microphone.</li>
     *   <li>Allocates a buffer to store the audio data.</li>
     *   <li>Starts recording audio using {@link AudioRecord#startRecording()}.</li>
     *   <li>Enters a loop that continues as long as {@link #isNoiseDetectionRunning} is
     *       {@code true} and the thread is not interrupted.</li>
     *   <li>Reads audio data from the microphone into the buffer using
     *       {@link AudioRecord#read(short[], int, int)}.</li>
     *   <li>If data is read, calculates the root mean square (RMS) of the audio data.</li>
     *   <li>Calculates the decibel (dB) level from the RMS value.</li>
     *   <li>Logs the current sound level in dB.</li>
     *   <li>Determines if the environment is considered "quiet" (dB < 40).</li>
     *   <li>If the "quiet" state has changed, updates the {@link #isQuiet} flag and calls
     *       {@link #checkConditionsUpdate()} on the UI thread.</li>
     *   <li>Pauses for 500 milliseconds using {@link Thread#sleep(long)}.</li>
     *   <li>If an {@link InterruptedException} occurs, it is caught and logged.</li>
     *   <li>In the {@code finally} block, stops and releases the {@link AudioRecord} instance.</li>
     *   <li>Starts the thread using {@link Thread#start()}.</li>
     * </ol>
     * <p>
     * The noise detection thread is stored in the {@link #noiseDetectionThread} field.
     * </p>
     */
    private void startNoiseDetection() {
        isNoiseDetectionRunning = true;
        noiseDetectionThread = new Thread(() -> {
            int bufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            if (ActivityCompat.checkSelfPermission(ChatActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                stopNoiseDetection();
                Log.d("ToggleCondition", "RECORD_AUDIO permission not granted");
                return;
            }
            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
            );
            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            try {
                while (isNoiseDetectionRunning && !Thread.currentThread().isInterrupted()) {
                    int readSize = audioRecord.read(buffer, 0, bufferSize);
                    if (readSize > 0) {
                        double sum = 0;
                        for (int i = 0; i < readSize; i++) {
                            sum += buffer[i] * buffer[i];
                        }
                        double rms = Math.sqrt(sum / readSize);
                        double db = 20 * Math.log10(rms);
                        Log.d("ToggleConditions", "Sound level: " + db);
                        boolean newQuiet = db < 35;
                        if (newQuiet != isQuiet) {
                            isQuiet = newQuiet;
                            runOnUiThread(this::checkConditionsUpdate);
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                audioRecord.stop();
                audioRecord.release();
            }
        });
        noiseDetectionThread.start();
    }
    /**
     * Stops the noise detection thread.
     * <p>
     * This method is responsible for stopping the background thread that monitors
     * ambient noise levels. It performs the following actions:
     * </p>
     * <ol>
     *   <li>Sets the {@link #isNoiseDetectionRunning} flag to {@code false}, which signals
     *       the noise detection thread to terminate its loop.</li>
     *   <li>Checks if the {@link #noiseDetectionThread} is not null and is alive.</li>
     *   <li>If the thread is running, it interrupts the thread using
     *       {@link Thread#interrupt()}.</li>
     *   <li>Attempts to join the thread using {@link Thread#join()}, waiting for it to
     *       terminate.</li>
     *   <li>If an {@link InterruptedException} occurs during the join operation, it is
     *       caught and logged.</li>
     * </ol>
     * <p>
     * This method should be called when the activity is being destroyed or when noise
     * detection is no longer needed.
     * </p>
     */
    private void stopNoiseDetection() {
        isNoiseDetectionRunning = false;
        if (noiseDetectionThread != null && noiseDetectionThread.isAlive()) {
            noiseDetectionThread.interrupt();
            try {
                noiseDetectionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}