package com.example.recipesbookapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import com.example.recipesbookapp.adapter.RecipeAdapter;
import com.example.recipesbookapp.databinding.FragmentHomeBinding;
import com.example.recipesbookapp.modales.Recipe;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
     FragmentHomeBinding binding;
     FirebaseFirestore firestore;
     RecipeAdapter recipeAdapter;
     List<Recipe> recipeList;
     String selectedCategory = "All";
     String searchQuery = "";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        recipeList = new ArrayList<>();

        recipeAdapter = new RecipeAdapter();
        binding.recipesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recipesRecyclerView.setAdapter(recipeAdapter);

        categoriesTabs();

        loadRecipes();

        binding.categoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedCategory = tab.getText().toString();
                recipeAdapter.filter(selectedCategory, searchQuery);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query.trim();
                recipeAdapter.filter(selectedCategory, searchQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.trim();
                recipeAdapter.filter(selectedCategory, searchQuery);
                return true;
            }
        });

        return binding.getRoot();

    }

    private void categoriesTabs() {
        binding.categoryTabLayout.removeAllTabs();

        binding.categoryTabLayout.addTab(binding.categoryTabLayout.newTab().setText("All"));

        String[] categories = getResources().getStringArray(R.array.recipe_categories);
        for (String category : categories) {
            binding.categoryTabLayout.addTab(binding.categoryTabLayout.newTab().setText(category));
        }
    }
    private void loadRecipes() {
        firestore.collection("recipes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        recipeList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Recipe recipe = document.toObject(Recipe.class);
                            recipeList.add(recipe);
                        }
                        recipeAdapter.setRecipes(recipeList);
                        recipeAdapter.filter(selectedCategory, searchQuery);
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}