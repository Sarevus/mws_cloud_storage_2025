package com.MWS.handlers;

public class ControllerResult {
    private int status;
    private String body;
    private String contentType;

    public ControllerResult(int status, String body) {
        this(status, body, "application/json; charset=utf-8");
    }

    public ControllerResult(int status, String body, String contentType) {
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    // геттеры
    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }
}