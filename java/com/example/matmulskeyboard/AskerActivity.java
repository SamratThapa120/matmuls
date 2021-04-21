package com.example.matmulskeyboard;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;

public class AskerActivity extends FragmentActivity {
    private MediaProjectionManager mgr;
    private static final int REQUEST_SCREENSHOT = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asker);
        mgr=(MediaProjectionManager)getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_SCREENSHOT){
            if(resultCode==RESULT_OK){
                Log.d("LOGGER","resultFine");
                Intent intent = new Intent(this,MyInputMethodService.class);
                intent.putExtra(MyInputMethodService.EXTRA_RESULT_INTENT,data);
                intent.putExtra(MyInputMethodService.EXTRA_RESULT_CODE,resultCode);
                startService(intent);
                this.finish();
            }
        }
        else{
            Log.d("LOGGER","NOREQUEST");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_SCREENSHOT);
    }
}