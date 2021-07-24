/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.commandexec.utils;

/**
 * Message Result for an Execution
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
  public LocalExecResult(final boolean isSuccess, final int status,
                         final Exception exception, final String result) {
    setSuccess(isSuccess);
    setStatus(status);
    setException(exception);
    setResult(result);
  }

  /**
   * Constructor from a pre-existing LocalExecResult
   *
   * @param localExecResult
   */
  public LocalExecResult(final LocalExecResult localExecResult) {
    setSuccess(localExecResult.isSuccess());
    setStatus(localExecResult.getStatus());
    setException(localExecResult.getException());
    setResult(localExecResult.getResult());
  }

  /**
   * Set the values from a LocalExecResult (pointer copy)
   *
   * @param localExecResult
   */
  public final void set(final LocalExecResult localExecResult) {
    setSuccess(localExecResult.isSuccess());
    setStatus(localExecResult.getStatus());
    setException(localExecResult.getException());
    setResult(localExecResult.getResult());
  }

  @Override
  public String toString() {
    return "Status: " + getStatus() + " Output: " + getResult() +
           (getException() != null? "\nError: " + getException().getMessage() :
               "");
  }

  /**
   * @return the status
   */
  public final int getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public final void setStatus(final int status) {
    this.status = status;
  }

  /**
   * @return the isSuccess
   */
  public final boolean isSuccess() {
    return isSuccess;
  }

  /**
   * @param isSuccess the isSuccess to set
   */
  public final void setSuccess(final boolean isSuccess) {
    this.isSuccess = isSuccess;
  }

  /**
   * @return the exception
   */
  public final Exception getException() {
    return exception;
  }

  /**
   * @param exception the exception to set
   */
  public final void setException(final Exception exception) {
    this.exception = exception;
  }

  /**
   * @return the result
   */
  public final String getResult() {
    return result;
  }

  /**
   * @param result the result to set
   */
  public final void setResult(final String result) {
    this.result = result;
  }

}
