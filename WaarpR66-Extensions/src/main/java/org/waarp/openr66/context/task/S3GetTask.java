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

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.s3.WaarpR66S3Client;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory.S3TaskType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * S3 GET Task<br>
 * <br>
 * The Rule shall be using SENDMD5THROUGHMODE or SENDTHROUGHMODE mode
 * (respectively 7 or 5) or RECVMD5THROUGHMODE or RECVTHROUGHMODE mode
 * (respectively 8 or 6) as pre-tasks since the file does not exist at
 * startup.<br>
 * <p>
 * Result of arguments will be as a S3 GET command.<br>
 * Format is the following:<br>
 * "-URL url of S3 service <br>
 * -accessKey access Key for S3 service <br>
 * -secretKey secret Key for S3 service <br>
 * -bucketName bucket Name where to retrieve the object <br>
 * -sourceName source Name from the bucket to select the final Object <br>
 * -file final File path (absolute or relative from IN path) <br>
 * [-getTags [* or list of tag names comma separated without space]]" <br>
 * <br>
 * <br>
 * The order of actions will be:<br>
 * 1) connection to S3 service using access Key and Secret Key<br>
 * 2) Retrieve from the bucket the source Object and store it in the file specified<br>
 * 3) if getTags is true, the informations are added to transferInfo and fileInfo<br>
 * 4) the current File is set to this new GET file<br>
 * 5) the sender send an update on file information (name and size)<br>
 */
public class S3GetTask extends S3AbstractTask {
  private static final S3TaskFactory.S3TaskType taskType =
      S3TaskFactory.S3TaskType.S3GET;

  /**
   * Constructor
   *
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public S3GetTask(final String argRule, final int delay,
                   final String argTransfer, final R66Session session) {
    super(TaskType.EXTENDED, delay, argRule, argTransfer, session);
  }

  /**
   * The order of actions will be:<br>
   * 1) connection to S3 service using access Key and Secret Key<br>
   * 2) Retrieve from the bucket the source Object and store it in the file specified<br>
   * 3) if getTags is true, the informations are added to transferInfo and fileInfo<br>
   * 4) the current File is set to this new GET file<br>
   * 5) the sender send an update on file information (name and size)<br>
   */
  @Override
  public void run() {
    try {
      internalRun();
      // The update will be done after PRE task done
      futureCompletion.setSuccess();
    } catch (final OpenR66ProtocolNetworkException | CommandAbstractException | WaarpDatabaseException e) {
      finalizeInError(e, "Error while S3 Action");
    }
  }

  void internalRun()
      throws OpenR66ProtocolNetworkException, CommandAbstractException,
             WaarpDatabaseException {
    if (!getParams()) {
      throw new OpenR66ProtocolNetworkException(
          "Error while Parameters parsing");
    }
    final WaarpR66S3Client s3Client =
        new WaarpR66S3Client(taskUtil.getAccessKey(), taskUtil.getSecretKey(),
                             taskUtil.getUrl());
    Map<String, String> map =
        s3Client.getFile(taskUtil.getBucketName(), taskUtil.getSourceName(),
                         taskUtil.getFile(), taskUtil.getGetTag());
    final File file = taskUtil.getFile();
    final String filePath = file.getAbsolutePath();
    if (!file.canRead()) {
      throw new OpenR66ProtocolNetworkException(
          "File was not received in " + filePath);
    }
    final DbTaskRunner runner = session.getRunner();
    if (taskUtil.getGetTag() && map != null && !map.isEmpty() &&
        runner != null) {
      final Set<String> keySet = taskUtil.getGetTags();
      if (keySet != null && !keySet.isEmpty()) {
        final Map<String, String> map2 = new HashMap<>(keySet.size());
        for (final String key : keySet) {
          final String value = map.get(key);
          if (value != null) {
            map2.put(key, value);
          }
        }
        if (map2.isEmpty()) {
          map = null;
        } else {
          map = map2;
        }
      }
      if (map != null && !map.isEmpty()) {
        runner.setTransferMap(map);
      }
    }

    final RenameTask renameTask = new RenameTask(filePath, 0, "", session);
    renameTask.run();
    if (renameTask.futureCompletion.isSuccess()) {
      final LocalChannelReference lcr = session.getLocalChannelReference();
      if (lcr != null && lcr.isSendThroughMode()) {
        lcr.setSendThroughMode(false);
      }
      if (runner != null && runner.isSendThrough()) {
        runner.checkThroughMode();
      }
      logger.debug("GET {} RENAME to {}", taskUtil.getSourceName(),
                   taskUtil.getFile().getAbsolutePath());
      futureCompletion.setSuccess();
    } else {
      logger.error("GET file OK but local file renaming in error");
      futureCompletion.setResult(renameTask.futureCompletion.getResult());
      futureCompletion.setFailure(renameTask.futureCompletion.getCause());
    }
  }

  @Override
  public S3TaskType getS3TaskType() {
    return taskType;
  }
}
