package com.example.matmulskeyboard;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;

public class TextExtractorFromImage {

    private static final String DATA_PATH = "matmuls_tesseract" ;
    private final TessBaseAPI mTess;

    public TextExtractorFromImage(Context context, String language) {
        mTess = new TessBaseAPI();
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "/tessdata" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    Log.d("LOGGER", "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.d("LOGGER", "Czsreated directory " + path + " on sdcard");
                }
            }

        }


        if (!(new File(DATA_PATH + "/tessdata/" + language + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = context.getAssets();
                InputStream in = assetManager.open(language + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "/tessdata/" + language + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                mTess.init(DATA_PATH + "/tessdata/" + language + ".traineddata",language);
                Log.d(TAG, "Copied and initialized" + language + " traineddata");
            } catch (IOException e) {
                Log.d(TAG, "Was unable to copy and initialize " + language + " traineddata " + e.toString());
            }
        }

    }

    public String getOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        return mTess.getUTF8Text();
    }
    public String getFristLineOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        return mTess.getUTF8Text().substring(0,15);
    }
    public void onDestroy() {
        if (mTess != null) mTess.end();
    }
}