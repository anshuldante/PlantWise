package com.leafiq.app.ai;

public class AIProviderException extends Exception {
    private final int httpStatusCode;

    public AIProviderException(String message) {
        super(message);
        this.httpStatusCode = 0;
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = 0;
    }

    public AIProviderException(String message, Throwable cause, int httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
