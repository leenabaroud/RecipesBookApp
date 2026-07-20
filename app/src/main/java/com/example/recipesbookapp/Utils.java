package com.example.recipesbookapp;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;


public class Utils {
    public static String USER_ID = "";
    // convert the bitmap to byte
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

}
