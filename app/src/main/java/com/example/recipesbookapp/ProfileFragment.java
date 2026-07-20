package com.example.recipesbookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.recipesbookapp.adapter.RecipeAdapter;
import com.example.recipesbookapp.auth.LoginActivity;
import com.example.recipesbookapp.databinding.FragmentHomeBinding;
import com.example.recipesbookapp.databinding.FragmentProfileBinding;
import com.example.recipesbookapp.modales.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private RecipeAdapter adapter;
    private List<Recipe> myRecipesList;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
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
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        myRecipesList = new ArrayList<>();
        adapter = new RecipeAdapter();
        binding.myRecipesRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.myRecipesRv.setAdapter(adapter);

        loadUserData();
        loadMyRecipes();

        binding.btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return binding.getRoot();
    }


    private void loadUserData() {

        firestore.collection("users").document(Utils.USER_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        binding.userNameTv.setText(name);
                        binding.userEmailTv.setText(email);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Picasso.get().load(imageUrl).placeholder(R.drawable.camera_icon).into(binding.userProfileIv);
                        }
                    }
                });
    }

    private void loadMyRecipes() {
        firestore.collection("recipes")
                .whereEqualTo("userId", Utils.USER_ID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isAdded() && queryDocumentSnapshots != null) {
                        myRecipesList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                myRecipesList.add(recipe);
                            }
                        }
                        adapter.setRecipes(myRecipesList);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}