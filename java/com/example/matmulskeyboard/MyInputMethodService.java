package com.example.matmulskeyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private TextView predictionView;
    private boolean caps = false;
    private Timer screenshotTimer;
    private int SCREENSHOT_TIMER = 5000;

    private class Predictor extends TimerTask{
        private Context appContext;
        private TextView displayView;
        private ImageReader mImageReader;
        private MediaProjection mVirtualDisplay;
        private int mWidth=100,mHeight=200;
        public Predictor(Context appCont,TextView textView){
            appContext = appCont;
            displayView = textView;

        }
        private void takeScreenshotAndProcess(){
            Log.v("LOG","screenshot");
            View v1 = getWindow().getWindow().getDecorView().getRootView();
            if (v1==null){
                Log.v("LOG","Null decor");
                return;
            }
            Log.v("LOG","Not Null");
            String mPath = "/storage/self/primary/DCIM/Camera/" + UUID.randomUUID().toString() + ".jpg";
            v1.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Bitmap b = Bitmap.createBitmap(v1.getMeasuredWidth(), v1.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            v1.layout(0, 0, v1.getMeasuredWidth(), v1.getMeasuredHeight());
            v1.draw(canvas);
            Log.v("LOG",b.toString());
            File imageFile = new File(mPath);

            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(imageFile);
                int quality = 100;
                b.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException e) {
                Log.v("LOG","FILENOTFOUND"+e.toString());
            } catch (IOException e) {
                Log.v("LOG","IOEXC"+mPath);
                e.printStackTrace();
            }
            finally {
                Log.v("LOG","SAVED SCREENSHOT"+mPath);
            }
        }
        @Override
        public void run() {
            takeScreenshotAndProcess();
        }
    }
    @Override
    public View onCreateInputView() {
        View view =  getLayoutInflater().inflate(R.layout.keyboard_view, null);
        predictionView = (TextView) view.findViewById(R.id.keyboard_options_display);
        predictionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection inputConnection = getCurrentInputConnection();
                inputConnection.commitText(((TextView)v).getText(),1);
                ((TextView)v).setText("");
            }
        });
        keyboardView = (KeyboardView) view.findViewById(R.id.keyboard_view_keys);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);

        keyboardView.setOnKeyboardActionListener(this);
        return view;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        screenshotTimer = new Timer(true);
        Predictor pred = new Predictor(getApplicationContext(),predictionView);
        screenshotTimer.scheduleAtFixedRate(pred,0,SCREENSHOT_TIMER);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        screenshotTimer.cancel();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }



    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            switch(primaryCode) {
                case Keyboard.KEYCODE_DELETE :
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if (TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0);
                    } else {
                        inputConnection.commitText("", 1);
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));

                    break;
                default :
                    char code = (char) primaryCode;
                    if(Character.isLetter(code) && caps){
                        code = Character.toUpperCase(code);
                    }
                    inputConnection.commitText(String.valueOf(code), 1);

            }
        }

    }


    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}