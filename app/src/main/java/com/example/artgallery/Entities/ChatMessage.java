package com.example.artgallery.Entities;

public class ChatMessage {
    public String sender;
    public String encryptedMessage; // This now holds the Base64 string of an image with embedded text.
    public long timestamp;

    // Empty constructor for Firebase
    public ChatMessage() {}

    public ChatMessage(String sender, String encryptedMessage, long timestamp) {
        this.sender = sender;
        this.encryptedMessage = encryptedMessage;
        this.timestamp = timestamp;
    }
}
