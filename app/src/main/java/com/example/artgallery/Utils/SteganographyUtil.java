package com.example.artgallery.Utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.charset.StandardCharsets;

public class SteganographyUtil {

    /**
     * Encodes a hidden message within a {@link Bitmap} image using the least
     * significant bit (LSB) steganography technique.
     * <p>
     * This method takes a {@link Bitmap} image and a message string as input. It
     * encodes the message within the image by modifying the least significant bit
     * of the blue color component of each pixel. The message is encoded as a
     * sequence of bits, preceded by a 32-bit integer representing the length of
     * the message.
     * </p>
     * <p>
     * The encoding process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Bitmap Preparation:</b> Creates a mutable copy of the input
     *       {@link Bitmap} with {@link Bitmap.Config#ARGB_8888} configuration using
     *       {@link Bitmap#copy(Bitmap.Config, boolean)}.</li>
     *   <li><b>Message Conversion:</b> Converts the message string into a byte array
     *       using {@link String#getBytes(java.nio.charset.Charset)} with
     *       {@link StandardCharsets#UTF_8}.</li>
     *   <li><b>Capacity Check:</b> Calculates the total number of bits required to
     *       store the message length (32 bits) and the message itself (message length
     *       * 8 bits). Checks if the image has enough pixels to store the message.
     *       If not, throws an {@link IllegalArgumentException}.</li>
     *   <li><b>Bit Array Creation:</b> Creates an integer array to hold the bits
     *       representing the message length and the message itself.</li>
     *   <li><b>Message Length Encoding:</b> Encodes the message length as a 32-bit
     *       integer into the first 32 elements of the bit array.</li>
     *   <li><b>Message Encoding:</b> Encodes each byte of the message into 8 bits
     *       and stores them in the bit array after the message length.</li>
     *   <li><b>Pixel Modification:</b> Iterates through each pixel of the image,
     *       modifying the least significant bit of the blue color component to match
     *       the corresponding bit in the bit array.
     *     <ul>
     *       <li>Retrieves the pixel color using {@link Bitmap#getPixel(int, int)}.</li>
     *       <li>Extracts the blue color component using {@link Color#blue(int)}.</li>
     *       <li>Modifies the least significant bit of the blue component.</li>
     *       <li>Creates a new pixel color using {@link Color#argb(int, int, int, int)}.</li>
     *       <li>Sets the new pixel color using {@link Bitmap#setPixel(int, int, int)}.</li>
     *     </ul>
     *   </li>
     * </ol>
     * <p>
     * <b>Message Capacity:</b> The maximum message length that can be encoded depends
     * on the dimensions of the image. Each pixel can store one bit of information,
     * so the total number of bits that can be stored is equal to the width * height
     * of the image. The first 32 bits are used to store the message length, so the
     * remaining bits are available for the message itself.
     * </p>
     * <p>
     * <b>Note:</b> This method modifies the input {@link Bitmap} by changing the
     * least significant bit of the blue color component of each pixel.
     * </p>
     *
     * @param bmp     The {@link Bitmap} image to encode the message into.
     * @param message The message string to encode.
     * @return A new {@link Bitmap} image with the message encoded within it.
     * @throws IllegalArgumentException If the image is not large enough to hold the message.
     */
    public static Bitmap encodeMessage(Bitmap bmp, String message) {
        Bitmap mutableBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        int width = mutableBmp.getWidth();
        int height = mutableBmp.getHeight();

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        int messageLength = messageBytes.length;
        int totalBits = 32 + messageLength * 8;
        if (totalBits > width * height) {
            throw new IllegalArgumentException("Image not large enough to hold the message");
        }

        int[] bits = new int[totalBits];

        // Store message length (32 bits)
        for (int i = 0; i < 32; i++) {
            bits[i] = (messageLength >> (31 - i)) & 1;
        }
        // Store message bits
        for (int i = 0; i < messageBytes.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                bits[32 + i * 8 + bit] = (messageBytes[i] >> (7 - bit)) & 1;
            }
        }

        int bitIndex = 0;
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex < totalBits) {
                    int pixel = mutableBmp.getPixel(x, y);
                    int blue = Color.blue(pixel);
                    blue = (blue & 0xFE) | bits[bitIndex];
                    int newPixel = Color.argb(Color.alpha(pixel),
                            Color.red(pixel),
                            Color.green(pixel),
                            blue);
                    mutableBmp.setPixel(x, y, newPixel);
                    bitIndex++;
                } else {
                    break outer;
                }
            }
        }
        return mutableBmp;
    }

    /**
     * Decodes a hidden message from a {@link Bitmap} image that was encoded using
     * the least significant bit (LSB) steganography technique implemented by
     * {@link #encodeMessage(Bitmap, String)}.
     * <p>
     * This method extracts a message that was previously hidden within a
     * {@link Bitmap} image by modifying the least significant bit of the blue
     * color component of each pixel. The message is encoded as a sequence of
     * bits, preceded by a 32-bit integer representing the length of the message.
     * </p>
     * <p>
     * The decoding process involves the following steps:
     * </p>
     * <ol>
     *   <li><b>Message Length Extraction:</b> Reads the first 32 bits from the
     *       image to determine the length of the hidden message.
     *     <ul>
     *       <li>Iterates through the pixels of the image.</li>
     *       <li>Retrieves the pixel color using {@link Bitmap#getPixel(int, int)}.</li>
     *       <li>Extracts the blue color component using {@link Color#blue(int)}.</li>
     *       <li>Extracts the least significant bit of the blue component.</li>
     *       <li>Shifts the current message length left by 1 bit and adds the
     *           extracted bit.</li>
     *     </ul>
     *   </li>
     *   <li><b>Message Length Validation:</b> Checks if the extracted message length
     *       is within a reasonable range (greater than 0 and not exceeding
     *       {@link Integer#MAX_VALUE}). If not, throws an
     *       {@link IllegalArgumentException}.</li>
     *   <li><b>Message Bit Extraction:</b> Reads the remaining bits from the image,
     *       based on the extracted message length, to reconstruct the message bytes.
     *     <ul>
     *       <li>Iterates through the pixels of the image, starting from the 33rd bit.</li>
     *       <li>Retrieves the pixel color using {@link Bitmap#getPixel(int, int)}.</li>
     *       <li>Extracts the blue color component using {@link Color#blue(int)}.</li>
     *       <li>Extracts the least significant bit of the blue component.</li>
     *       <li>Collects 8 bits to form a byte.</li>
     *       <li>Stores the reconstructed byte in the message byte array.</li>
     *     </ul>
     *   </li>
     *   <li><b>Message Reconstruction:</b> Converts the extracted message bytes into
     *       a string using {@link String#String(byte[], java.nio.charset.Charset)}
     *       with {@link StandardCharsets#UTF_8}.</li>
     * </ol>
     * <p>
     * <b>Note:</b> This method assumes that the input {@link Bitmap} image has a
     * message encoded within it using the {@link #encodeMessage(Bitmap, String)}
     * method.
     * </p>
     *
     * @param bmp The {@link Bitmap} image to decode the message from.
     * @return The decoded message as a string.
     * @throws IllegalArgumentException If the extracted message length is invalid.
     */
    public static String decodeMessage(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        long messageLength = 0L;  // Use long to treat 32 bits as unsigned.
        int bitIndex = 0;
        // Read the first 32 bits for message length.
        for (int y = 0; y < height && bitIndex < 32; y++) {
            for (int x = 0; x < width && bitIndex < 32; x++) {
                int pixel = bmp.getPixel(x, y);
                int blue = Color.blue(pixel);
                messageLength = (messageLength << 1) | (blue & 1);
                bitIndex++;
            }
        }
        // Check that the length is within a reasonable range.
        if (messageLength > Integer.MAX_VALUE || messageLength <= 0) {
            throw new IllegalArgumentException("Invalid message length extracted: " + messageLength);
        }
        int msgLen = (int) messageLength;
        int totalMessageBits = msgLen * 8;
        byte[] messageBytes = new byte[msgLen];
        int currentByte = 0, bitsCollected = 0;
        for (int i = 32; i < 32 + totalMessageBits; i++) {
            int x = i % width;
            int y = i / width;
            if (y >= height) break;
            int pixel = bmp.getPixel(x, y);
            int blue = Color.blue(pixel);
            currentByte = (currentByte << 1) | (blue & 1);
            bitsCollected++;
            if (bitsCollected == 8) {
                messageBytes[(i - 32) / 8] = (byte) currentByte;
                bitsCollected = 0;
                currentByte = 0;
            }
        }
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
}