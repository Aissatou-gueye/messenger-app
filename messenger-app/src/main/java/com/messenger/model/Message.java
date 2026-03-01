package com.messenger.model;

public class Message {
    private String content;
    private String time;
    private boolean isSent;

    private boolean isRead;

    public Message(String content, String time, boolean isSent) {
        this.content = content;
        this.time = time;
        this.isSent = isSent;
        this.isRead = false;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
