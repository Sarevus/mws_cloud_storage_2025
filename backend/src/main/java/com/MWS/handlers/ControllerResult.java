package com.MWS.handlers;

public class ControllerResult {
    private static int status;
    private static String body;
    private static String contentType;

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
