/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context;

import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66Exception;

/**
 * This class is the result for every operations in OpenR66.
 * 
 * @author Frederic Bregier
 * 
 */
public class R66Result {
    /**
     * The exception associated in case of error (if any exception)
     */
    private OpenR66Exception exception = null;
    /**
     * The file if any
     */
    private R66File file = null;
    /**
     * The runner if any
     */
    private DbTaskRunner runner = null;
    /**
     * Does this result already have been transfered to the remote server
     */
    private boolean isAnswered = false;
    /**
     * The code (error or not)
     */
    private ErrorCode code;
    /**
     * Any other object for special operations (test or shutdown for instance)
     */
    private Object other = null;

    /**
     * @param exception
     * @param session
     * @param isAnswered
     * @param code
     * @param runner
     */
    public R66Result(OpenR66Exception exception, R66Session session,
            boolean isAnswered, ErrorCode code, DbTaskRunner runner) {
        this.setException(exception);
        this.setRunner(runner);
        if (session != null) {
            setFile(session.getFile());
            this.setRunner(session.getRunner());
        }
        this.setAnswered(isAnswered);
        this.setCode(code);
    }

    /**
     * @param session
     * @param isAnswered
     * @param code
     * @param runner
     */
    public R66Result(R66Session session, boolean isAnswered, ErrorCode code,
            DbTaskRunner runner) {
        this.setRunner(runner);
        if (session != null) {
            setFile(session.getFile());
            this.setRunner(session.getRunner());
        }
        this.setAnswered(isAnswered);
        this.setCode(code);
    }

    @Override
    public String toString() {
        return (getException() != null ? "Exception: " + getException().toString() : "") +
                (getFile() != null ? getFile().toString() : " no file") + "     " +
                (getRunner() != null ? getRunner().toShortString() : " no runner") +
                " isAnswered: " + isAnswered() + " Code: " + getCode().getMesg();
    }

    /**
     * 
     * @return the associated message with this Result
     */
    public String getMessage() {
        if (getException() != null) {
            return getException().getMessage();
        } else {
            return getCode().getMesg();
        }
    }

    /**
     * @return the exception
     */
    public OpenR66Exception getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(OpenR66Exception exception) {
        this.exception = exception;
    }

    /**
     * @return the file
     */
    public R66File getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(R66File file) {
        this.file = file;
    }

    /**
     * @return the runner
     */
    public DbTaskRunner getRunner() {
        return runner;
    }

    /**
     * @param runner the runner to set
     */
    public void setRunner(DbTaskRunner runner) {
        this.runner = runner;
    }

    /**
     * @return the isAnswered
     */
    public boolean isAnswered() {
        return isAnswered;
    }

    /**
     * @param isAnswered the isAnswered to set
     */
    public void setAnswered(boolean isAnswered) {
        this.isAnswered = isAnswered;
    }

    /**
     * @return the code
     */
    public ErrorCode getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(ErrorCode code) {
        this.code = code;
    }

    /**
     * @return the other
     */
    public Object getOther() {
        return other;
    }

    /**
     * @param other the other to set
     */
    public void setOther(Object other) {
        this.other = other;
    }
}
