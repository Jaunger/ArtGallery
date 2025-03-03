package com.example.artgallery.Adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.artgallery.Entities.ChatMessage;
import com.example.artgallery.R;
import com.example.artgallery.Utils.SteganographyUtil;
import com.example.artgallery.Utils.Utils;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ArrayList<ChatMessage> messages;
    private boolean decryptMode = false;
    private String currentUsername;

    public ChatAdapter(ArrayList<ChatMessage> messages, String currentUsername) {
        this.messages = messages;
        this.currentUsername = currentUsername;
    }

    public void setDecryptMode(boolean mode) {
        if(decryptMode == mode)
            return;
        decryptMode = mode;
        notifyDataSetChanged();
    }

    public boolean checkDecryptionMode() {
        return decryptMode;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.textViewSender.setText(message.sender);

        long now = System.currentTimeMillis();
        long diff = now - message.timestamp;

        if (diff < 60 * 1000L) {
            // Less than one minute: show "now"
            holder.textViewTimestamp.setText("now");
        } else {
            java.util.Calendar msgCal = java.util.Calendar.getInstance();
            msgCal.setTimeInMillis(message.timestamp);
            java.util.Calendar nowCal = java.util.Calendar.getInstance();

            boolean isToday = (msgCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR))
                    && (msgCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR));

            if (isToday) {
                // If message is from today, display "Today at hh:mm a"
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("'Today at' hh:mm a", java.util.Locale.getDefault());
                holder.textViewTimestamp.setText(sdf.format(new java.util.Date(message.timestamp)));
            } else {
                // Otherwise, display full date.
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
                holder.textViewTimestamp.setText(sdf.format(new java.util.Date(message.timestamp)));
            }
        }

        // Convert the stored Base64 string into a Bitmap.
        Bitmap encodedBitmap = Utils.stringToBitMap(message.encryptedMessage);

        if (!decryptMode) {
            // Fade in the encoded image.
            holder.imageViewArt.setAlpha(0f);
            holder.imageViewArt.setImageBitmap(encodedBitmap);
            holder.imageViewArt.setVisibility(View.VISIBLE);
            holder.imageViewArt.animate().alpha(1f).setDuration(300).start();

            // Fade out the decrypted text.
            holder.textViewDecrypted.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> holder.textViewDecrypted.setVisibility(View.GONE))
                    .start();
        } else {
            // Fade out the image.
            holder.imageViewArt.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> holder.imageViewArt.setVisibility(View.GONE))
                    .start();

            // Extract payload and decrypt.
            String extractedPayload = SteganographyUtil.decodeMessage(encodedBitmap);
            try {
                String decryptedText = Utils.decryptMessageHybrid(extractedPayload);
                holder.textViewDecrypted.setText(decryptedText);
            } catch (Exception e) {
                e.printStackTrace();
                holder.textViewDecrypted.setText("Decryption error");
            }
            // Fade in the decrypted text.
            holder.textViewDecrypted.setAlpha(0f);
            holder.textViewDecrypted.setVisibility(View.VISIBLE);
            holder.textViewDecrypted.animate().alpha(1f).setDuration(300).start();
        }

        // Adjust the CardView's gravity based on the sender.
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.cardViewMessage.getLayoutParams();
        if (message.sender.equals(currentUsername)) {
            params.gravity = android.view.Gravity.END;
            holder.containerMessage.setBackgroundResource(R.drawable.chat_bubble_sent);
            holder.textViewSender.setVisibility(View.GONE);
        } else {
            params.gravity = android.view.Gravity.START;
            holder.containerMessage.setBackgroundResource(R.drawable.chat_bubble_received);
            holder.textViewSender.setVisibility(View.VISIBLE);
        }
        holder.cardViewMessage.setLayoutParams(params);
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSender, textViewDecrypted, textViewTimestamp;
        ImageView imageViewArt;
        LinearLayout containerMessage;
        FrameLayout cardViewMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewDecrypted = itemView.findViewById(R.id.textViewDecrypted);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            imageViewArt = itemView.findViewById(R.id.imageViewArt);
            containerMessage = itemView.findViewById(R.id.containerMessage);
            cardViewMessage = itemView.findViewById(R.id.cardViewMessage);
        }
    }
}