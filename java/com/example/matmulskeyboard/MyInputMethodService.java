package com.example.matmulskeyboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;


import java.io.Serializable;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import eu.bolt.screenshotty.ScreenshotManager;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;


public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener{

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT";
    public static final String SCREENSHOT_HANDLER_THREAD = "SCREENSHOT_HANDLER_THREAD";
    static final int VIRT_DISPLAY_FLAGS =DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static final int START_FOREGROUND = 1;
    private static final String USE_SERVICES = "USE_SERVICES";

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private TextView predictionView1,predictionView2;
    private boolean caps = false;
    private int SCREENSHOT_TIMER = 2000;
    private MediaProjectionManager mgr;
    private WindowManager windowMgr;
    private long beforeTime;
    private boolean hasPermission = false;
    private Intent screenshotPermissionIntent;
    private int screenshotResultCode;
    private HandlerThread screenshotHandlerThread;
    private Handler screenshotHandler;
    private ImageTransmogrifier imageTransmogrifier;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    private Bitmap latestBitmap;
    private TextRecognizer textExtractor;
    private PredictionProducer predictionProducer;
    private Switch enableServices;
    private boolean useServices;


    public Handler getHandler() {
        return screenshotHandler;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForeground() {
        String channelId = createNotificationChannel("my_service", "My Background Service");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Matmuls Assistant")
                .setContentText("Always at your service.")
                .setPriority(PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(START_FOREGROUND, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        mgr = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            startForeground();
        }
        else{
            Notification notification =
                    new Notification.Builder(this)
                            .setContentTitle("Starting Foreground")
                            .setContentText("The keyboard is running foregorund")
                            .build();
            startForeground(START_FOREGROUND,notification);
        }
        windowMgr = (WindowManager) getSystemService(WINDOW_SERVICE);
        checkForScreenshotPermission();
        if (hasPermission) {
            initializeScreenCapturing();
        }
        beforeTime = System.currentTimeMillis();
    }
    @Override
    public View onCreateInputView() {
        View view =  getLayoutInflater().inflate(R.layout.keyboard_view, null);
        predictionView1 = (TextView) view.findViewById(R.id.keyboard_options_display1);
        predictionView2 = (TextView) view.findViewById(R.id.keyboard_options_display2);

        enableServices = (Switch) view.findViewById(R.id.enable_services_switch);
        enableServices.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useServices = isChecked;
                if(!isChecked) {
                    stopServices();
                    predictionView2.setText("");
                    predictionView1.setText("");
                }
            }
        });
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(pref.contains(USE_SERVICES))
            useServices  = pref.getBoolean(USE_SERVICES,true);
        else
            useServices=true;
        enableServices.setChecked(useServices);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection inputConnection = getCurrentInputConnection();
                inputConnection.commitText(((TextView)v).getText(),1);
                predictionView1.setText("");
                predictionView2.setText("");
            }
        };
        predictionView1.setOnClickListener(clickListener);
        predictionView2.setOnClickListener(clickListener);
        keyboardView = (KeyboardView) view.findViewById(R.id.keyboard_view_keys);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ImageTransmogrifier newIt=new ImageTransmogrifier(this);
        if (newIt.getWidth()!=imageTransmogrifier.getWidth() ||
                newIt.getHeight()!=imageTransmogrifier.getHeight()) {
            ImageTransmogrifier oldIt=imageTransmogrifier;
            imageTransmogrifier=newIt;
            vdisplay.resize(imageTransmogrifier.getWidth(), imageTransmogrifier.getHeight(),
                    getResources().getDisplayMetrics().densityDpi);
            vdisplay.setSurface(imageTransmogrifier.getSurface());
            oldIt.close();
        }
    }

    private void initializeScreenCapturing() {
        try {
            screenshotHandlerThread = new HandlerThread(SCREENSHOT_HANDLER_THREAD, Process.THREAD_PRIORITY_BACKGROUND);
            screenshotHandlerThread.start();
            screenshotHandler = new Handler(screenshotHandlerThread.getLooper());
            projection = mgr.getMediaProjection(screenshotResultCode, screenshotPermissionIntent);
            imageTransmogrifier = new ImageTransmogrifier(this);
            vdisplay = projection.createVirtualDisplay("andprojector",
                    imageTransmogrifier.getWidth(), imageTransmogrifier.getHeight(),
                    imageTransmogrifier.getDensity(),
                    VIRT_DISPLAY_FLAGS, imageTransmogrifier.getSurface(), null, screenshotHandler);
            MediaProjection.Callback cb = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    vdisplay.release();
                }
            };
            projection.registerCallback(cb, screenshotHandler);
            textExtractor = TextRecognition.getClient();
            predictionProducer = new PredictionProducer(this,imageTransmogrifier.getWidth());
            Log.d("LOGGER", "Got media projection");
        }
        catch (Exception e){
            Log.d("LOGGER", "Got error while setting media projector");
            hasPermission = false;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(EXTRA_RESULT_INTENT)) {
            this.screenshotPermissionIntent = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
            this.screenshotResultCode = intent.getIntExtra(EXTRA_RESULT_CODE,-1);
            return START_REDELIVER_INTENT;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkForScreenshotPermission() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(screenshotPermissionIntent != null){
            hasPermission = true;
            Log.d("LOGGER","has permission, has intent");
        }
        else {
            Log.d("LOGGER", "NO_PERMISSION while checking screenshot permission");
            hasPermission = false;
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        predictionView2.setText("");
        predictionView1.setText("");
        stopServices();
    }

    private void stopServices() {
        if(screenshotHandlerThread!=null){
            screenshotHandlerThread.quit();
        }
        if(projection!=null){
            projection.stop();
            vdisplay.release();
        }
        if(predictionProducer!=null)
            predictionProducer.closePredictor();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.edit().putBoolean(USE_SERVICES,useServices).commit();
        stopForeground(true);
    }

    @Override
    public void onPress(int i) {
        if(!useServices)
            return;
        if ((System.currentTimeMillis() - beforeTime) > SCREENSHOT_TIMER) {
            if (!hasPermission) {
                checkForScreenshotPermission();
                if (!hasPermission) {
                    Intent intent = new Intent(getApplicationContext(), AskerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    startActivity(intent);
                } else {
                    initializeScreenCapturing();
                }
            } else {
                if(latestBitmap!=null){
                    InputImage image = InputImage.fromBitmap(latestBitmap,0);
                    textExtractor.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text result) {
                                predictionProducer.addMessages(result);
                                predictionProducer.newPredictions();
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("LOGGER","Could not extract text from image "+e.getMessage());
                                    }
                                });

                    latestBitmap = null;
                }
                beforeTime = System.currentTimeMillis();
            }
        }
    }


    public void receivedPredictionResponse(SmartReplySuggestionResult result) {
        List<SmartReplySuggestion> sugg = result.getSuggestions();
        if(sugg.size()>0)
            predictionView1.setText(sugg.get(0).getText());
        if(sugg.size()>1)
            predictionView2.setText(sugg.get(1).getText());
    }

    public void setScreenCaptureIntent(Intent data) {
        this.screenshotPermissionIntent = data;
    }

    public void updateBitmap(Bitmap cropped) {
        latestBitmap = cropped;
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