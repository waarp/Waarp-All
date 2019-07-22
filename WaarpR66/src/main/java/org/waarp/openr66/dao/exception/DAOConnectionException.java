package org.waarp.openr66.dao.exception;

import java.lang.Exception;
import java.lang.Throwable;

/**
 * Connection DAO Exception
 */
public class DAOConnectionException extends DAOException {

    public DAOConnectionException(String message) {
        super(message);
    }

    public DAOConnectionException(Throwable cause) {
        super(cause);
    }

    public DAOConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
} 
