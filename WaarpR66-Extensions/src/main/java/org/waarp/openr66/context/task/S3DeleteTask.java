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

import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.s3.WaarpR66S3Client;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory.S3TaskType;

/**
 * S3 DELETE Task<br>
 * <p>
 * Result of arguments will be as S3 DELETE command.<br>
 * Format is the following:<br>
 * "-URL url of S3 service <br>
 * -accessKey access Key for S3 service <br>
 * -secretKey secret Key for S3 service <br>
 * -bucketName bucket Name where to retrieve the object <br>
 * -sourceName source Name from the bucket to delete the final Object <br>
 * <br>
 * <br>
 * The order of actions will be:<br>
 * 1) connection to S3 service using access Key and Secret Key<br>
 * 2) Delete from the bucket the source Object<br>
 * 3) the current File is unchanged<br>
 */
public class S3DeleteTask extends S3AbstractTask {
  private static final S3TaskFactory.S3TaskType taskType =
      S3TaskFactory.S3TaskType.S3DELETE;

  /**
   * Constructor
   *
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public S3DeleteTask(final String argRule, final int delay,
                      final String argTransfer, final R66Session session) {
    super(TaskType.EXTENDED, delay, argRule, argTransfer, session);
  }

  /**
   * The order of actions will be:<br>
   * 1) connection to S3 service using access Key and Secret Key<br>
   * 2) Delete from the bucket the source Object<br>
   * 3) the current File is unchanged<br>
   */
  @Override
  public void run() {
    if (!getParams()) {
      return;
    }
    final WaarpR66S3Client s3Client =
        new WaarpR66S3Client(taskUtil.getAccessKey(), taskUtil.getSecretKey(),
                             taskUtil.getUrl());
    try {
      s3Client.deleteFile(taskUtil.getBucketName(), taskUtil.getSourceName());
      logger.debug("DELETED {}", taskUtil.getSourceName());
      futureCompletion.setSuccess();
    } catch (final OpenR66ProtocolNetworkException e) {
      finalizeInError(e, "Error while S3 Action");
    }
  }

  @Override
  public final S3TaskType getS3TaskType() {
    return taskType;
  }
}
