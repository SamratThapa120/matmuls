package com.example.matmulskeyboard;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class PredictionProducer {
    private final SmartReplyGenerator smartReply;
    private final MyInputMethodService parentService;
    private ArrayList<String> historyRemote;
    private ArrayList<String> historyLocal;
    private static int historySize;
    private static int contextSize;
    private OnSuccessListener<? super SmartReplySuggestionResult> successListener;
    private OnFailureListener failureListener;

    public PredictionProducer(MyInputMethodService svc) {
        smartReply = SmartReply.getClient();
        parentService = svc;
        successListener = new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                SmartReplySuggestionResult result = (SmartReplySuggestionResult) o;
                if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                    // The conversation's language isn't supported, so
                    // the result doesn't contain any suggestions.
                } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    svc.receivedPredictionResponse(result);
                }
            }
        };
        failureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        };
    }

    public void getPredictions(){
        smartReply.suggestReplies(getRecentConversation())
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
    public List<TextMessage>  getRecentConversation(){
        return null;
    }
}
