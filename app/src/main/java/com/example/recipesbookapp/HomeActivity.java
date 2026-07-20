package com.example.recipesbookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.recipesbookapp.databinding.ActivityHomeBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Utils.USER_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();

        binding.fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.home) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
                } else if (id == R.id.profile) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new ProfileFragment())
                            .commit();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }
}