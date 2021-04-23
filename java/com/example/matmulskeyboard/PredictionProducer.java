package com.example.matmulskeyboard;

import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;
import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import androidx.annotation.NonNull;

public class PredictionProducer {
    private final SmartReplyGenerator smartReply;
    private final MyInputMethodService parentService;
    private final int middle_pixel;
//    private MessagesStore historyRemote;
//    private MessagesStore historyLocal;
    private MessagesStore messgesHistory;
    private OnSuccessListener<? super SmartReplySuggestionResult> successListener;
    private OnFailureListener failureListener;
    private int CONTEXT_COUNT = 10;
    private int STORE_WIDTH = 20;

    public PredictionProducer(MyInputMethodService svc,int img_width) {
        smartReply = SmartReply.getClient();
        middle_pixel = img_width/2;
//        historyRemote = new MessagesStore(STORE_WIDTH,"Remote",true);
//        historyLocal = new MessagesStore(STORE_WIDTH,"Local",false);
        messgesHistory = new MessagesStore(STORE_WIDTH);
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
        String resultText = result.getText();
        for (Text.TextBlock block : result.getTextBlocks()) {
            Point[] blockCornerPoints = block.getCornerPoints();
            if((blockCornerPoints[0].x+blockCornerPoints[1].x)/2<middle_pixel) {
                Log.d("LOGGER_pred", "ADDED to remote " + block.getText());
                messgesHistory.putRemoteMessage(block.getText(),"USER");
            }
            else {
                Log.d("LOGGER_pred","ADDED to local "+block.getText());
                messgesHistory.putLocalMessage(block.getText());
            }
//            Rect blockFrame = block.getBoundingBox();
//            for (Text.Line line : block.getLines()) {
//                String lineText = line.getText();
//                Point[] lineCornerPoints = line.getCornerPoints();
//                Rect lineFrame = line.getBoundingBox();
//                for (Text.Element element : line.getElements()) {
//                    String elementText = element.getText();
//                    Point[] elementCornerPoints = element.getCornerPoints();
//                    Rect elementFrame = element.getBoundingBox();
//                }
//            }
        }
    }
    public void newPredictions(){
        if (messgesHistory.isEmpty()){
            return;
        }
        smartReply.suggestReplies(messgesHistory.getRecentMessages(CONTEXT_COUNT))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}
