package com.example.recipesbookapp.auth;

import static com.example.recipesbookapp.Utils.bitmapToByteArray;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.recipesbookapp.CloudinaryHelper;
import com.example.recipesbookapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    FirebaseAuth auth;
    FirebaseFirestore fireStore;
    FirebaseStorage storage;
    Uri uri;
    String imageUrl = null;
    Bitmap bitmap;

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult o) {
                    if (o.getResultCode() == RESULT_OK && o.getData() != null) {
                        uri = o.getData().getData();
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            binding.profileImageView.setImageBitmap(bitmap);

                            MediaManager.get().upload(bitmapToByteArray(bitmap))
                                    .option("folder", "uploads")
                                    .option("public_id", "profile_" + System.currentTimeMillis())
                                    .callback(new UploadCallback() {

                                        @Override
                                        public void onStart(String requestId) {
                                            runOnUiThread(() ->
                                                    Toast.makeText(RegisterActivity.this, "Uploading image please wait...", Toast.LENGTH_SHORT).show()
                                            );
                                        }

                                        @Override
                                        public void onProgress(String requestId, long bytes, long totalBytes) {
                                        }

                                        @Override
                                        public void onSuccess(String requestId, Map resultData) {
                                            imageUrl = (String) resultData.get("secure_url");
                                            runOnUiThread(() -> {
                                                Toast.makeText(RegisterActivity.this, "Upload success!", Toast.LENGTH_SHORT).show();
                                                Picasso.get().load(imageUrl).into(binding.profileImageView);
                                            });
                                        }

                                        @Override
                                        public void onError(String requestId, ErrorInfo error) {
                                            Log.e("CloudinaryUpload", "onError: " + error.getCode() + " --> " + error.getDescription());
                                            runOnUiThread(() ->
                                                    Toast.makeText(RegisterActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show()
                                            );
                                        }

                                        @Override
                                        public void onReschedule(String requestId, ErrorInfo error) {
                                        }
                                    })
                                    .dispatch();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(RegisterActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CloudinaryHelper.init(this);

        auth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ArrayList<String> countriesList = new ArrayList<>();
        countriesList.add("Select Country");

        ArrayList<String> tempCountries = new ArrayList<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String countryName = locale.getDisplayCountry();
            if (!countryName.isEmpty() && !tempCountries.contains(countryName)) {
                tempCountries.add(countryName);
            }
        }

        Collections.sort(tempCountries);
        countriesList.addAll(tempCountries);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                countriesList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.countrySpinner.setAdapter(adapter);


        binding.registerBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.emailEt.getText().toString().trim();
                String name = binding.NameEt.getText().toString().trim();
                String password = binding.passwordEt.getText().toString().trim();
                String comfortPassword = binding.confirmPasswordEt.getText().toString().trim();
                String country = binding.countrySpinner.getSelectedItem().toString();

                if (email.isEmpty() || name.isEmpty() || password.isEmpty() || comfortPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (country.equals("Select Country")) {
                    Toast.makeText(RegisterActivity.this, "Please select your country", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (imageUrl == null) {
                    Toast.makeText(RegisterActivity.this, "Please wait until the profile image is uploaded", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!comfortPassword.equals(password)) {
                    Toast.makeText(RegisterActivity.this, "Password and confirm password do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("email", email);
                                hashMap.put("name", name);
                                hashMap.put("country", country);
                                hashMap.put("imageUrl", imageUrl);

                                fireStore.collection("users").document(user.getUid()).set(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> firebaseTask) {

                                        if (firebaseTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            if (firebaseTask.getException() != null) {
                                                Log.e("FirestoreError", "Error: " + firebaseTask.getException().getMessage());
                                            }
                                            Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        binding.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                launcher.launch(intent);
            }
        });

        binding.loginTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });
    }

}