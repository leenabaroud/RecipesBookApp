package com.example.recipesbookapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.recipesbookapp.databinding.ActivityRecipeDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class RecipeDetailsActivity extends AppCompatActivity {
    ActivityRecipeDetailsBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String recipeId;
    String videoUrl = "";

    String currentTitle = "";
    String currentDescription = "";
    String currentIngredients = "";
    String currentSteps = "";
    String currentImageUrl = "";
    String currentCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recipeId = getIntent().getStringExtra("recipe_id");

        if (recipeId != null) {
            loadRecipeDetails();
        } else {
            Toast.makeText(this, "Recipe details not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void loadRecipeDetails() {
        db.collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentTitle = documentSnapshot.getString("title");
                        currentCategory = documentSnapshot.getString("category");
                        currentDescription = documentSnapshot.getString("description");
                        currentIngredients = documentSnapshot.getString("ingredients");
                        currentSteps = documentSnapshot.getString("steps");
                        currentImageUrl = documentSnapshot.getString("imageUrl");
                        if (documentSnapshot.contains("video")) {
                            videoUrl = documentSnapshot.getString("video");
                        } else {
                            videoUrl = documentSnapshot.getString("videoUrl");
                        }
                        String recipeOwnerId = documentSnapshot.getString("userId");

                        binding.recipeTitleTv.setText(currentTitle);
                        binding.recipeCategoryTv.setText(currentCategory);
                        binding.recipeDescriptionTv.setText(currentDescription);

                        if (currentIngredients != null) {
                            binding.recipeIngredientsTv.setText("• " + currentIngredients.replace(",", "\n• "));
                        }
                        if (currentSteps != null) {
                            binding.recipeStepsTv.setText(currentSteps.replace(",", "\n"));
                        }

                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            Picasso.get().load(currentImageUrl).placeholder(android.R.drawable.ic_menu_gallery).into(binding.recipeImageView);
                        }

                        checkOwnership(recipeOwnerId);
                        setupVideoButton();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void checkOwnership(String ownerId) {
        if (Utils.USER_ID.equals(ownerId) && !Utils.USER_ID.isEmpty()) {
            binding.creatorActionsLayout.setVisibility(View.VISIBLE);

            binding.btnEditRecipe.setOnClickListener(v -> {
                Intent intent = new Intent(RecipeDetailsActivity.this, AddRecipeActivity.class);
                intent.putExtra("recipe_id", recipeId);
                intent.putExtra("title", currentTitle);
                intent.putExtra("description", currentDescription);
                intent.putExtra("ingredients", currentIngredients);
                intent.putExtra("steps", currentSteps);
                intent.putExtra("video_url", videoUrl);
                intent.putExtra("image_url", currentImageUrl);
                intent.putExtra("category", currentCategory);
                intent.putExtra("screen", "detailsScreen");
                startActivity(intent);
            });

            binding.btnDeleteRecipe.setOnClickListener(v -> {
                db.collection("recipes").document(recipeId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(RecipeDetailsActivity.this, "Recipe deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(RecipeDetailsActivity.this, "Failed to delete recipe", Toast.LENGTH_SHORT).show());

            });
        } else {
            binding.creatorActionsLayout.setVisibility(View.GONE);
        }
    }

    private void setupVideoButton() {
        binding.btnWatchVideo.setOnClickListener(v -> {
            if (videoUrl != null && !videoUrl.trim().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(intent);
            } else {
                Toast.makeText(RecipeDetailsActivity.this, "No video tutorial available for this recipe", Toast.LENGTH_SHORT).show();
            }
        });
    }


}