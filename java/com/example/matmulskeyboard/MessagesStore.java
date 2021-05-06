package com.example.matmulskeyboard;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.mlkit.nl.smartreply.TextMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

public class MessagesStore {
    private final Context context;
    private TextMessage[] messages;
    private int maxLimit;
    private int currentCount;
    private int currentPointer;
    private String userID;
//    private String REM_USER_DIRECTORY ="/REMOTE_USER_DATA/";

//    private final String USERID = "USER_ID";
    private final String MESSAGE = "MESSAGE";
//    private final String TIMESTAMP = "TIMESTAMP";
    private final String REMOTE = "REMOTE";
    public MessagesStore(Context context, int maxLimit, String remote) {
        this.maxLimit = maxLimit;
        this.currentCount = 0;
        this.currentPointer = 0;
        this.messages = new TextMessage[maxLimit];
        this.userID = remote;
        this.context = context;
        try {
            File user_file = new File(context.getFilesDir(), this.userID);
            if(!user_file.exists()){
                user_file.createNewFile();
            }
            else{
                loadFileContents(user_file);
            }
        } catch (Exception e){
            Log.d("LOGGER","Error loading file "+e.getMessage());
        }
    }

    private void loadFileContents(File user_file) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(user_file.getAbsoluteFile()));
        String line=reader.readLine();
        while(line!=null){
            JSONObject obj = new JSONObject(line);
            if(obj.has(REMOTE)){
                if(obj.getBoolean(REMOTE))
                    this.putLocalMessage(obj.getString(MESSAGE));
                else
                    this.putRemoteMessage(obj.getString(MESSAGE));
            }
            line = reader.readLine();
        }
        reader.close();
        Log.d("LOGGER","LOADED_EXISTING_USER "+this.userID);
    }

    public void saveToFile() throws IOException, JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        for(TextMessage m: getNMessages(currentCount>maxLimit?maxLimit:currentCount)){
            JsonObject obj = new JsonObject();
            obj.addProperty(MESSAGE,m.zza());
            obj.addProperty(REMOTE,m.zzd());
            stringBuilder.append(obj.toString());
            stringBuilder.append("\n");
        }
        OutputStreamWriter writer = new OutputStreamWriter(this.context.openFileOutput
                (this.userID, Context.MODE_PRIVATE));
        writer.write(stringBuilder.toString());
        writer.flush();
        writer.close();
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
