package org.waarp.openr66.dao.exception;

/**
 * Exception when no data found in select or update
 */
public class DAONoDataException extends DAOException {

    public DAONoDataException(String message) {
        super(message);
    }

    public DAONoDataException(Throwable cause) {
        super(cause);
    }

    public DAONoDataException(String message, Throwable cause) {
        super(message, cause);
    }
} 
