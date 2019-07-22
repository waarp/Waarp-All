/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.commandexec.utils;

/**
 * Message Result for an Execution
 *
 * @author Frederic Bregier
 *
 */
public class LocalExecResult {
    private int status;
    private boolean isSuccess;
    private Exception exception;
    private String result;

    /**
     * @param status
     * @param exception
     * @param result
     */
    public LocalExecResult(boolean isSuccess, int status, Exception exception, String result) {
        this.setSuccess(isSuccess);
        this.setStatus(status);
        this.setException(exception);
        this.setResult(result);
    }

    /**
     * Constructor from a pre-existing LocalExecResult
     * 
     * @param localExecResult
     */
    public LocalExecResult(LocalExecResult localExecResult) {
        this.setSuccess(localExecResult.isSuccess());
        this.setStatus(localExecResult.getStatus());
        this.setException(localExecResult.getException());
        this.setResult(localExecResult.getResult());
    }

    /**
     * Set the values from a LocalExecResult (pointer copy)
     * 
     * @param localExecResult
     */
    public void set(LocalExecResult localExecResult) {
        this.setSuccess(localExecResult.isSuccess());
        this.setStatus(localExecResult.getStatus());
        this.setException(localExecResult.getException());
        this.setResult(localExecResult.getResult());
    }

    @Override
    public String toString() {
        return "Status: " + getStatus() + " Output: " + getResult()
                + (getException() != null ? "\nError: " + getException().getMessage() : "");
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the isSuccess
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * @param isSuccess the isSuccess to set
     */
    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    /**
     * @return the exception
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

}
