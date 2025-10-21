package com.itemfinder.midtermappdev.Find;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itemfinder.midtermappdev.R;
import com.itemfinder.midtermappdev.utils.NotificationManager; // ✅ Added import
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class Processclaim extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    // Claimer inputs
    private EditText claimerNameInput, claimerIdInput, claimerDescriptionInput;

    // Finder details
    private TextView finderContact, claimLocation;

    // Buttons and image views
    private Button btnClaim;
    private ImageView backButton, proof1, proof2, proof3;

    // For storing selected images
    private Uri[] selectedImages = new Uri[3];
    private int currentImageIndex = -1;

    // ✅ Firebase User ID
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.process_claim);

        // ✅ Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // 🔙 Back button
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // 🧾 Inputs
        claimerNameInput = findViewById(R.id.claimerNameInput);
        claimerIdInput = findViewById(R.id.claimerIdInput);
        claimerDescriptionInput = findViewById(R.id.claimerDescriptionInput);

        // 👤 Finder info
        finderContact = findViewById(R.id.finderContact);
        claimLocation = findViewById(R.id.claimLocation);

        // ✅ Load item details from Intent
        Intent intent = getIntent();
        String itemName = intent.getStringExtra("itemName");
        String itemCategory = intent.getStringExtra("itemCategory");
        String itemLocation = intent.getStringExtra("itemLocation");
        String itemDate = intent.getStringExtra("itemDate");
        String itemStatus = intent.getStringExtra("itemStatus");
        String itemImageUrl = intent.getStringExtra("itemImageUrl");

        // 🟦 Handle finder anonymity
        boolean isAnonymous = intent.getBooleanExtra("isAnonymous", false);
        String finderSchoolId = intent.getStringExtra("finderContact");
        String claimLocationValue = intent.getStringExtra("claimLocation");

        if (isAnonymous) {
            finderContact.setText("Finder chose to remain anonymous.");
        } else {
            finderContact.setText(finderSchoolId != null ? finderSchoolId : "Not available");
        }

        claimLocation.setText(claimLocationValue != null ? claimLocationValue : "Will be provided by admin");

        // ✅ Access included item card
        View itemCardView = findViewById(R.id.item_card_include);
        if (itemCardView != null) {
            TextView tvItemName = itemCardView.findViewById(R.id.tvItemName);
            TextView tvCategory = itemCardView.findViewById(R.id.tvCategory);
            TextView tvLocation = itemCardView.findViewById(R.id.tvLocation);
            TextView tvDate = itemCardView.findViewById(R.id.tvDate);
            TextView tvStatus = itemCardView.findViewById(R.id.tvStatus);
            ImageView ivItemImage = itemCardView.findViewById(R.id.ivItemImage);

            if (tvItemName != null) tvItemName.setText(itemName);
            if (tvCategory != null) tvCategory.setText(itemCategory);
            if (tvLocation != null) tvLocation.setText(itemLocation);
            if (tvDate != null) tvDate.setText("Date Found: " + itemDate);
            if (tvStatus != null) tvStatus.setText(itemStatus);

            if (itemImageUrl != null && !itemImageUrl.isEmpty()) {
                Picasso.get()
                        .load(itemImageUrl)
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_error_image)
                        .fit()
                        .centerCrop()
                        .into(ivItemImage);
            }
        }

        // 🖼 Proof image views
        proof1 = findViewById(R.id.proof1);
        proof2 = findViewById(R.id.proof2);
        proof3 = findViewById(R.id.proof3);

        proof1.setOnClickListener(v -> openImagePicker(0));
        proof2.setOnClickListener(v -> openImagePicker(1));
        proof3.setOnClickListener(v -> openImagePicker(2));

        // ✅ Initialize Firebase user
        initializeUser();

        // ✅ Claim button
        btnClaim = findViewById(R.id.btnClaim);
        btnClaim.setOnClickListener(v -> handleClaim(itemName));
    }

    // ✅ Initialize Firebase User
    private void initializeUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    private void openImagePicker(int index) {
        currentImageIndex = index;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImages[currentImageIndex] = imageUri;

            switch (currentImageIndex) {
                case 0: proof1.setImageURI(imageUri); break;
                case 1: proof2.setImageURI(imageUri); break;
                case 2: proof3.setImageURI(imageUri); break;
            }
        }
    }

    // ✅ UPDATED CLAIM HANDLER
    private void handleClaim(String itemName) {
        String claimerName = claimerNameInput.getText().toString().trim();
        String claimerId = claimerIdInput.getText().toString().trim();
        String description = claimerDescriptionInput.getText().toString().trim();

        if (claimerName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (claimerId.isEmpty()) {
            Toast.makeText(this, "Please enter your Claimer ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasProof = false;
        for (Uri uri : selectedImages) {
            if (uri != null) {
                hasProof = true;
                break;
            }
        }

        if (!hasProof) {
            Toast.makeText(this, "Please upload at least one proof photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get item details from intent
        Intent intent = getIntent();
        String itemId = intent.getStringExtra("itemId");
        String itemCategory = intent.getStringExtra("itemCategory");
        String itemLocation = intent.getStringExtra("itemLocation");
        String itemDate = intent.getStringExtra("itemDate");
        String itemImageUrl = intent.getStringExtra("itemImageUrl");

        // Prepare claim data
        Map<String, Object> claimData = new HashMap<>();
        claimData.put("itemId", itemId);
        claimData.put("itemName", itemName);
        claimData.put("itemCategory", itemCategory);
        claimData.put("itemLocation", itemLocation);
        claimData.put("itemDate", itemDate);
        claimData.put("itemImageUrl", itemImageUrl);
        claimData.put("claimantId", currentUserId != null ? currentUserId : claimerId);
        claimData.put("claimantName", claimerName);
        claimData.put("claimantEmail", ""); // optional
        claimData.put("claimantPhone", ""); // optional
        claimData.put("description", description);
        claimData.put("status", "Pending");
        claimData.put("claimDate", System.currentTimeMillis());

        // Convert URIs to string list (for now, store local URIs)
        List<String> proofUrls = new ArrayList<>();
        for (Uri uri : selectedImages) {
            if (uri != null) {
                proofUrls.add(uri.toString());
            }
        }
        claimData.put("proofImages", proofUrls);

        // Submit to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("claims")
                .add(claimData)
                .addOnSuccessListener(documentReference -> {
                    String claimDocId = documentReference.getId();
                    Toast.makeText(this, "Claim submitted successfully!", Toast.LENGTH_LONG).show();

                    // ✅ Trigger notification
                    NotificationManager.getInstance()
                            .notifyClaimSubmitted(itemName, claimDocId);

                    // ✅ Navigate to notification screen
                    Intent notifIntent = new Intent(Processclaim.this, NotifActivity.class);
                    notifIntent.putExtra("itemName", itemName);
                    notifIntent.putExtra("claimerName", claimerName);
                    notifIntent.putExtra("claimerId", claimerId);
                    notifIntent.putExtra("status", "Pending");
                    startActivity(notifIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit claim: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
