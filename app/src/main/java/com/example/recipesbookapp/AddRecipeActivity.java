package com.example.recipesbookapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.recipesbookapp.auth.LoginActivity;
import com.example.recipesbookapp.databinding.ActivityAddRecipeBinding;
import com.example.recipesbookapp.modales.Recipe;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddRecipeActivity extends AppCompatActivity {
    ActivityAddRecipeBinding binding;
    FirebaseFirestore firestore;
    boolean isEditMode = false;
    String recipeId = null;
    Uri imageUri = null;
    Bitmap bitmap = null;
    String uploadedImageUrl = "";String screen ;
    ArrayAdapter<CharSequence> spinnerAdapter;

     final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        binding.recipeImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        CloudinaryHelper.init(this);

        // list of category
        String[] categoriesArray = getResources().getStringArray(R.array.recipe_categories);

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesArray);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.recipeCategorySpinner.setAdapter(spinnerAdapter);


        binding.selectImageBt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Recipe Image"));
        });

        if (getIntent().hasExtra("recipe_id")) {
            isEditMode = true;
            recipeId = getIntent().getStringExtra("recipe_id");
            // update the screen data to be edite
            binding.screenTitleTv.setText("Edit Recipe");
            binding.submitRecipeBt.setText("Update Recipe");
            // get date the recipe to show in screen
            binding.recipeTitleEt.setText(getIntent().getStringExtra("title"));
            binding.recipeDescriptionEt.setText(getIntent().getStringExtra("description"));
            binding.recipeIngredientsEt.setText(getIntent().getStringExtra("ingredients"));
            binding.recipeStepsEt.setText(getIntent().getStringExtra("steps"));
            binding.recipeVideoUrlEt.setText(getIntent().getStringExtra("video_url"));
            uploadedImageUrl = getIntent().getStringExtra("image_url");
            screen = getIntent().getStringExtra("screen");

            if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(uploadedImageUrl)
                        .placeholder(R.drawable.camera_icon)
                        .error(R.drawable.camera_icon)
                        .centerCrop()
                        .into(binding.recipeImageView);
            }
            // to show the category  recipe
            String currentCategory = getIntent().getStringExtra("category");
            if (currentCategory != null) {
                int spinnerPosition = spinnerAdapter.getPosition(currentCategory);
                if (spinnerPosition >= 0) {
                    binding.recipeCategorySpinner.setSelection(spinnerPosition);
                }
            }
        }

        binding.submitRecipeBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if image null
                if (bitmap == null && !isEditMode) {
                    Toast.makeText(AddRecipeActivity.this, "Please select recipe image", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (bitmap != null) {
                    uploadImageAndSaveRecipe();
                } else {
                    saveRecipe();
                }
            }
        });
    }

    // to save image im cloud
    private void uploadImageAndSaveRecipe() {
        Toast.makeText(this, "Uploading image please wait...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(Utils.bitmapToByteArray(bitmap))
                .option("folder", "recipes_uploads")
                .option("public_id", "recipe_" + System.currentTimeMillis())
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        uploadedImageUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> {
                            Toast.makeText(AddRecipeActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            saveRecipe();
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("CloudinaryUpload", "onError: " + error.getDescription());
                        runOnUiThread(() ->
                                Toast.makeText(AddRecipeActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    // save recipe in firebase
    private void saveRecipe() {
        String title = binding.recipeTitleEt.getText().toString();
        String description = binding.recipeDescriptionEt.getText().toString();
        String ingredients = binding.recipeIngredientsEt.getText().toString();
        String steps = binding.recipeStepsEt.getText().toString();
        String video = binding.recipeVideoUrlEt.getText().toString();
        String category = binding.recipeCategorySpinner.getSelectedItem().toString();

        if (title.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }


        if (!isEditMode || recipeId == null || recipeId.isEmpty()) {
            recipeId = UUID.randomUUID().toString();
        }

        Recipe recipe = new Recipe(recipeId, Utils.USER_ID, title, ingredients, steps, category, description, video, uploadedImageUrl);

        firestore.collection("recipes").document(recipeId)
                .set(recipe)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            String message = isEditMode ? "Recipe updated successfully!" : "Recipe added successfully!";
                            Toast.makeText(AddRecipeActivity.this, message, Toast.LENGTH_SHORT).show();
                            if(screen.equalsIgnoreCase("detailsScreen") ){
                                Intent intent = new Intent(AddRecipeActivity.this, RecipeDetailsActivity.class);
                                intent.putExtra("recipe_id", recipe.getRecipeId());
                                startActivity(intent);
                               finish();
                            }else{
                                Intent intent = new Intent(AddRecipeActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(AddRecipeActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}