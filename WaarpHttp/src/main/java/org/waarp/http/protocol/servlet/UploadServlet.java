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

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.http.protocol.HttpHelper;
import org.waarp.http.protocol.HttpResumableInfo;
import org.waarp.http.protocol.HttpResumableSession;
import org.waarp.http.protocol.HttpSessions;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Upload Servlet: enables uploading file from Web Browser from final user
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024)
public class UploadServlet extends AbstractServlet {
  private static final long serialVersionUID = 2003L;
  public static final String RESUMABLE_CHUNK_NUMBER = "resumableChunkNumber";
  public static final String RESUMABLE_CHUNK_SIZE = "resumableChunkSize";
  public static final String RESUMABLE_TOTAL_SIZE = "resumableTotalSize";
  public static final String RESUMABLE_IDENTIFIER = "resumableIdentifier";
  public static final String RESUMABLE_FILENAME = "resumableFilename";
  public static final String RESUMABLE_RELATIVE_PATH = "resumableRelativePath";
  public static final String FIELD_FILE = "file";
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(UploadServlet.class);
  private static final MultipartConfigElement MULTI_PART_CONFIG =
      new MultipartConfigElement("/tmp");
  private static final String __MULTIPART_CONFIG_ELEMENT =
      "org.eclipse.jetty.multipartConfig";
  protected static final String SHA_256 = "sha256";
  protected static final String INVALID_BLOCK = "Invalid block.";


  @Override
  protected void doPost(final HttpServletRequest request,
                        final HttpServletResponse response)
      throws ServletException {
    // Check that we have a file upload request
    final boolean isMultipart =
        request.getHeader("Content-Type").contains("multipart/");
    InputStream inputStream = null;
    final HttpResumableInfo resumableInfo;
    final HttpResumableSession session;
    final Map<String, String> arguments = new HashMap<String, String>();
    logger.debug("MULTIPART MODE? {} {}", isMultipart,
                 request.getHeader("Content-Type"));
    if (isMultipart) {
      if (request.getAttribute(__MULTIPART_CONFIG_ELEMENT) == null) {
        request.setAttribute(__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
      }
      try {
        final Collection<Part> parts = request.getParts();
        if (parts.isEmpty()) {
          logger.warn("MULTIPART MODE BUT EMPTY");
          inputStream = request.getInputStream();
        } else {
          for (final Part part : parts) {

            if (part.getName().equalsIgnoreCase(FIELD_FILE)) {
              inputStream = part.getInputStream();
            } else {
              final InputStream finalInputStream = part.getInputStream();
              final ByteSource byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                  return finalInputStream;
                }
              };

              final String valueText =
                  byteSource.asCharSource(WaarpStringUtils.UTF8).read();
              arguments.put(part.getName(), valueText);
            }
          }
        }
      } catch (final ServletException ignored) {
        logger.warn("MULTIPART MODE BUT error", ignored);
        try {
          inputStream = request.getInputStream();
        } catch (final IOException ignore) {
          throw new ServletException(INVALID_BLOCK, ignore);
        }
      } catch (final IOException ignored) {
        logger.warn("MULTIPART MODE BUT error", ignored);
        try {
          inputStream = request.getInputStream();
        } catch (final IOException ignore) {
          throw new ServletException(INVALID_BLOCK, ignore);
        }
      }
    } else {
      final Enumeration<String> names = request.getParameterNames();
      while (names.hasMoreElements()) {
        final String name = names.nextElement();
        arguments.put(name, request.getParameter(name));
      }
      try {
        inputStream = request.getInputStream();
      } catch (final IOException ignore) {
        throw new ServletException(INVALID_BLOCK, ignore);
      }
    }
    logger.warn("PARAMS: {}", arguments);
    resumableInfo = getResumableInfo(arguments);
    logger.debug("RECV: {}", resumableInfo);
    session = getResumableSession(arguments, resumableInfo);//NOSONAR
    logger.debug("SESSION: {}", session);
    try {
      if (!session.tryWrite(resumableInfo, inputStream)) {
        throw new ServletException(INVALID_BLOCK);
      }
    } catch (final IOException e) {
      throw new ServletException(INVALID_BLOCK, e);
    }

    String sha = arguments.get(SHA_256);
    if (!Strings.isNullOrEmpty(sha) && sha.equalsIgnoreCase("undefined")) {
      sha = null;
    }
    if (session.checkIfUploadFinished(sha)) {
      // Check if all chunks uploaded, and change filename
      logger.warn("ALL USER TRANSFER FINISHED: {}", session);
      HttpSessions.getInstance().removeSession(session);
      response.setStatus(200);
      try {
        response.getWriter().print("All finished.");
      } catch (final IOException ignore) {
        logger.debug(ignore);
      }
    } else {
      logger.debug("PARTIAL UPLOAD: {}", session);
      response.setStatus(201);
      try {
        response.getWriter().print("Upload");
      } catch (final IOException ignore) {
        logger.debug(ignore);
      }
    }

  }

  /**
   * From the HttpServletRequest, build the HttpResumableInfo
   *
   * @param arguments Map of arguments
   *
   * @return the HttpResumableInfo
   */
  private HttpResumableInfo getResumableInfo(
      final Map<String, String> arguments) {
    final int resumableChunkNumber =
        HttpHelper.toInt(arguments.get(RESUMABLE_CHUNK_NUMBER), -1);
    final int resumableChunkSize =
        HttpHelper.toInt(arguments.get(RESUMABLE_CHUNK_SIZE), -1);
    final long resumableTotalSize =
        HttpHelper.toLong(arguments.get(RESUMABLE_TOTAL_SIZE), -1);
    final String resumableIdentifier = arguments.get(RESUMABLE_IDENTIFIER);
    final String resumableFilename = arguments.get(RESUMABLE_FILENAME);
    final String resumableRelativePath = arguments.get(RESUMABLE_RELATIVE_PATH);
    return new HttpResumableInfo(resumableChunkNumber, resumableChunkSize,
                                 resumableTotalSize, resumableIdentifier,
                                 resumableFilename, resumableRelativePath);
  }

  /**
   * From the HttpServletRequest and the HttpResumableInfo, build the
   * HttpResumableSession
   *
   * @param arguments Map of arguments
   * @param resumableInfo
   *
   * @return the HttpResumableSession
   *
   * @throws ServletException
   */
  private HttpResumableSession getResumableSession(
      final Map<String, String> arguments,
      final HttpResumableInfo resumableInfo) throws ServletException {
    final String rulename = arguments.get(RULENAME);
    if (rulename == null) {
      throw new ServletException(INVALID_REQUEST_PARAMS);
    }
    String comment = arguments.get(COMMENT);
    if (comment == null) {
      comment = "Web Upload " + resumableInfo.getIdentifier();
    }
    final HttpSessions sessions = HttpSessions.getInstance();

    try {
      final HttpAuthent authent =
          (HttpAuthent) WaarpSystemUtil.newInstance(authentClass);
      authent.initializeAuthent(arguments);
      final HttpResumableSession session = sessions
          .getOrCreateResumableSession(resumableInfo, rulename, comment,
                                       authent);
      if (!session.valid(resumableInfo)) {
        sessions.removeSession(resumableInfo);
        throw new ServletException(INVALID_REQUEST_PARAMS);
      }
      return session;
    } catch (final IllegalArgumentException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (final IllegalAccessException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (final InstantiationException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (InvocationTargetException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    } catch (NoSuchMethodException e) {
      throw new ServletException(INVALID_REQUEST_PARAMS, e);
    }
  }

  @Override
  protected void doGet(final HttpServletRequest request,
                       final HttpServletResponse response)
      throws ServletException {
    final Map<String, String> arguments = new HashMap<String, String>();
    final Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      arguments.put(name, request.getParameter(name));
    }
    final HttpResumableInfo resumableInfo = getResumableInfo(arguments);
    logger.debug("RECVGET: {}", resumableInfo);
    final HttpResumableSession session =
        getResumableSession(arguments, resumableInfo);
    logger.debug("SESSION: {}", session);
    if (session.contains(resumableInfo)) {
      logger.info("ALREADY: {}", session);
      response.setStatus(200);
      try {
        response.getWriter().print("Uploaded."); //This Chunk has been Uploaded.
      } catch (final IOException ignore) {
        logger.debug(ignore);
      }
    } else {
      logger.info("NOTDONE: {}", session);
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
