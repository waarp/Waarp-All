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

import org.waarp.common.file.AbstractDir;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerException;

import java.io.File;
import java.util.regex.Pattern;

/**
 * This task allows to change the mode of the file (as in Unix CHMOD command)
 * according to the following
 * argument:<br>
 * - the full path is get from the current file<br>
 * - the arg path is transformed as usual (static and dynamic from information
 * transfer)<br>
 * - this final path arg should be of the form [ua][+-=][rwx] where multiple
 * elements could be specified,
 * separated by blank character<br>
 * - u/a meaning user(Waarp system user)/all (group and other do not exist in
 * Java), +/-/= meaning
 * add/remove/set (set means all other values are removed), r/w/x meaning
 * Read/Write/Execute<br>
 * <br>
 * For instance, the final path arg could be:<br>
 * <ul>
 * <li>u=rwx a=r</li>
 * <li>ua+rw</li>
 * <li>u=rw a-wx</li>
 * <li>a+rw</li>
 * </ul>
 * Current access mode for the application will be applied as default "user"
 * access mode, and no access mode
 * (-rwx) will be the default for "all" access mode.<br>
 * If several access mode are set in sequence, the result will be the sum of the
 * results, step by step.<br>
 * "a=r a+w a-r" will result in a=w (first step is r--, second step is rw-,
 * third step is -w-)
 */
public class ChModTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ChModTask.class);
  private static final Pattern BLANK = Pattern.compile(" ");

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public ChModTask(String argRule, int delay, String argTransfer,
                   R66Session session) {
    super(TaskType.CHMOD, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    String finalname = argRule;
    finalname = AbstractDir
        .normalizePath(getReplacedValue(finalname, BLANK.split(argTransfer)))
        .trim().toLowerCase();
    logger.info("ChMod with arg " + finalname + " from {}", session);
    final File file = session.getFile().getTrueFile();
    boolean user;
    boolean all;
    boolean isall = false;
    boolean ur;
    boolean uw;
    boolean ux;
    boolean ar = false;
    boolean aw = false;
    boolean ax = false;
    ur = file.canRead();
    uw = file.canWrite();
    ux = file.canExecute();
    final String[] chmods = BLANK.split(finalname);
    for (final String chmod : chmods) {
      user = chmod.indexOf('u') >= 0;
      all = chmod.indexOf('a') >= 0;
      if (!user && !all) {
        // ignore
        continue;
      }
      isall |= all;
      boolean isp = false;
      boolean ism = false;
      boolean ise = false;
      if (chmod.indexOf('=') >= 0) {
        ise = true;
      } else if (chmod.indexOf('+') >= 0) {
        isp = true;
      } else if (chmod.indexOf('-') >= 0) {
        ism = true;
      } else {
        // ignore
        continue;
      }
      boolean isr, isw, isx;
      isr = chmod.indexOf('r') >= 0;
      isw = chmod.indexOf('w') >= 0;
      isx = chmod.indexOf('x') >= 0;
      if (!isr && !isw && !isx) {
        // ignore
        continue;
      }
      if (user) {
        ur = ise && isr || isp && (isr || ur) || ism && !isr && ur;
        uw = ise && isw || isp && (isw || uw) || ism && !isw && uw;
        ux = ise && isx || isp && (isx || ux) || ism && !isx && ux;
      }
      if (all) {
        ar = ise && isr || isp && (isr || ar) || ism && !isr && ar;
        aw = ise && isw || isp && (isw || aw) || ism && !isw && aw;
        ax = ise && isx || isp && (isx || ax) || ism && !isx && ax;
      }
    }
    boolean result = true;
    if (isall) {
      result &= file.setReadable(ar, false);
      result &= file.setWritable(aw, false);
      result &= file.setExecutable(ax, false);
    }
    result &= file.setReadable(ur, true);
    result &= file.setWritable(uw, true);
    result &= file.setExecutable(ux, true);
    if (result) {
      futureCompletion.setSuccess();
    } else {
      logger.error(
          "ChMod " + finalname + " on file : " + file + "     " + session);
      futureCompletion.setFailure(
          new OpenR66RunnerException("Chmod not fully applied on File"));
    }
  }

}
