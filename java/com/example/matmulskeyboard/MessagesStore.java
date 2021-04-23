package com.example.matmulskeyboard;

public class MessagesStore {
    private String[] messages;
    private int maxLimit;
    private int currentCount;
    private int currentPointer;

    public MessagesStore(int maxLimit) {
        this.maxLimit = maxLimit;
        this.currentCount = 0;
        this.currentPointer = 0;
        this.messages = new String[maxLimit];
    }
    public String[] getRecentMessages(int count){
        if(count>maxLimit)
            count = maxLimit;
        if (count>currentCount){
            return this.getNMessages(currentCount);
        }
        else{
            return this.getNMessages(count);
        }
    }

    private String[] getNMessages(int count) {
        int i=0;
        int p = currentPointer-1;
        String[] recent= new String[count];
        while(i<count){
            int z = p-i;
            if(z<0)
                z += maxLimit;
            recent[count-i-1] = this.messages[z];
            i+=1;
        }
        return recent;
    }

    public void putMessages(String messages[]){
        if(messages.length<1)
            return;
        for(String m:messages){
            currentCount +=1;
            this.messages[currentPointer] = m;
            currentPointer = (currentPointer+1)%maxLimit;
        }
    }
}
