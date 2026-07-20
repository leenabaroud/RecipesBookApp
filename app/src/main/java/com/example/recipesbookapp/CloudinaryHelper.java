package com.example.recipesbookapp;


import android.content.Context;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "hq9prp9u");
            config.put("api_key", "289751215631727");
            config.put("api_secret", "CaSVgC1YsY3y5Gb8YpVuC26b8-Y");
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }
}