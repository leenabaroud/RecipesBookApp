package com.example.recipesbookapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.recipesbookapp.AddRecipeActivity;
import com.example.recipesbookapp.R;
import com.example.recipesbookapp.RecipeDetailsActivity;
import com.example.recipesbookapp.Utils;
import com.example.recipesbookapp.databinding.ItemRecipeBinding;
import com.example.recipesbookapp.modales.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> allRecipes = new ArrayList<>();
    private List<Recipe> filteredRecipes = new ArrayList<>();


    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipeBinding binding = ItemRecipeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RecipeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = filteredRecipes.get(position);

        // if user is owner the recipe to show edite and delete
        if (recipe.getUserId().equals(Utils.USER_ID)) {
            holder.binding.ownerActionsLayout.setVisibility(View.VISIBLE);

            holder.binding.btnEditRecipe.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, AddRecipeActivity.class);

                intent.putExtra("recipe_id", recipe.getRecipeId());
                intent.putExtra("title", recipe.getTitle());
                intent.putExtra("description", recipe.getDescription());
                intent.putExtra("ingredients", recipe.getIngredients());
                intent.putExtra("steps", recipe.getSteps());
                intent.putExtra("video_url", recipe.getVideo());
                intent.putExtra("image_url", recipe.getImageUrl());
                intent.putExtra("category", recipe.getCategory());
                intent.putExtra("screen", "homeScreen");

                context.startActivity(intent);
            });

            // to delete recipe and show dialog
            holder.binding.btnDeleteRecipe.setOnClickListener(v -> {
                Context context = v.getContext();

                new android.app.AlertDialog.Builder(context)
                        .setTitle("Delete Recipe")
                        .setMessage("Are you sure you want to delete this recipe?")
                        .setPositiveButton("Yes", (dialog, which) -> {

                            int currentPos = holder.getAdapterPosition();
                            if (currentPos == RecyclerView.NO_POSITION) return;

                            FirebaseFirestore.getInstance()
                                    .collection("recipes")
                                    .document(recipe.getRecipeId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        android.widget.Toast.makeText(context, "Recipe deleted successfully", android.widget.Toast.LENGTH_SHORT).show();

                                        Recipe removedRecipe = filteredRecipes.remove(currentPos);
                                        allRecipes.remove(removedRecipe);
                                        notifyDataSetChanged();

                                    })
                                    .addOnFailureListener(e -> {
                                        android.widget.Toast.makeText(context, "Failed to delete recipe", android.widget.Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        } else {
            holder.binding.ownerActionsLayout.setVisibility(View.GONE);
        }

        holder.binding.itemRecipeTitleTv.setText(recipe.getTitle());
        holder.binding.itemRecipeCategoryTv.setText(recipe.getCategory());
        holder.binding.itemRecipeDescTv.setText(recipe.getDescription());

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.camera_icon)
                    .error(R.drawable.camera_icon)
                    .centerCrop()
                    .into(holder.binding.itemRecipeIv);
        } else {
            holder.binding.itemRecipeIv.setImageResource(R.drawable.camera_icon);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, RecipeDetailsActivity.class);
            intent.putExtra("recipe_id", recipe.getRecipeId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredRecipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ItemRecipeBinding binding;

        public RecipeViewHolder(@NonNull ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }


    }

    public void setRecipes(List<Recipe> recipes) {
        this.allRecipes = recipes;
        this.filteredRecipes = new ArrayList<>(recipes);
        notifyDataSetChanged();
    }

    public void filter(String category, String query) {
        List<Recipe> recipesList = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
            boolean matchesCategory = category.equalsIgnoreCase("All") || recipe.getCategory().equalsIgnoreCase(category);
            boolean matchesSearch = query.isEmpty() || recipe.getTitle().toLowerCase().contains(query.toLowerCase());

            if (matchesCategory && matchesSearch) {
                recipesList.add(recipe);
            }
        }
        filteredRecipes = recipesList;
        notifyDataSetChanged();
    }
}