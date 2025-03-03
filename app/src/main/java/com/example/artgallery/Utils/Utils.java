package com.example.artgallery.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private static final String TAG = "Utils";


    /**
     * Converts a {@link Bitmap} image to a Base64-encoded string.
     * <p>
     * This method takes a {@link Bitmap} object and converts it into a string
     * representation using Base64 encoding. The process involves the following steps:
     * </p>
     * <ol>
     *   <li>Compresses the {@link Bitmap} into a PNG format using
     *       {@link Bitmap#compress(Bitmap.CompressFormat, int, java.io.OutputStream)}.
     *       The compression quality is set to 100, indicating no loss of quality.</li>
     *   <li>Writes the compressed image data to a {@link ByteArrayOutputStream}.</li>
     *   <li>Retrieves the image data as a byte array using
     *       {@link ByteArrayOutputStream#toByteArray()}.</li>
     *   <li>Encodes the byte array into a Base64 string using
     *       {@link Base64#encodeToString(byte[], int)} with the {@link Base64#DEFAULT}
     *       flag.</li>
     * </ol>
     * <p>
     * The resulting Base64 string can be used to store or transmit the image data
     * in a text-based format.
     * </p>
     *
     * @param bitmap The {@link Bitmap} image to convert.
     * @return A Base64-encoded string representation of the {@link Bitmap}.
     */
    public static String bitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    /**
     * Converts a Base64-encoded string back into a {@link Bitmap} image.
     * <p>
     * This method takes a Base64-encoded string and decodes it to reconstruct the
     * original {@link Bitmap} image. The process involves the following steps:
     * </p>
     * <ol>
     *   <li>Decodes the Base64 string into a byte array using
     *       {@link Base64#decode(String, int)} with the {@link Base64#DEFAULT} flag.</li>
     *   <li>Creates a {@link Bitmap} from the byte array using
     *       {@link BitmapFactory#decodeByteArray(byte[], int, int)}.</li>
     * </ol>
     * <p>
     * If any exception occurs during the decoding process, an error message is logged,
     * and {@code null} is returned.
     * </p>
     *
     * @param encodedString The Base64-encoded string to decode.
     * @return The {@link Bitmap} image represented by the encoded string, or {@code null}
     *         if an error occurred during decoding.
     */
    public static Bitmap stringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding bitmap: " + e.getMessage());
            return null;
        }
    }


    /**
     * Retrieves the {@link Bitmap} from an {@link AppCompatImageView}.
     * <p>
     * This method extracts the {@link Bitmap} that is currently displayed in an
     * {@link AppCompatImageView}. It assumes that the {@link AppCompatImageView} is
     * displaying a {@link BitmapDrawable}.
     * </p>
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     *   <li>Gets the {@link Drawable} from the {@link AppCompatImageView} using
     *       {@link AppCompatImageView#getDrawable()}.</li>
     *   <li>Casts the {@link Drawable} to a {@link BitmapDrawable}.</li>
     *   <li>Retrieves the underlying {@link Bitmap} from the {@link BitmapDrawable}
     *       using {@link BitmapDrawable#getBitmap()}.</li>
     * </ol>
     * <p>
     * <b>Note:</b> This method will throw a {@link ClassCastException} if the
     * {@link AppCompatImageView} is not displaying a {@link BitmapDrawable}.
     * </p>
     *
     * @param imageView The {@link AppCompatImageView} from which to retrieve the {@link Bitmap}.
     * @return The {@link Bitmap} displayed in the {@link AppCompatImageView}.
     * @throws ClassCastException if the {@link AppCompatImageView} is not displaying a {@link BitmapDrawable}.
     */
    public static Bitmap getBitmapFromImageView(AppCompatImageView imageView) {
        return ((BitmapDrawable) imageView.getDrawable()).getBitmap();
    }

    /**
     * Retrieves the current screen brightness level.
     * <p>
     * This method fetches the current screen brightness setting from the system settings.
     * The brightness level is an integer value typically ranging from 0 to 255, where 0
     * represents the minimum brightness and 255 represents the maximum brightness.
     * </p>
     * <p>
     * The method uses {@link Settings.System#getInt(ContentResolver, String)} to retrieve
     * the {@link Settings.System#SCREEN_BRIGHTNESS} setting.
     * </p>
     * <p>
     * If the screen brightness setting is not found, a
     * {@link Settings.SettingNotFoundException} is caught, an error message is printed
     * to the stack trace, and 0 is returned as a default value.
     * </p>
     *
     * @param context The {@link Context} used to access the {@link ContentResolver}.
     * @return The current screen brightness level as an integer, or 0 if the setting is not found.
     */
    public static int getScreenBrightness(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Retrieves the current volume level as a percentage.
     * <p>
     * This method fetches the current volume level for the music stream and returns it
     * as a percentage of the maximum volume. The steps involved are:
     * </p>
     * <ol>
     *   <li>Gets an instance of {@link AudioManager} using
     *       {@link Context#getSystemService(String)} with {@link Context#AUDIO_SERVICE}.</li>
     *   <li>Checks if the {@link AudioManager} instance is not null.</li>
     *   <li>If the {@link AudioManager} is valid:
     *     <ul>
     *       <li>Retrieves the current volume for the music stream using
     *           {@link AudioManager#getStreamVolume(int)} with
     *           {@link AudioManager#STREAM_MUSIC}.</li>
     *       <li>Retrieves the maximum volume for the music stream using
     *           {@link AudioManager#getStreamMaxVolume(int)} with
     *           {@link AudioManager#STREAM_MUSIC}.</li>
     *       <li>Calculates the volume percentage by dividing the current volume by the
     *           maximum volume, multiplying by 100, and casting the result to an integer.</li>
     *     </ul>
     *   </li>
     *   <li>If the {@link AudioManager} is null, returns 0.</li>
     * </ol>
     *
     * @param context The {@link Context} used to access the {@link AudioManager}. Must not be null.
     * @return The current volume level as a percentage (0-100), or 0 if the
     *         {@link AudioManager} is not available.
     * @throws NullPointerException if the context is null.
     */
    public static int getVolumeLevel(@NonNull Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            return (int) (((float) currentVolume / maxVolume) * 100);
        }
        return 0;
    }

    /**
     * Determines whether the toggle should be displayed based on current conditions.
     * <p>
     * This method checks the current screen brightness, volume level, and ambient
     * noise status to determine if a toggle (e.g., a UI element) should be displayed.
     * The conditions for showing the toggle are:
     * </p>
     * <ul>
     *   <li><b>Brightness:</b> The screen brightness must be greater than or equal to 80.</li>
     *   <li><b>Volume:</b> The volume level must be greater than or equal to 20%.</li>
     *   <li><b>Quiet:</b> The environment must be considered "quiet" (as indicated by the
     *       {@code isQuiet} parameter).</li>
     * </ul>
     * <p>
     * The method retrieves the screen brightness using {@link #getScreenBrightness(Context)}
     * and the volume level using {@link #getVolumeLevel(Context)}. It also logs the
     * current brightness, volume, and quiet status for debugging purposes.
     * </p>
     *
     * @param context The {@link Context} used to access system settings.
     * @param isQuiet A boolean indicating whether the environment is considered "quiet".
     * @return {@code true} if the toggle should be displayed; {@code false} otherwise.
     */
    public static boolean shouldShowToggle(Context context, boolean isQuiet) {
        int brightness = getScreenBrightness(context);
        int volumePercentage = getVolumeLevel(context);
        Log.d(TAG, "Brightness: " + brightness + " Volume: " + volumePercentage + "%, isQuiet: " + isQuiet);
        return (brightness >= 80 && volumePercentage <= 20 && isQuiet);
    }

    // Static variable to hold the master key retrieved from remote.
    private static SecretKey REMOTE_MASTER_KEY = null;

    /**
     * Call this method after retrieving the Base64-encoded master key from your remote database.
     *
     * @param base64Key A Base64-encoded string representing the AES master key.
     */
    public static void setRemoteMasterKey(String base64Key) {
        if (base64Key != null && !base64Key.isEmpty()) {
            byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
            REMOTE_MASTER_KEY = new SecretKeySpec(keyBytes, "AES");
            Log.d(TAG, "Remote master key has been set.");
        } else {
            Log.e(TAG, "Received empty master key from remote.");
        }
    }

    /**
     * Retrieve the master key stored in REMOTE_MASTER_KEY.
     * Throws an exception if the key is not set.
     */
    private static SecretKey getMasterKey() throws Exception {
        if (REMOTE_MASTER_KEY == null) {
            throw new Exception("Master key not set. Retrieve it from remote database first.");
        }
        return REMOTE_MASTER_KEY;
    }

    /**
     * Generates a random AES encryption key.
     * <p>
     * This method creates a new, cryptographically secure random key for use with
     * the Advanced Encryption Standard (AES) algorithm. The key is generated using
     * the {@link KeyGenerator} class.
     * </p>
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     *   <li>Gets an instance of {@link KeyGenerator} for the "AES" algorithm using
     *       {@link KeyGenerator#getInstance(String)}.</li>
     *   <li>Initializes the {@link KeyGenerator} with a key size of 128 bits using
     *       {@link KeyGenerator#init(int)}.</li>
     *   <li>Generates the random key using {@link KeyGenerator#generateKey()}.</li>
     * </ol>
     * <p>
     * The generated key can be used for encrypting and decrypting data with AES.
     * </p>
     *
     * @return A randomly generated {@link SecretKey} for AES encryption.
     * @throws Exception If there is an error generating the key.
     */
    public static SecretKey generateRandomKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // 128-bit key; adjust as needed.
        return keyGen.generateKey();
    }

    /**
     * Encrypts data using AES encryption in CBC mode with PKCS5 padding.
     * <p>
     * This method encrypts the provided plaintext data using the Advanced Encryption
     * Standard (AES) algorithm in Cipher Block Chaining (CBC) mode with PKCS5 padding.
     * It uses a randomly generated Initialization Vector (IV) for each encryption operation.
     * </p>
     * <p>
     * The encryption process involves the following steps:
     * </p>
     * <ol>
     *   <li>Gets an instance of {@link Cipher} for the "AES/CBC/PKCS5Padding" transformation
     *       using {@link Cipher#getInstance(String)}.</li>
     *   <li>Gets the block size of the cipher using {@link Cipher#getBlockSize()}.</li>
     *   <li>Generates a random Initialization Vector (IV) of the appropriate block size
     *       using {@link SecureRandom#nextBytes(byte[])}.</li>
     *   <li>Creates an {@link IvParameterSpec} from the generated IV.</li>
     *   <li>Initializes the {@link Cipher} for encryption mode using
     *       {@link Cipher#init(int, java.security.Key, java.security.spec.AlgorithmParameterSpec)}
     *       with the provided {@link SecretKey}, the {@link IvParameterSpec}, and
     *       {@link Cipher#ENCRYPT_MODE}.</li>
     *   <li>Encrypts the plaintext data using {@link Cipher#doFinal(byte[])}.</li>
     *   <li>Creates a {@link ByteBuffer} to store both the IV and the ciphertext.</li>
     *   <li>Puts the IV into the {@link ByteBuffer}.</li>
     *   <li>Puts the ciphertext into the {@link ByteBuffer}.</li>
     *   <li>Returns the combined IV and ciphertext as a byte array using
     *       {@link ByteBuffer#array()}.</li>
     * </ol>
     * <p>
     * The output of this method is a byte array that contains the IV followed by the
     * ciphertext. This format is necessary for decryption, as the IV is required for
     * the decryption process.
     * </p>
     *
     * @param key       The {@link SecretKey} to use for encryption.
     * @param plaintext The plaintext data to encrypt as a byte array.
     * @return A byte array containing the IV followed by the ciphertext.
     * @throws Exception If there is an error during the encryption process.
     */
    public static byte[] encryptData(SecretKey key, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        int blockSize = cipher.getBlockSize();
        byte[] iv = new byte[blockSize];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        return byteBuffer.array();
    }

    /**
     * Decrypts data that was encrypted using AES encryption in CBC mode with PKCS5 padding.
     * <p>
     * This method decrypts data that was previously encrypted using the
     * {@link #encryptData(SecretKey, byte[])} method. It expects the input to be a byte
     * array containing the Initialization Vector (IV) followed by the ciphertext.
     * </p>
     * <p>
     * The decryption process involves the following steps:
     * </p>
     * <ol>
     *   <li>Gets an instance of {@link Cipher} for the "AES/CBC/PKCS5Padding" transformation
     *       using {@link Cipher#getInstance(String)}.</li>
     *   <li>Gets the block size of the cipher using {@link Cipher#getBlockSize()}.</li>
     *   <li>Wraps the input byte array (IV and ciphertext) in a {@link ByteBuffer} using
     *       {@link ByteBuffer#wrap(byte[])}.</li>
     *   <li>Creates a byte array to hold the IV, with a size equal to the block size.</li>
     *   <li>Extracts the IV from the {@link ByteBuffer} using {@link ByteBuffer#get(byte[])}.</li>
     *   <li>Creates a byte array to hold the ciphertext, with a size equal to the remaining
     *       bytes in the {@link ByteBuffer} using {@link ByteBuffer#remaining()}.</li>
     *   <li>Extracts the ciphertext from the {@link ByteBuffer} using
     *       {@link ByteBuffer#get(byte[])}.</li>
     *   <li>Creates an {@link IvParameterSpec} from the extracted IV.</li>
     *   <li>Initializes the {@link Cipher} for decryption mode using
     *       {@link Cipher#init(int, java.security.Key, java.security.spec.AlgorithmParameterSpec)}
     *       with the provided {@link SecretKey}, the {@link IvParameterSpec}, and
     *       {@link Cipher#DECRYPT_MODE}.</li>
     *   <li>Decrypts the ciphertext using {@link Cipher#doFinal(byte[])}.</li>
     * </ol>
     * <p>
     * This method is designed to work with the output of the
     * {@link #encryptData(SecretKey, byte[])} method.
     * </p>
     *
     * @param key             The {@link SecretKey} to use for decryption.
     * @param ivAndCiphertext A byte array containing the IV followed by the ciphertext.
     * @return The decrypted plaintext data as a byte array.
     * @throws Exception If there is an error during the decryption process.
     */
    public static byte[] decryptData(SecretKey key, byte[] ivAndCiphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        int blockSize = cipher.getBlockSize();
        ByteBuffer byteBuffer = ByteBuffer.wrap(ivAndCiphertext);
        byte[] iv = new byte[blockSize];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(ciphertext);
    }

    /**
     * Wraps (encrypts) a {@link SecretKey} using a master key.
     * <p>
     * This method wraps (encrypts) a given {@link SecretKey} using a master key.
     * Key wrapping is a process of encrypting a key with another key to protect it
     * during storage or transmission. This method uses the AESWrap algorithm for key
     * wrapping.
     * </p>
     * <p>
     * The wrapping process involves the following steps:
     * </p>
     * <ol>
     *   <li>Retrieves the master key using {@link #getMasterKey()}.</li>
     *   <li>Gets an instance of {@link Cipher} for the "AESWrap" transformation using
     *       {@link Cipher#getInstance(String)}.</li>
     *   <li>Initializes the {@link Cipher} for wrap mode using
     *       {@link Cipher#init(int, java.security.Key)} with the master key and
     *       {@link Cipher#WRAP_MODE}.</li>
     *   <li>Wraps the key to be wrapped using {@link Cipher#wrap(java.security.Key)}.</li>
     * </ol>
     * <p>
     * The output of this method is a byte array representing the wrapped key.
     * </p>
     *
     * @param keyToWrap The {@link SecretKey} to be wrapped.
     * @return A byte array representing the wrapped key.
     * @throws Exception If there is an error during the key wrapping process.
     */
    public static byte[] wrapKey(SecretKey keyToWrap) throws Exception {
        SecretKey masterKey = getMasterKey();
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.WRAP_MODE, masterKey);
        return cipher.wrap(keyToWrap);
    }

    /**
     * Unwraps (decrypts) a wrapped {@link SecretKey} using the master key.
     * <p>
     * This method unwraps (decrypts) a {@link SecretKey} that was previously wrapped
     * using the {@link #wrapKey(SecretKey)} method. Key unwrapping is the process of
     * decrypting a key that was encrypted with another key. This method uses the
     * AESWrap algorithm for key unwrapping.
     * </p>
     * <p>
     * The unwrapping process involves the following steps:
     * </p>
     * <ol>
     *   <li>Retrieves the master key using {@link #getMasterKey()}.</li>
     *   <li>Gets an instance of {@link Cipher} for the "AESWrap" transformation using
     *       {@link Cipher#getInstance(String)}.</li>
     *   <li>Initializes the {@link Cipher} for unwrap mode using
     *       {@link Cipher#init(int, java.security.Key)} with the master key and
     *       {@link Cipher#UNWRAP_MODE}.</li>
     *   <li>Unwraps the wrapped key using {@link Cipher#unwrap(byte[], String, int)},
     *       specifying "AES" as the algorithm and {@link Cipher#SECRET_KEY} as the key type.</li>
     *   <li>Casts the unwrapped key to a {@link SecretKey}.</li>
     * </ol>
     * <p>
     * This method is designed to work with the output of the {@link #wrapKey(SecretKey)}
     * method.
     * </p>
     *
     * @param wrappedKey The byte array representing the wrapped key.
     * @return The unwrapped {@link SecretKey}.
     * @throws Exception If there is an error during the key unwrapping process.
     */
    public static SecretKey unwrapKey(byte[] wrappedKey) throws Exception {
        SecretKey masterKey = getMasterKey();
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.UNWRAP_MODE, masterKey);
        return (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
    }

    /**
     * Encrypts a message using a hybrid encryption scheme with a randomly generated
     * per-message key.
     * <p>
     * This method implements a hybrid encryption scheme that combines symmetric and
     * asymmetric encryption techniques to securely encrypt a message. It uses a
     * randomly generated AES key to encrypt the message (symmetric encryption) and
     * then encrypts the AES key itself using a master key (asymmetric encryption).
     * </p>
     * <p>
     * The encryption process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Input Validation:</b> Checks if the input message is null. If it is,
     *       throws an {@link Exception}.</li>
     *   <li><b>Per-Message Key Generation:</b> Generates a random AES key using
     *       {@link #generateRandomKey()}. This key will be used to encrypt the message.</li>
     *   <li><b>Data Encryption:</b> Encrypts the message using the per-message key and
     *       {@link #encryptData(SecretKey, byte[])}. The output is the encrypted data
     *       (ciphertext) along with the Initialization Vector (IV).</li>
     *   <li><b>Key Wrapping:</b> Encrypts the per-message key using the master key and
     *       {@link #wrapKey(SecretKey)}. This step protects the per-message key.</li>
     *   <li><b>Wrapped Key Validation:</b> Checks if the wrapped key is null or empty.
     *       If it is, throws an {@link Exception}.</li>
     *   <li><b>Payload Construction:</b> Creates a payload with the following format:
     *     <ul>
     *       <li><b>Wrapped Key Length (4 bytes):</b> An integer representing the length
     *           of the wrapped key.</li>
     *       <li><b>Wrapped Key:</b> The wrapped (encrypted) per-message key.</li>
     *       <li><b>Encrypted Data:</b> The encrypted message data (IV + ciphertext).</li>
     *     </ul>
     *   </li>
     *   <li><b>Payload Encoding:</b> Encodes the payload using Base64 encoding for safe
     *       transmission or storage.</li>
     * </ol>
     * <p>
     * <b>Output Payload Format (Before Base64 Encoding):</b>
     * </p>
     * <pre>
     * [4-byte length of wrapped key][wrapped key][encrypted data (IV + ciphertext)]
     * </pre>
     * <p>
     * <b>Output:</b> The method returns a Base64-encoded string representing the
     * encrypted payload.
     * </p>
     *
     * @param message The message to encrypt. Must not be null.
     * @return A Base64-encoded string representing the encrypted payload.
     * @throws Exception If there is an error during the encryption process, if the message is null, or if the wrapped key is null or empty.
     */
    public static String encryptMessageHybrid(String message) throws Exception {
        if (message == null) {
            throw new Exception("Input message is null.");
        }
        SecretKey perMessageKey = generateRandomKey();
        byte[] encryptedData = encryptData(perMessageKey, message.getBytes(StandardCharsets.UTF_8));
        byte[] wrappedKey = wrapKey(perMessageKey);
        if (wrappedKey == null || wrappedKey.length == 0) {
            throw new Exception("Wrapped key is null or empty.");
        }
        ByteBuffer buffer = ByteBuffer.allocate(4 + wrappedKey.length + encryptedData.length);
        buffer.putInt(wrappedKey.length);
        buffer.put(wrappedKey);
        buffer.put(encryptedData);
        byte[] payload = buffer.array();
        return Base64.encodeToString(payload, Base64.DEFAULT);
    }

    /**
     * Decrypts a message that was encrypted using the hybrid encryption scheme
     * implemented by {@link #encryptMessageHybrid(String)}.
     * <p>
     * This method decrypts a message that was previously encrypted using the
     * {@link #encryptMessageHybrid(String)} method. It reverses the hybrid
     * encryption process by first unwrapping the per-message key and then using
     * that key to decrypt the message data.
     * </p>
     * <p>
     * The decryption process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Input Validation:</b> Checks if the input payload is null. If it is,
     *       throws an {@link Exception}.</li>
     *   <li><b>Payload Decoding:</b> Decodes the Base64-encoded payload into a byte
     *       array using {@link Base64#decode(String, int)}.</li>
     *   <li><b>Payload Parsing:</b> Wraps the payload byte array in a
     *       {@link ByteBuffer} for easy parsing.</li>
     *   <li><b>Wrapped Key Length Extraction:</b> Extracts the length of the wrapped
     *       key from the first 4 bytes of the payload using {@link ByteBuffer#getInt()}.</li>
     *   <li><b>Wrapped Key Length Validation:</b> Checks if the extracted wrapped key
     *       length is valid (greater than 0 and not exceeding the payload length). If
     *       it is not, throws an {@link Exception}.</li>
     *   <li><b>Wrapped Key Extraction:</b> Extracts the wrapped key from the payload
     *       using {@link ByteBuffer#get(byte[])}.</li>
     *   <li><b>Encrypted Data Extraction:</b> Extracts the encrypted data (IV +
     *       ciphertext) from the remaining bytes of the payload using
     *       {@link ByteBuffer#get(byte[])}.</li>
     *   <li><b>Per-Message Key Unwrapping:</b> Unwraps (decrypts) the per-message key
     *       using the master key and {@link #unwrapKey(byte[])}.</li>
     *   <li><b>Data Decryption:</b> Decrypts the encrypted data using the unwrapped
     *       per-message key and {@link #decryptData(SecretKey, byte[])}.</li>
     *   <li><b>Message Reconstruction:</b> Converts the decrypted data (byte array)
     *       back into a string using {@link String#String(byte[], java.nio.charset.Charset)}
     *       with {@link StandardCharsets#UTF_8}.</li>
     * </ol>
     * <p>
     * <b>Input Payload Format (Base64-Encoded):</b>
     * </p>
     * <pre>
     * [4-byte length of wrapped key][wrapped key][encrypted data (IV + ciphertext)]
     * </pre>
     * <p>
     * <b>Output:</b> The method returns the decrypted message as a string.
     * </p>
     *
     * @param base64Payload The Base64-encoded payload to decrypt. Must not be null.
     * @return The decrypted message as a string.
     * @throws Exception If there is an error during the decryption process, if the payload is null, or if the wrapped key length is invalid.
     */
    public static String decryptMessageHybrid(String base64Payload) throws Exception {
        if (base64Payload == null) {
            throw new Exception("Input payload is null.");
        }
        byte[] payload = Base64.decode(base64Payload, Base64.DEFAULT);
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int wrappedKeyLength = buffer.getInt();
        if (wrappedKeyLength <= 0 || wrappedKeyLength > payload.length) {
            throw new Exception("Invalid wrapped key length: " + wrappedKeyLength);
        }
        byte[] wrappedKey = new byte[wrappedKeyLength];
        buffer.get(wrappedKey);
        byte[] encryptedData = new byte[buffer.remaining()];
        buffer.get(encryptedData);
        SecretKey perMessageKey = unwrapKey(wrappedKey);
        byte[] decryptedData = decryptData(perMessageKey, encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}