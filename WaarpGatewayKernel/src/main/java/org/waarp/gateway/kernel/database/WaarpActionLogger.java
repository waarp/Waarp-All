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

/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author
 * tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual
 * contributors.
 * <p>
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 3.0 of the
 * License, or (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.gateway.kernel.database;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.database.data.DbTransferLog;
import org.waarp.gateway.kernel.session.HttpSession;

/**
 * Class to help to log any actions through the interface of Waarp
 *
 *
 */
public class WaarpActionLogger {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpActionLogger.class);

  /**
   * Log the action
   *
   * @param dbSession
   * @param message
   * @param session
   */
  public static void logCreate(DbSession dbSession, String message,
                               HttpSession session) {
    final String sessionContexte = session.toString();
    logger.info(message + " " + sessionContexte);
    if (dbSession != null) {
      final PageRole code = session.getCurrentCommand();
      boolean isSender = false;
      switch (code) {
        case ERROR:
        case HTML:
        case MENU:
          session.setLogid(DbConstant.ILLEGALVALUE);
          return;
        case DELETE:
        case GETDOWNLOAD:
          isSender = false;
          break;
        case POST:
        case POSTUPLOAD:
        case PUT:
          isSender = true;
          break;
        default:
          break;
      }
      // Insert new one
      try {
        final DbTransferLog log =
            new DbTransferLog(dbSession, session.getAuth().getUser(),
                              session.getAuth().getAccount(),
                              org.waarp.common.database.DbConstant.ILLEGALVALUE,
                              isSender, session.getFilename(), code.name(),
                              HttpResponseStatus.OK, message,
                              UpdatedInfo.TOSUBMIT);
        logger.debug("Create FS: " + log);
        session.setLogid(log.getSpecialId());
        return;
      } catch (final WaarpDatabaseException e1) {
        // Do nothing
      }
      /*
       * if (FileBasedConfiguration.fileBasedConfiguration.monitoring != null) { if (isSender) {
       * FileBasedConfiguration.fileBasedConfiguration.monitoring .updateLastOutBand(); } else {
       * FileBasedConfiguration.fileBasedConfiguration.monitoring .updateLastInBound(); } }
       */
    }
    session.setLogid(org.waarp.common.database.DbConstant.ILLEGALVALUE);
  }

  /**
   * Log the action
   *
   * @param dbSession
   * @param session
   * @param message
   * @param rcode
   * @param info
   */
  public static void logAction(DbSession dbSession, HttpSession session,
                               String message, HttpResponseStatus rcode,
                               UpdatedInfo info) {
    final String sessionContexte = session.toString();
    final long specialId = session.getLogid();
    logger.info(message + " " + sessionContexte);
    if (dbSession != null &&
        specialId != org.waarp.common.database.DbConstant.ILLEGALVALUE) {
      final PageRole code = session.getCurrentCommand();
      switch (code) {
        case ERROR:
        case HTML:
        case MENU:
          return;
        case DELETE:
        case GETDOWNLOAD:
        case POST:
        case POSTUPLOAD:
        case PUT:
          break;
        default:
          return;
      }
      try {
        // Try load
        final DbTransferLog log =
            new DbTransferLog(dbSession, session.getAuth().getUser(),
                              session.getAuth().getAccount(), specialId);
        log.changeUpdatedInfo(info);
        log.setInfotransf(message);
        log.setReplyCodeExecutionStatus(rcode);
        log.update();
        logger.debug("Update FS: " + log);
        session.setLogid(log.getSpecialId());
        return;
      } catch (final WaarpDatabaseException e) {
        // Do nothing
      }
    }
  }

  /**
   * Log the action in error
   *
   * @param dbSession
   * @param session
   * @param message
   * @param rcode
   */
  public static void logErrorAction(DbSession dbSession, HttpSession session,
                                    String message, HttpResponseStatus rcode) {
    final String sessionContexte = session.toString();
    final long specialId = session.getLogid();
    logger.error(rcode.code() + ":" + message + " " + sessionContexte);
    logger.warn("To Change to debug Log", new Exception("Log"));
    if (dbSession != null &&
        specialId != org.waarp.common.database.DbConstant.ILLEGALVALUE) {
      final PageRole code = session.getCurrentCommand();
      switch (code) {
        case ERROR:
        case HTML:
        case MENU:
          return;
        case DELETE:
        case GETDOWNLOAD:
        case POST:
        case POSTUPLOAD:
        case PUT:
          break;
        default:
          return;
      }
      final UpdatedInfo info = UpdatedInfo.INERROR;
      try {
        // Try load
        final DbTransferLog log =
            new DbTransferLog(dbSession, session.getAuth().getUser(),
                              session.getAuth().getAccount(), specialId);
        log.changeUpdatedInfo(info);
        log.setInfotransf(message);
        if (rcode.code() < 400) {
          log.setReplyCodeExecutionStatus(HttpResponseStatus.BAD_REQUEST);
        } else {
          log.setReplyCodeExecutionStatus(rcode);
        }
        if (session.getFilename() != null) {
          log.setFilename(session.getFilename());
        }
        log.update();
        logger.debug("Update FS: " + log);
      } catch (final WaarpDatabaseException e) {
        // Do nothing
      }
    }
  }
}
