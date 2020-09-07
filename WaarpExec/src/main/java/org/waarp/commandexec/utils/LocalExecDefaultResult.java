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
 * Default message for LocalExec
 */
public final class LocalExecDefaultResult {
  public static final LocalExecResult NoCommand =
      new LocalExecResult(false, -1, null, "No Command");
  public static final LocalExecResult BadTransmition =
      new LocalExecResult(false, -2, null, "Bad Transmission");
  public static final LocalExecResult NoMessage =
      new LocalExecResult(false, -3, null, "No Message received");
  public static final LocalExecResult NotExecutable =
      new LocalExecResult(false, -4, null, "Not Executable");
  public static final LocalExecResult BadExecution =
      new LocalExecResult(false, -5, null, "Bad Execution");
  public static final LocalExecResult TimeOutExecution =
      new LocalExecResult(false, -6, null, "TimeOut Execution");
  public static final LocalExecResult InternalError =
      new LocalExecResult(false, -7, null, "Internal Error");
  public static final LocalExecResult NoStatus =
      new LocalExecResult(false, -8, null, "No Status");
  public static final LocalExecResult ConnectionRefused =
      new LocalExecResult(false, -9, null,
                          "Exec Server refused the connection");
  public static final LocalExecResult ShutdownOnGoing =
      new LocalExecResult(false, -10, null, "Exec Server shutdown on going");
  public static final LocalExecResult CorrectExec =
      new LocalExecResult(false, 1, null, "Correctly Executed");
  public static final long RETRYINMS = 200;
  public static final long MAXWAITPROCESS = 60000;
  public static final String ENDOFCOMMAND = "$#GGEXEC END OF COMMAND#$";

  private LocalExecDefaultResult() {
  }
}
