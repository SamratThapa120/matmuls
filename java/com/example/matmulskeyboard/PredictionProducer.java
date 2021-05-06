package com.example.matmulskeyboard;

import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.vision.text.Text;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class PredictionProducer {
    private static final String[] INV_WORDS_CONTAIN = {" am"," pm"," missed your"," minutes ago"," hours ago"};
    private static final String[] INV_WORDS = {"seen","read","active"};

    private final SmartReplyGenerator smartReply;
    private final MyInputMethodService parentService;
    private boolean hasNewInfo;
    private final int middle_pixel;
    private OnSuccessListener<? super SmartReplySuggestionResult> successListener;
    private OnFailureListener failureListener;
    private int CONTEXT_COUNT = 10;
    private int STORE_WIDTH = 50;
    private MessagesStore activeUserHistory;

    public PredictionProducer(MyInputMethodService svc,int img_width) {
        smartReply = SmartReply.getClient();
        middle_pixel = img_width/2;
        parentService = svc;
        successListener = new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                SmartReplySuggestionResult result = (SmartReplySuggestionResult) o;
                if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                    Log.d("LOGGER","Language not supported while predicting");
                } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    svc.receivedPredictionResponse(result);
                }
            }
        };
        failureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("LOGGER","Failed to get prediction code "+e.getMessage());
            }
        };
    }
    public void addMessages(Text result){
        ArrayList<String> messages= new ArrayList<String>(50);
        ArrayList<Boolean> remLocal= new ArrayList<Boolean>(50);
        List<Text.TextBlock> textBlocks = result.getTextBlocks();
        if(activeUserHistory==null)
            setActiveUser(textBlocks.get(0).getLines().get(0).getText());    //Assuming that the first line will always have the username of the remote user.
        String last_message = activeUserHistory.getLastMessage();
        for(int x=textBlocks.size()-1;x>=0;x--) {
            List<Text.Line> lines = textBlocks.get(x).getLines();
            for(int y=lines.size()-1;y>=0;y--) {
                Text.Line line = lines.get(y);
                Point[] blockCornerPoints = line.getCornerPoints();
                int gravity = (blockCornerPoints[0].x+blockCornerPoints[1].x)/2;
                if(Math.abs(gravity-middle_pixel)<5){ //Ignores text at the centre. Usually time and date of conversation.
                    continue;
                }
                String text = line.getText();
                if(last_message !=null) {
                    if (text.equals(last_message)) {
                        for (int z = messages.size() - 1; z >= 0; z--) {
                            if (remLocal.get(z)) {
                                Log.d("LOGGER_pred", "ADDED to remote " + text);
                                activeUserHistory.putRemoteMessage(messages.get(z));
                            } else {
                                activeUserHistory.putLocalMessage(messages.get(z));
                                Log.d("LOGGER_pred", "ADDED to local " + text);

                            }
                        }
                        return;
                    }
                }
                if(unnecessaryText(text))
                    continue;
                messages.add(text);
                if(gravity<middle_pixel) { //Remote messages are aligned to the left of the screen
                    remLocal.add(true);
                }
                else { //Local messages are aligned to the right of the screen
                    remLocal.add(false);
                }
            }
        }
        for(int z=messages.size()-1;z>=0;z--){
            if(remLocal.get(z)) {
                Log.d("LOGGER_pred", "ADDED to remote " + messages.get(z));
                activeUserHistory.putRemoteMessage(messages.get(z));
            }
            else {
                activeUserHistory.putLocalMessage(messages.get(z));
                Log.d("LOGGER_pred","ADDED to local "+messages.get(z));

            }
        }
    }

    private boolean unnecessaryText(String text) {
        text = text.toLowerCase();
        for(String inv: INV_WORDS_CONTAIN){
            if(text.contains(inv))
                return true;
        }
        for(String inv: INV_WORDS){
            if(text.equals(inv))
                return true;
        }
        return false;
    }

    private void setActiveUser(String remote) {
        activeUserHistory = new MessagesStore(parentService.getApplicationContext(),STORE_WIDTH,remote);
        Log.d("LOGGER_pred","Switched to user :"+remote);
    }

    public void closePredictor(){
        smartReply.close();
        try {
            activeUserHistory.saveToFile();
            Log.d("LOGGER","SAVED USER DATA");
        } catch (Exception e) {
            Log.d("LOGGER","Failed to save "+e.getMessage());
        }
    }
    public void newPredictions(){
        if (activeUserHistory.isEmpty() && !hasNewInfo){
            return;
        }
        hasNewInfo=false;
        smartReply.suggestReplies(activeUserHistory.getRecentMessages(CONTEXT_COUNT))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}
