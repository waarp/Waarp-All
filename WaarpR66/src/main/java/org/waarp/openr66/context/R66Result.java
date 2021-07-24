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
package org.waarp.openr66.context;

import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66Exception;

/**
 * This class is the result for every operations in OpenR66.
 */
public class R66Result {
  /**
   * The exception associated in case of error (if any exception)
   */
  private OpenR66Exception exception;
  /**
   * The file if any
   */
  private R66File file;
  /**
   * The runner if any
   */
  private DbTaskRunner runner;
  /**
   * Does this result already have been transfered to the remote server
   */
  private boolean isAnswered;
  /**
   * The code (error or not)
   */
  private ErrorCode code;
  /**
   * Any other object for special operations (test or shutdown for instance)
   */
  private Object other;

  /**
   * @param exception
   * @param session
   * @param isAnswered
   * @param code
   * @param runner
   */
  public R66Result(final OpenR66Exception exception, final R66Session session,
                   final boolean isAnswered, final ErrorCode code,
                   final DbTaskRunner runner) {
    setException(exception);
    setRunner(runner);
    if (session != null) {
      setFile(session.getFile());
      setRunner(session.getRunner());
    }
    setAnswered(isAnswered);
    setCode(code);
  }

  /**
   * @param session
   * @param isAnswered
   * @param code
   * @param runner
   */
  public R66Result(final R66Session session, final boolean isAnswered,
                   final ErrorCode code, final DbTaskRunner runner) {
    setRunner(runner);
    if (session != null) {
      setFile(session.getFile());
      setRunner(session.getRunner());
    }
    setAnswered(isAnswered);
    setCode(code);
  }

  @Override
  public final String toString() {
    return (getException() != null? "Exception: " + getException() : "") +
           (getFile() != null? getFile().toString() : " no file") + "     " +
           (getRunner() != null? getRunner().toShortString() : " no runner") +
           " isAnswered: " + isAnswered() + " Code: " + getCode().getMesg();
  }

  /**
   * @return the associated message with this Result
   */
  public final String getMessage() {
    if (getException() != null) {
      return getException().getMessage();
    } else {
      return getCode().getMesg();
    }
  }

  /**
   * @return the exception
   */
  public final OpenR66Exception getException() {
    return exception;
  }

  /**
   * @param exception the exception to set
   */
  public final void setException(final OpenR66Exception exception) {
    this.exception = exception;
  }

  /**
   * @return the file
   */
  public final R66File getFile() {
    return file;
  }

  /**
   * @param file the file to set
   */
  public final void setFile(final R66File file) {
    this.file = file;
  }

  /**
   * @return the runner
   */
  public final DbTaskRunner getRunner() {
    return runner;
  }

  /**
   * @param runner the runner to set
   */
  public final void setRunner(final DbTaskRunner runner) {
    this.runner = runner;
  }

  /**
   * @return the isAnswered
   */
  public final boolean isAnswered() {
    return isAnswered;
  }

  /**
   * @param isAnswered the isAnswered to set
   */
  public final void setAnswered(final boolean isAnswered) {
    this.isAnswered = isAnswered;
  }

  /**
   * @return the code
   */
  public final ErrorCode getCode() {
    return code;
  }

  /**
   * @param code the code to set
   */
  public final void setCode(final ErrorCode code) {
    this.code = code;
  }

  /**
   * @return the other
   */
  public final Object getOther() {
    return other;
  }

  /**
   * @param other the other to set
   */
  public final void setOther(final Object other) {
    this.other = other;
  }
}
