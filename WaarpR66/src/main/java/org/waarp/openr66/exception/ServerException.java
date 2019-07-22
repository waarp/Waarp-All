package org.waarp.openr66.exception;

public class ServerException extends Exception {
    public ServerException (String message) {
        super(message);
    }

    public ServerException (String message, Throwable cause) {
        super(message, cause);
    }
}
