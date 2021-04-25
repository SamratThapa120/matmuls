package com.example.matmulskeyboard;

import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.Arrays;
import java.util.List;

public class MessagesStore {
    private TextMessage[] messages;
    private int maxLimit;
    private int currentCount;
    private int currentPointer;
    private String userID;
    public MessagesStore(int maxLimit, String remote) {
        this.maxLimit = maxLimit;
        this.currentCount = 0;
        this.currentPointer = 0;
        this.messages = new TextMessage[maxLimit];
        this.userID = remote;
    }
    public List<TextMessage> getRecentMessages(int count){
        if(count>maxLimit)
            count = maxLimit;
        if (count>currentCount){
            return Arrays.asList(this.getNMessages(currentCount));
        }
        else{
            return Arrays.asList(this.getNMessages(count));
        }
    }

    private TextMessage[] getNMessages(int count) {
        int i=0;
        int p = currentPointer-1;
        TextMessage[] recent= new TextMessage[count];
        while(i<count){
            int z = p-i;
            if(z<0)
                z += maxLimit;
            recent[count-i-1] = this.messages[z];
            i+=1;
        }
        return recent;
    }
//
//    public void putMessages(String messages[]){
//        if(messages.length<1)
//            return;
//        for(String m:messages){
//            currentCount +=1;
//            this.messages[currentPointer] = TextMessage.createForRemoteUser(m,System.currentTimeMillis(),this.userID);
//            currentPointer = (currentPointer+1)%maxLimit;
//        }
//    }

    public boolean isEmpty() {
        if(currentCount<1)
            return true;
        else
            return false;
    }

    public void putRemoteMessage(String text) {
        currentCount +=1;
        this.messages[currentPointer] = TextMessage.createForRemoteUser(text,System.currentTimeMillis(),this.userID);
        currentPointer = (currentPointer+1)%maxLimit;
    }
    public void putLocalMessage(String text) {
        currentCount +=1;
        this.messages[currentPointer] = TextMessage.createForLocalUser(text,System.currentTimeMillis());
        currentPointer = (currentPointer+1)%maxLimit;
    }

    public String getUserID() {
        return this.userID;
    }

    public String getLastMessage() {
        if(currentCount==0)
            return null;
        else
            return messages[(currentPointer+maxLimit-1)%maxLimit].zza();
    }
}
