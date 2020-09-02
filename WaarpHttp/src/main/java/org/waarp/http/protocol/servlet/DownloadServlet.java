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

package org.waarp.http.protocol.servlet;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.guid.LongUuid;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.http.protocol.HttpDownloadSession;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Download Servlet: enables downloading file from Web Browser to final user
 */
public class DownloadServlet extends AbstractServlet {
  private static final long serialVersionUID = 2002L;
  public static final String FILENAME = "filename";
  public static final String IDENTIFIER = "identifier";
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DownloadServlet.class);
  public static final String X_HASH_SHA_256 = "X-Hash-Sha-256";

  @Override
  protected void doPost(final HttpServletRequest request,
                        final HttpServletResponse response)
      throws ServletException {
    doGet(request, response);
  }

  @Override
  protected void doHead(final HttpServletRequest request,
                        final HttpServletResponse response)
      throws ServletException, IOException {
    final Map<String, String> arguments = new HashMap<String, String>();
    final Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      arguments.put(name, request.getParameter(name));
    }
    final String filename = getFilename(arguments);
    logger.debug("RECVHEAD: {}", filename);
    final HttpDownloadSession session =
        getDownloadSession(arguments, filename, true);//NOSONAR
    logger.debug("RECVHEAD SESSION: {}", session);
    logger.debug("Check on going");
    response.setHeader("Expires", "0");
    response.setHeader("Cache-Control",
                       "must-revalidate, post-check=0, " + "pre-check=0");
    if (session == null) {
      logger.debug("Not found");
      response.setStatus(404);
    } else if (session.isTransmitting()) {
      logger.debug("On going");
      response.setStatus(202);
    } else if (session.isFinished()) {
      logger.debug("Done");
      response.setHeader(X_HASH_SHA_256, session.getComputedHadh());
      response.setStatus(200);
    }
  }

  @Override
  protected void doGet(final HttpServletRequest request,
                       final HttpServletResponse response)
      throws ServletException {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      final Map<String, String> arguments = new HashMap<String, String>();
      final Enumeration<String> names = request.getParameterNames();
      while (names.hasMoreElements()) {
        final String name = names.nextElement();
        arguments.put(name, request.getParameter(name));
      }
      final String filename = getFilename(arguments);
      logger.debug("RECVGET: {}", filename);
      final HttpDownloadSession session =
          getDownloadSession(arguments, filename, false);
      logger.debug("SESSION: {}", session);
      final Callable<String> hashCompute = new Callable<String>() {
        @Override
        public String call() {
          return session.getHash();
        }
      };
      final Future<String> future = executor.submit(hashCompute);
      response.setHeader("Content-Disposition",
                         "attachment; filename=\"" + session.getFinalName() +
                         "\"");
      response.setHeader("Content-Description", "File Transfer");
      response.setHeader("Content-Type", "application/octet-stream");
      response.setHeader("Content-Transfer-Encoding", "binary");
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control",
                         "must-revalidate, post-check=0, " + "pre-check=0");
      // Used by javascript downloader
      final Cookie cookie = new Cookie("fileDownload", "true");
      cookie.setHttpOnly(true);
      cookie.setSecure(true);
      response.addCookie(cookie);
      response
          .setHeader("Content-Length", Long.toString(session.getFileSize()));
      String hash = null;
      try {
        hash = future.get();
      } catch (final InterruptedException e) {//NOSONAR
        logger.debug(e);
      } catch (final ExecutionException e) {
        logger.debug(e);
      }
      if (hash != null) {
        response.setHeader(X_HASH_SHA_256, hash);

      }
      try {
        if (session.tryWrite(response.getOutputStream())) {
          session.downloadFinished();
          logger.info("Download OK: {}", session);
          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          logger.info("NOT FOUND: {}", session);
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      } catch (final IOException e) {
        logger.error("Error: {}", session, e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } finally {
      executor.shutdown();
    }
  }

  private String getFilename(final Map<String, String> arguments) {
    return arguments.get(FILENAME);
  }

  private HttpDownloadSession getDownloadSession(
      final Map<String, String> arguments, final String filename,
      final boolean check) throws ServletException {
    final String rulename = arguments.get(RULENAME);
    if (rulename == null) {
      throw new ServletException(INVALID_REQUEST_PARAMS);
    }
    String identifier = arguments.get(IDENTIFIER);
    if (identifier == null) {
      identifier = new LongUuid().getLong() + "";
    }
    String comment = arguments.get(COMMENT);
    if (comment == null) {
      comment = "Web Download " + identifier;
    }

    try {
      final HttpAuthent authent = authentClass.newInstance();
      authent.initializeAuthent(arguments);
      if (check) {
        try {
          return new HttpDownloadSession(identifier, authent);
        } catch (final WaarpDatabaseException e) {
          logger.debug(e);
          return null;
        }
      }
      return new HttpDownloadSession(filename, rulename, identifier, comment,
                                     authent);
    } catch (final IllegalArgumentException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (final IllegalAccessException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (final InstantiationException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    }
  }
}
