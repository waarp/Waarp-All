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
package org.waarp.openr66.context.task;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Reschedule Transfer task to a time delayed by the specified number of
 * milliseconds, if the error code is
 * one of the specified codes and the optional intervals of date are compatible
 * with the new time schedule<br>
 * <br>
 * <p>
 * Result of arguments will be as following options (the two first are
 * mandatory):<br>
 * <br>
 * <p>
 * "-delay ms" where ms is the added number of ms on current time before retry
 * on schedule<br>
 * <br>
 * "-case errorCode,errorCode,..." where errorCode is one of the following error
 * of the current transfer
 * (either literal or code in 1 character:<br>
 * ConnectionImpossible(C), ServerOverloaded(l), BadAuthent(A), ExternalOp(E),
 * TransferError(T), MD5Error(M),
 * Disconnection(D), RemoteShutdown(r), FinalOp(F), Unimplemented(U),
 * Shutdown(S), RemoteError(R),
 * Internal(I), StoppedTransfer(H), CanceledTransfer(K), Warning(W), Unknown(-),
 * QueryAlreadyFinished(Q),
 * QueryStillRunning(s), NotKnownHost(N), QueryRemotelyUnknown(u),
 * FileNotFound(f), CommandNotFound(c),
 * PassThroughMode(p)<br>
 * <br>
 * "-between start;end" and/or "-notbetween start;end" (multiple times are
 * allowed, start or end can be not
 * set) and where start and stop are in the following format:<br>
 * Yn:Mn:Dn:Hn:mn:Sn where n is a number for each time specification, each
 * specification is optional, as
 * Y=Year, M=Month, D=Day, H=Hour, m=minute, s=second.<br>
 * Format can be X+n, X-n, X=n or Xn where X+-n means adding/subtracting n to
 * current date value, while X=n or
 * Xn means setting exact value<br>
 * If one time specification is not set, it is based on the current date.<br>
 * <br>
 * "-count limit" will be the limit of retry. The current value limit is taken
 * from the "transferInfo"
 * internal code (not any more the "information of transfer")and not from the
 * rule as "{"CPTLIMIT": limit}" as
 * JSON code. Each time this function is called, the limit value will be
 * replaced as newlimit = limit - 1 in
 * the "transferInfo" as "{"CPTLIMIT": limit}" as JSON code.<br>
 * To ensure correctness, the value must be in the "transferInfo" internal code
 * since this value will be
 * changed statically in the "transferInfo". If taken from the rule, it will be
 * wrong since the value will
 * never decrease. However, a value must be setup in the rule in order to reset
 * the value when the count reach
 * 0. <br>
 * So in the rule, "-count resetlimit" must be present, where resetlimit will be
 * the new value set when the
 * limit reach 0, and in the "transferInfo" internal code, "{"CPTLIMIT": limit}"
 * as JSON code must be present.
 * If one is missing, the condition is not applied. <br>
 * <br>
 * If "-notbetween" is specified, the planned date must not be in the area.<br>
 * If "-between" is specified, the planned date must be found in any such
 * specified areas (could be in any of
 * the occurrence). If not specified, it only depends on "-notbetween".<br>
 * If none is specified, the planned date is always valid.<br>
 * <br>
 * <p>
 * Note that if a previous called to a reschedule was done for this attempt and
 * was successful, the following
 * calls will be ignored.<br>
 * <br>
 * <B>Important note: any subsequent task will be ignored and not executed once
 * the reschedule is
 * accepted.</B><br>
 * <br>
 * In case start > end, end will be +1 day<br>
 * In case start and end < current planned date, both will have +1 day.<br>
 * <br>
 * <p>
 * Example: -delay 3600000 -case ConnectionImpossible,ServerOverloaded,Shutdown
 * -notbetween H7:m0:S0;H19:m0:S0
 * -notbetween H1:m0:S0;H=3:m0:S0<br>
 * means retry in case of error during initialization of connection in 1 hour if
 * not between 7AM to 7PM and
 * not between 1AM to 3AM.<br>
 */
public class RescheduleTransferTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RescheduleTransferTask.class);

  /**
   * Delimiter for -count option in Reschedule to be placed in the transfer
   * info
   * of transfer as {"CPTLIMIT":
   * limit} where limit is an integer.
   */
  public static final String CPTLIMIT = "CPTLIMIT";
  public static final String CPTTOTAL = "CPTTOTAL";

  protected long newdate;

  protected Calendar newDate;

  protected boolean countUsed;

  protected int limitCount = -1;

  protected int totalCount;

  protected int resetCount = -1;

  protected DbTaskRunner runner;

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public RescheduleTransferTask(final String argRule, final int delay,
                                final String argTransfer,
                                final R66Session session) {
    super(TaskType.RESCHEDULE, delay, argRule, argTransfer, session);
  }

  @Override
  public final void run() {
    logger.info("Reschedule with {}:{} and {}", argRule, argTransfer, session);
    runner = session.getRunner();
    if (runner == null) {
      futureCompletion.setFailure(
          new OpenR66RunnerErrorException("No valid runner in Reschedule"));
      return;
    }
    if (runner.isRescheduledTransfer()) {
      // Already rescheduled so ignore
      final R66Result result =
          new R66Result(session, false, ErrorCode.Warning, runner);
      futureCompletion.setResult(result);
      logger.warn("Transfer already Rescheduled: " + runner.toShortString());
      futureCompletion.setSuccess();
      return;
    }
    if (runner.isRequestOnRequested()) {
      // Self Requested Request so reschedule is ignored
      final R66Result result =
          new R66Result(session, false, ErrorCode.LoopSelfRequestedHost,
                        runner);
      futureCompletion.setResult(result);
      futureCompletion.setFailure(new OpenR66RunnerErrorException(
          "No valid runner in Reschedule since Self Requested"));
      return;
    }
    String finalname = argRule;
    finalname = getReplacedValue(finalname, BLANK.split(argTransfer));
    final String[] args = BLANK.split(finalname);
    if (args.length < 4) {
      final R66Result result =
          new R66Result(session, false, ErrorCode.Warning, runner);
      futureCompletion.setResult(result);
      logger.warn(
          "Not enough argument in Reschedule: " + runner.toShortString());
      futureCompletion.setSuccess();
      return;
    }
    if (!validateArgs(args)) {
      final R66Result result =
          new R66Result(session, false, ErrorCode.Warning, runner);
      futureCompletion.setResult(result);
      logger.warn(
          "Reschedule unallowed due to argument: " + runner.toShortString());
      futureCompletion.setSuccess();
      return;
    }
    if (countUsed) {
      limitCount--;
      if (limitCount >= 0) {
        // restart is allowed so resetting to new limitCount
        resetCount = limitCount;
      }
      resetInformation(resetCount);
      if (limitCount < 0) {
        // Must not reschedule since limit is reached
        try {
          runner.saveStatus();
        } catch (final OpenR66RunnerErrorException ignored) {
          // nothing
        }
        final R66Result result =
            new R66Result(session, false, ErrorCode.Warning, runner);
        futureCompletion.setResult(result);
        logger.warn("Reschedule unallowed due to limit reached: " +
                    runner.toShortString());
        futureCompletion.setSuccess();
        return;
      }
    }
    final Timestamp start = new Timestamp(newdate);
    try {
      runner.setStart(start);
      if (runner.restart(true)) {
        runner.saveStatus();
      }
    } catch (final OpenR66RunnerErrorException e) {
      logger.error(
          "Prepare transfer in     FAILURE      " + runner.toShortString() +
          "     <AT>" + new Date(newdate) + "</AT>", e);
      futureCompletion.setFailure(new OpenR66RunnerErrorException(
          "Reschedule failed: " + e.getMessage(), e));
      return;
    }
    runner.setRescheduledTransfer();
    final R66Result result =
        new R66Result(session, false, ErrorCode.Warning, runner);
    futureCompletion.setResult(result);
    logger.warn(
        "Reschedule transfer in     SUCCESS     " + runner.toShortString() +
        "     <AT>" + new Date(newdate) + "</AT>");
    futureCompletion.setSuccess();
  }

  protected final void resetInformation(final int value) {
    final Map<String, Object> root = runner.getTransferMap();
    root.put(CPTLIMIT, value);
    try {
      totalCount = (Integer) root.get(CPTTOTAL);
      totalCount++;
      root.put(CPTTOTAL, totalCount);
    } catch (final Exception e) {
      totalCount = 1;
      root.put(CPTTOTAL, totalCount);
    }
    runner.setTransferMap(root);
  }

  protected final boolean validateArgs(final String[] args) {
    boolean validCode = false;
    for (int i = 0; i < args.length; i++) {
      if ("-delay".equalsIgnoreCase(args[i])) {
        i++;
        try {
          newdate = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          logger.warn("Bad Long format: args[i]");
          return false;
        }
      } else if ("-case".equalsIgnoreCase(args[i])) {
        i++;
        if (!validCode) {
          final String[] codes = args[i].split(",");
          for (final String code2 : codes) {
            final ErrorCode code = ErrorCode.getFromCode(code2);
            if (session.getLocalChannelReference().getCurrentCode() == code) {
              logger.debug("Code valid: {}", code);
              validCode = true;
            }
          }
        }
      } else if ("-count".equalsIgnoreCase(args[i])) {
        i++;
        try {
          resetCount = Integer.parseInt(args[i]);
        } catch (final NumberFormatException e) {
          logger.warn("ResetLimit is not an integer: " + args[i]);
          countUsed = false;
          return false;
        }
        final Map<String, Object> root = runner.getTransferMap();
        final Integer limit;
        try {
          limit = (Integer) root.get(CPTLIMIT);
        } catch (final Exception e) {
          logger.warn("Bad Long format: CPTLIMIT" + " : {}", e.getMessage());
          return false;
        }
        if (limit != null) {
          limitCount = limit;
        } else {
          limitCount = resetCount;
          root.put(CPTLIMIT, limitCount);
        }
        countUsed = true;
      }
    }
    // now we have new delay plus code
    if (!validCode) {
      logger.warn("No valid Code found");
      return false;
    }
    if (newdate <= 0) {
      logger.warn("Delay is negative: " + newdate);
      return false;
    }
    newdate += System.currentTimeMillis();
    newDate = Calendar.getInstance();
    newDate.setTimeInMillis(newdate);
    boolean betweenTest = false;
    boolean betweenResult = false;
    for (int i = 0; i < args.length; i++) {
      if ("-notbetween".equalsIgnoreCase(args[i])) {
        i++;
        final String[] elmts = args[i].split(";");
        boolean startModified = false;
        String[] values = elmts[0].split(":");
        Calendar start = getCalendar(values);
        if (start != null) {
          startModified = true;
        } else {
          start = Calendar.getInstance();
        }
        boolean stopModified = false;
        values = elmts[1].split(":");
        Calendar stop = getCalendar(values);
        if (stop != null) {
          stopModified = true;
        } else {
          stop = Calendar.getInstance();
        }
        logger.debug("Dates before check: Not between {} and {}",
                     start.getTime(), stop.getTime());
        // Check that start < stop
        if (start.compareTo(stop) > 0) {
          // no so add 24H to stop
          stop.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Check that start and stop > newDate (only start is checked since start <= stop)
        if (start.compareTo(newDate) < 0) {
          start.add(Calendar.DAY_OF_MONTH, 1);
          stop.add(Calendar.DAY_OF_MONTH, 1);
        }
        logger.debug("Dates after check: Not between {} and {}",
                     start.getTime(), stop.getTime());
        if (!startModified) {
          if (newDate.compareTo(stop) < 0) {
            logger.debug("newDate: {} Should not be between {} and {}",
                         newDate.getTime(), start.getTime(), stop.getTime());
            return false;
          }
        } else if (start.compareTo(newDate) < 0) {
          if (!stopModified || newDate.compareTo(stop) < 0) {
            logger.debug("newDate: {} Should not be between {} and {}",
                         newDate.getTime(), start.getTime(), stop.getTime());
            return false;
          }
        }
      } else if ("-between".equalsIgnoreCase(args[i])) {
        i++;
        betweenTest = true;
        final String[] elmts = args[i].split(";");
        boolean startModified = false;
        String[] values = elmts[0].split(":");
        Calendar start = getCalendar(values);
        if (start != null) {
          startModified = true;
        } else {
          start = Calendar.getInstance();
        }
        boolean stopModified = false;
        values = elmts[1].split(":");
        Calendar stop = getCalendar(values);
        if (stop != null) {
          stopModified = true;
        } else {
          stop = Calendar.getInstance();
        }
        logger.debug("Dates before check: Between {} and {}", start.getTime(),
                     stop.getTime());
        // Check that start < stop
        if (start.compareTo(stop) > 0) {
          // no so add 24H to stop
          stop.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Check that start and stop > newDate (only start is checked since start <= stop)
        if (start.compareTo(newDate) < 0) {
          start.add(Calendar.DAY_OF_MONTH, 1);
          stop.add(Calendar.DAY_OF_MONTH, 1);
        }
        logger.debug("Dates before check: Between {} and {}", start.getTime(),
                     stop.getTime());
        if (!startModified) {
          if (newDate.compareTo(stop) < 0) {
            logger.debug("newDate: {} is between {} and {}", newDate.getTime(),
                         start.getTime(), stop.getTime());
            betweenResult = true;
          }
        } else if (start.compareTo(newDate) < 0) {
          if (!stopModified || newDate.compareTo(stop) < 0) {
            logger.debug("newDate: {} is between {} and {}", newDate.getTime(),
                         start.getTime(), stop.getTime());
            betweenResult = true;
          }
        }
      }
    }
    if (betweenTest) {
      logger.debug(
          "Since between is specified, do we found newDate: {} Result: {}",
          newDate.getTime(), betweenResult);
      return betweenResult;
    }
    logger.debug("newDate: {} rescheduled", newDate.getTime());
    return true;
  }

  /**
   * @param values as X+n or X-n or X=n or Xn where X=Y/M/D/H/m/s,
   *     n=number
   *     and +/- meaning adding/subtracting
   *     from current date and = meaning specific set value
   *
   * @return the Calendar if any specification, or null if no calendar
   *     specified
   */
  private Calendar getCalendar(final String[] values) {
    final Calendar newcal = Calendar.getInstance();
    boolean isModified = false;
    for (final String value2 : values) {
      if (value2.length() > 1) {
        int addvalue = 0; // will be different of 0
        int value = -1; // will be >= 0
        switch (value2.charAt(0)) {
          case '+':
            try {
              addvalue = Integer.parseInt(value2.substring(2));
            } catch (final NumberFormatException e) {
              continue;
            }
            break;
          case '-':
            try {
              addvalue = Integer.parseInt(value2.substring(1));
            } catch (final NumberFormatException e) {
              continue;
            }
            break;
          case '=':
            try {
              value = Integer.parseInt(value2.substring(2));
            } catch (final NumberFormatException e) {
              continue;
            }
            break;
          default: // no sign
            try {
              value = Integer.parseInt(value2.substring(1));
            } catch (final NumberFormatException e) {
              continue;
            }
        }
        switch (value2.charAt(0)) {
          case 'Y':
            if (value >= 0) {
              newcal.set(Calendar.YEAR, value);
            } else {
              newcal.add(Calendar.YEAR, addvalue);
            }
            isModified = true;
            break;
          case 'M':
            if (value >= 0) {
              newcal.set(Calendar.MONTH, value);
            } else {
              newcal.add(Calendar.MONTH, addvalue);
            }
            isModified = true;
            break;
          case 'D':
            if (value >= 0) {
              newcal.set(Calendar.DAY_OF_MONTH, value);
            } else {
              newcal.add(Calendar.DAY_OF_MONTH, addvalue);
            }
            isModified = true;
            break;
          case 'H':
            if (value >= 0) {
              newcal.set(Calendar.HOUR_OF_DAY, value);
            } else {
              newcal.add(Calendar.HOUR_OF_DAY, addvalue);
            }
            isModified = true;
            break;
          case 'm':
            if (value >= 0) {
              newcal.set(Calendar.MINUTE, value);
            } else {
              newcal.add(Calendar.MINUTE, addvalue);
            }
            isModified = true;
            break;
          case 'S':
            if (value >= 0) {
              newcal.set(Calendar.SECOND, value);
            } else {
              newcal.add(Calendar.SECOND, addvalue);
            }
            isModified = true;
            break;
          default:
            // nothing
        }
      }
    }
    if (isModified) {
      return newcal;
    }
    return null;
  }
}
