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
package org.waarp.openr66.context.task.javatask;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.guid.GUID;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.task.AbstractExecJavaTask;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Add an UUID in the TransferInformation to the current Task.</br>
 * This should be called on caller side in pre-task since the transfer
 * information will be transfered just
 * after.</br>
 * The second argument is -1 = added in front, +1 = added at last, default
 * being
 * -1.</br>
 * The third argument, optional, is "-format" followed by a string containing
 * "#UUID#" to be replaced by the
 * uuid and starting with - or +, meaning this will be added at the beginning
 * or
 * the end of the generated new
 * string. Default is equivalent to "-format -##UUID##". </br>
 * To be called as: <task><type>EXECJAVA</type><path>org.waarp.openr66.context.task.javatask.AddUuidJavaTask
 * [-format (-/+)##UUID##]</path></task>
 */
public class AddUuidJavaTask extends AbstractExecJavaTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AddUuidJavaTask.class);
  private static final String sUUID = "#UUID#";
  private static final Pattern UUID_COMPILE =
      Pattern.compile(sUUID, Pattern.LITERAL);

  @Override
  public void run() {
    logger.debug(toString());
    final GUID uuid = new GUID();
    final String[] args = BLANK.split(fullarg);
    String fileInfo;
    String format = "-##UUID##";
    int way = -1;
    for (int i = 0; i < args.length; i++) {
      if ("-format".equalsIgnoreCase(args[i])) {
        format = args[i + 1];
        if (format.charAt(0) == '-') {
          way = -1;
          format = format.substring(1);
        } else if (format.charAt(0) == '+') {
          way = 1;
          format = format.substring(1);
        }
        i++;
      }
    }
    logger.debug("Replace UUID in {} way: {}", format, way);
    if (way < 0) {
      fileInfo = UUID_COMPILE.matcher(format).replaceAll(
          Matcher.quoteReplacement(uuid.toString())) + ' ' +
                 session.getRunner().getFileInformation();
    } else {
      fileInfo = session.getRunner().getFileInformation() + ' ' +
                 UUID_COMPILE.matcher(format).replaceAll(
                     Matcher.quoteReplacement(uuid.toString()));
    }
    session.getRunner().setFileInformation(fileInfo);
    session.getRunner().addToTransferMap("uuid", uuid.toString());
    try {
      session.getRunner().update();
    } catch (final WaarpDatabaseException e) {
      logger.error("UUID cannot be saved to fileInformation:" + fileInfo);
      status = 2;
      return;
    }
    logger.debug("UUID saved to fileInformation:" + fileInfo);
    status = 0;
  }
}
