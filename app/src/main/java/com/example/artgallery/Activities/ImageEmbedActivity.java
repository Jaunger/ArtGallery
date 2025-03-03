package com.example.artgallery.Activities;

import static androidx.activity.result.contract.ActivityResultContracts.*;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.artgallery.R;
import com.example.artgallery.Utils.SteganographyUtil;
import com.example.artgallery.Utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;

public class ImageEmbedActivity extends AppCompatActivity {

    private static final String TAG = "ImageEmbedActivity";

    private Button buttonEncryptTab, buttonDecryptTab;
    private ImageView imageViewBaseEncrypt, imageViewFinalEncrypt;
    private TextView editTextEmbed;

    private Button buttonSelectImageDecrypt, buttonExtractText;
    private ImageView imageViewDecrypt;
    private TextView textViewExtracted;
    private Button buttonSelectImageEncrypt,buttonEmbedText,buttonDownloadImage;

    private View layoutEncryption, layoutDecryption;

    private boolean isEncryptionImagePicker = true;

    private Uri decryptionImageUri;

    private Bitmap finalEncodedBitmap;

    private Bitmap baseImage;

    /**
     * An {@link ActivityResultLauncher} for picking an image from the device's
     * storage.
     * <p>
     * This launcher is used to start an activity that allows the user to select
     * an image. The selected image can be used as a base image for encryption or
     * as the image to be decrypted, depending on the context.
     * </p>
     * <p>
     * The launcher is registered using
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with {@link StartActivityForResult()} as the contract.
     * </p>
     * <p>
     * The {@link ActivityResultCallback} associated with this launcher handles the
     * result of the image selection activity. It performs the following actions:
     * </p>
     * <ol>
     *   <li><b>Checks Result Code:</b> Verifies that the result code is
     *       {@link Activity#RESULT_OK}, indicating that the user successfully
     *       selected an image.</li>
     *   <li><b>Retrieves Image URI:</b> Extracts the {@link Uri} of the selected
     *       image from the {@link Intent} data.</li>
     *   <li><b>Loads Image:</b> Opens an {@link InputStream} for the image URI
     *       using {@link ContentResolver#openInputStream(Uri)} and decodes it into
     *       a {@link Bitmap} using {@link BitmapFactory#decodeStream(InputStream)}.</li>
     *   <li><b>Closes InputStream:</b> Closes the {@link InputStream} to release
     *       resources.</li>
     *   <li><b>Saves Base Image:</b> Stores the selected image as the base image
     *       in the {@code baseImage} field.</li>
     *   <li><b>Handles Encryption Mode:</b> If {@code isEncryptionImagePicker} is
     *       {@code true}, sets the selected image to the
     *       {@code imageViewBaseEncrypt} and displays a toast message.</li>
     *   <li><b>Handles Decryption Mode:</b> If {@code isEncryptionImagePicker} is
     *       {@code false}, stores the image URI in the {@code decryptionImageUri}
     *       field, sets the selected image to the {@code imageViewDecrypt}, and
     *       displays a toast message.</li>
     *   <li><b>Logs Success:</b> Logs a success message to the console.</li>
     *   <li><b>Handles Errors:</b> If any exception occurs during the process,
     *       prints the stack trace and logs an error message to the console.</li>
     * </ol>
     * <p>
     * This launcher is shared by both the encryption and decryption modes,
     * allowing the user to select an image for either operation.
     * </p>
     *
     * @see ActivityResultLauncher
     * @see StartActivityForResult
     * @see ActivityResultCallback
     * @see ActivityResult
     * @see Intent
     * @see Uri
     * @see Bitmap
     * @see ContentResolver
     * @see InputStream
     * @see BitmapFactory
     */
    private final ActivityResultLauncher<Intent> imageActivityResultLauncher = registerForActivityResult(
            new StartActivityForResult(),
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
                                baseImage = selectedImage;  // Save the selected image as base.
                                if (isEncryptionImagePicker) {
                                    imageViewBaseEncrypt.setImageBitmap(selectedImage);
                                    Toast.makeText(ImageEmbedActivity.this, "Base image selected for encryption", Toast.LENGTH_SHORT).show();
                                } else {
                                    // For decryption, store the URI for full-resolution loading.
                                    decryptionImageUri = imageUri;
                                    imageViewDecrypt.setImageBitmap(selectedImage);
                                    Toast.makeText(ImageEmbedActivity.this, "Image selected for decryption", Toast.LENGTH_SHORT).show();
                                }
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
        setContentView(R.layout.activity_image_embed);
        // Lock to portrait.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set up the toolbar with back navigation.
        Toolbar toolbar = findViewById(R.id.toolbarImageEmbed);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViews();

        // Initially show encryption layout.
        showEncryptionLayout();

        initViews();
    }

    private void initViews() {
        buttonEncryptTab.setOnClickListener(v -> showEncryptionLayout());
        buttonDecryptTab.setOnClickListener(v -> showDecryptionLayout());

        buttonSelectImageEncrypt.setOnClickListener(v -> {
            isEncryptionImagePicker = true;
            openImagePicker();
        });

        buttonEmbedText.setOnClickListener(v -> {
            String textToEmbed = editTextEmbed.getText().toString().trim();
            if (textToEmbed.isEmpty() || imageViewBaseEncrypt.getDrawable() == null) {
                Toast.makeText(this, "Please select a base image and enter text.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                embedText(textToEmbed);
                Toast.makeText(this, "Text embedded successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error embedding text: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        buttonDownloadImage.setOnClickListener(v -> downloadFinalImage());



        buttonSelectImageDecrypt.setOnClickListener(v -> {
            isEncryptionImagePicker = false;
            openImagePicker();
            textViewExtracted.setText("");
        });

        buttonExtractText.setOnClickListener(v -> {
            if (decryptionImageUri == null) {
                Toast.makeText(this, "No image selected for decryption.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                extractAndDecryptMessage();
            } catch (Exception e) {
                e.printStackTrace();
                textViewExtracted.setText("Error extracting text");
                Toast.makeText(this, "Error extracting text: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });
    }
    /**
     * Extracts and decrypts a hidden message from an image selected for decryption.
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     *   <li><b>Loads Image:</b> Loads the image from the {@code decryptionImageUri}
     *       using {@link ContentResolver#openInputStream(Uri)} and
     *       {@link BitmapFactory#decodeStream(InputStream)}.</li>
     *   <li><b>Handles Decoding Failure:</b> Checks if the image decoding was
     *       successful. If not, displays a toast message and returns.</li>
     *   <li><b>Decodes Message:</b> Extracts the hidden message from the image
     *       using {@link SteganographyUtil#decodeMessage(Bitmap)}.</li>
     *   <li><b>Handles Empty Payload:</b> Checks if the extracted payload is empty.
     *       If it is, displays a toast message and returns.</li>
     *   <li><b>Decrypts Message:</b> Decrypts the extracted payload using
     *       {@link Utils#decryptMessageHybrid(String)}.</li>
     *   <li><b>Handles Empty Decryption Result:</b> Checks if the decryption
     *       result is empty. If it is, displays a toast message.</li>
     *   <li><b>Displays Decrypted Text:</b> If the decryption is successful,
     *       displays the decrypted text in the {@code textViewExtracted} and
     *       shows a success toast message.</li>
     * </ol>
     *
     * @throws Exception If there is an error during the image loading, message
     *                   decoding, or message decryption process.
     */
    private void extractAndDecryptMessage() throws Exception {
        InputStream is = getContentResolver().openInputStream(decryptionImageUri);
        Bitmap bmp = BitmapFactory.decodeStream(is);
        if (is != null)
            is.close();
        if (bmp == null) {
            Toast.makeText(this, "Failed to decode image.", Toast.LENGTH_SHORT).show();
            return;
        }
        String extractedPayload = SteganographyUtil.decodeMessage(bmp);
        Log.d(TAG, "Extracted payload: " + extractedPayload);
        if (extractedPayload.trim().isEmpty()) {
            Toast.makeText(this, "No payload found in image.", Toast.LENGTH_SHORT).show();
            return;
        }
        String decryptedText = Utils.decryptMessageHybrid(extractedPayload);
        if (decryptedText.trim().isEmpty()) {
            Toast.makeText(this, "Decryption returned an empty result.", Toast.LENGTH_SHORT).show();
        } else {
            textViewExtracted.setText(decryptedText);
            Toast.makeText(this, "Text extracted successfully!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Encrypts the given text and embeds the resulting payload into the base image.
     * <p>
     * This method first encrypts the provided plaintext using the hybrid encryption method
     * (which generates a per-message key, encrypts the text, and wraps the key). It then retrieves
     * the base image from {@code imageViewBaseEncrypt} and embeds the encrypted payload into the image
     * using steganography. The resulting encoded image is displayed in {@code imageViewFinalEncrypt}
     * and stored in {@code finalEncodedBitmap} for later use (e.g., for download).
     * </p>
     *
     * @param textToEmbed The plaintext message to be encrypted and embedded into the image.
     * @throws Exception If an error occurs during encryption or during the steganography encoding process.
     */
    private void embedText(String textToEmbed) throws Exception {
        String payload = Utils.encryptMessageHybrid(textToEmbed);
        Bitmap baseImg = ((BitmapDrawable) imageViewBaseEncrypt.getDrawable()).getBitmap();
        Bitmap encodedBitmap = SteganographyUtil.encodeMessage(baseImg, payload);
        imageViewFinalEncrypt.setImageBitmap(encodedBitmap);
        finalEncodedBitmap = encodedBitmap;
    }

    private void findViews() {
        buttonEncryptTab = findViewById(R.id.buttonEncryptTab);
        buttonDecryptTab = findViewById(R.id.buttonDecryptTab);
        layoutEncryption = findViewById(R.id.layoutEncryption);
        layoutDecryption = findViewById(R.id.layoutDecryption);

        buttonSelectImageEncrypt = findViewById(R.id.buttonSelectImageEncrypt);
        imageViewBaseEncrypt = findViewById(R.id.imageViewBaseEncrypt);
        buttonEmbedText = findViewById(R.id.buttonEmbedText);
        imageViewFinalEncrypt = findViewById(R.id.imageViewFinalEncrypt);
        editTextEmbed = findViewById(R.id.editTextEmbed);
        buttonDownloadImage = findViewById(R.id.buttonDownloadImage);

        buttonSelectImageDecrypt = findViewById(R.id.buttonSelectImageDecrypt);
        imageViewDecrypt = findViewById(R.id.imageViewDecrypt);
        buttonExtractText = findViewById(R.id.buttonExtractText);
        textViewExtracted = findViewById(R.id.textViewExtracted);
    }

    /**
     * Opens the image picker to allow the user to select an image from their device's storage.
     * <p>
     * This method creates an {@link Intent} to start an activity that allows the user to pick
     * an image from their device's external storage. The intent is configured to filter for
     * image files using the "image/*" MIME type.
     * </p>
     * <p>
     * The selected image is then handled by the {@link #imageActivityResultLauncher}, which
     * processes the result of the image selection activity.
     * </p>
     * <p>
     * The process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Create Intent:</b> Creates a new {@link Intent} with the
     *       {@link Intent#ACTION_PICK} action.</li>
     *   <li><b>Set Data and Type:</b> Sets the data and type of the intent to
     *       {@link android.provider.MediaStore.Images.Media#EXTERNAL_CONTENT_URI}
     *       and "image/*", respectively. This specifies that the intent should
     *       allow the user to pick an image from external storage.</li>
     *   <li><b>Launch Activity:</b> Launches the image picker activity using
     *       {@link ActivityResultLauncher#launch(Object)} with the created intent.
     *       The result of this activity will be delivered to the
     *       {@link #imageActivityResultLauncher}.</li>
     * </ol>
     *
     * @see Intent
     * @see ActivityResultLauncher
     * @see android.provider.MediaStore.Images.Media#EXTERNAL_CONTENT_URI
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imageActivityResultLauncher.launch(intent);
    }

    /**
     * Downloads the final encoded image to the device's external storage.
     * <p>
     * This method saves the {@code finalEncodedBitmap} to the device's Pictures
     * directory. It handles the case where no image is available for download and
     * provides feedback to the user through toast messages.
     * </p>
     * <p>
     * The process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Check for Image:</b> Checks if {@code finalEncodedBitmap} is null.
     *       If it is, displays a toast message indicating that no image is
     *       available and returns.</li>
     *   <li><b>Prepare Image Metadata:</b> Creates {@link ContentValues} to store
     *       metadata for the image, including:
     *     <ul>
     *       <li><b>Display Name:</b> A unique title for the image file,
     *           prefixed with "EmbeddedImage_" and appended with the current
     *           timestamp.</li>
     *       <li><b>MIME Type:</b> "image/png" to indicate that the image is in PNG
     *           format.</li>
     *       <li><b>Relative Path:</b> "Pictures/ArtGallery" to specify the
     *           destination directory within the device's Pictures folder.</li>
     *     </ul>
     *   </li>
     *   <li><b>Insert Image Entry:</b> Inserts a new entry into the
     *       {@link MediaStore.Images.Media#EXTERNAL_CONTENT_URI} using
     *       {@link ContentResolver#insert(Uri, ContentValues)} to create a new
     *       image file.</li>
     *   <li><b>Open Output Stream:</b> Opens an {@link OutputStream} to the newly
     *       created image file using {@link ContentResolver#openOutputStream(Uri)}.</li>
     *   <li><b>Compress and Write Image:</b> Compresses the
     *       {@code finalEncodedBitmap} into PNG format with 100% quality and
     *       writes it to the output stream using
     *       {@link Bitmap#compress(Bitmap.CompressFormat, int, OutputStream)}.</li>
     *   <li><b>Close Output Stream:</b> Closes the output stream to release
     *       resources.</li>
     *   <li><b>Display Success Message:</b> Displays a toast message indicating
     *       that the image was downloaded successfully.</li>
     *   <li><b>Reset Encryption Page:</b> Calls {@link #resetEncryptionPage()} to
     *       reset the encryption page after the download is complete.</li>
     *   <li><b>Handle Errors:</b> If any exception occurs during the process,
     *       prints the stack trace, displays an error toast message, and returns.</li>
     * </ol>
     *
     * @see Bitmap
     * @see ContentValues
     * @see ContentResolver
     * @see MediaStore.Images.Media
     * @see OutputStream
     */
    private void downloadFinalImage() {
        if (finalEncodedBitmap == null) {
            Toast.makeText(this, "No image available to download.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String title = "EmbeddedImage_" + System.currentTimeMillis();
            String description = "Image with embedded text";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtGallery");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if(os != null) {
                    finalEncodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                Toast.makeText(this, "Image downloaded successfully!", Toast.LENGTH_SHORT).show();
                // Reset the encryption page after download.
                resetEncryptionPage();
            } else {
                Toast.makeText(this, "Error downloading image.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error downloading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void resetEncryptionPage() {
        imageViewBaseEncrypt.setImageDrawable(null);
        imageViewFinalEncrypt.setImageDrawable(null);
        editTextEmbed.setText("");
        finalEncodedBitmap = null;
        baseImage = null;
    }

    private void showEncryptionLayout() {
        layoutEncryption.setVisibility(View.VISIBLE);
        layoutDecryption.setVisibility(View.GONE);

        buttonEncryptTab.setSelected(true);
        buttonDecryptTab.setSelected(false);
    }

    private void showDecryptionLayout() {
        layoutEncryption.setVisibility(View.GONE);
        layoutDecryption.setVisibility(View.VISIBLE);

        buttonEncryptTab.setSelected(false);
        buttonDecryptTab.setSelected(true);
    }
}