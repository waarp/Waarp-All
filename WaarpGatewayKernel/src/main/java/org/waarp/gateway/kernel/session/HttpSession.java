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
package org.waarp.gateway.kernel.session;

import io.netty.handler.codec.http.HttpMethod;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileParameterInterface;
import org.waarp.common.file.Restart;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedOptsMLSxImpl;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.commonfile.CommonDirImpl;
import org.waarp.gateway.kernel.commonfile.FilesystemBasedFileParameterImpl;
import org.waarp.gateway.kernel.database.DbConstantGateway;

/**
 *
 */
public class HttpSession implements SessionInterface {
  protected HttpAuthInterface httpAuth;
  protected long logid = DbConstantGateway.ILLEGALVALUE;
  protected CommonDirImpl dir;
  HttpMethod method;
  private String cookieSession;
  private PageRole currentCommand;
  protected String filename;

  /**
   *
   */
  public HttpSession() {
    // nothing
  }

  /**
   * @return the method
   */
  public final HttpMethod getMethod() {
    return method;
  }

  /**
   * @param method the method to set
   */
  public final void setMethod(final HttpMethod method) {
    this.method = method;
  }

  /**
   * @param httpAuth the httpAuth to set
   */
  public final void setHttpAuth(final HttpAuthInterface httpAuth) {
    this.httpAuth = httpAuth;
    dir = new CommonDirImpl(this, new FilesystemBasedOptsMLSxImpl());
    try {
      dir.changeDirectoryNotChecked(httpAuth.getUser());
      dir.changeDirectoryNotChecked(httpAuth.getAccount());
    } catch (final CommandAbstractException ignored) {
      // nothing
    }
  }

  @Override
  public final HttpAuthInterface getAuth() {
    return httpAuth;
  }

  @Override
  public final void clear() {
    if (httpAuth != null) {
      httpAuth.clear();
    }
  }

  @Override
  public final int getBlockSize() {
    return 8192; // HttpChunk size
  }

  @Override
  public final FileParameterInterface getFileParameter() {
    return FilesystemBasedFileParameterImpl.fileParameterInterface;
  }

  @Override
  public final Restart getRestart() {
    return null;
  }

  @Override
  public final String getUniqueExtension() {
    return ".postu";
  }

  /**
   * @return the logid
   */
  public final long getLogid() {
    return logid;
  }

  /**
   * @param logid the logid to set
   */
  public final void setLogid(final long logid) {
    this.logid = logid;
  }

  @Override
  public final DirInterface getDir() {
    return dir;
  }

  /**
   * @return the currentCommand
   */
  public final PageRole getCurrentCommand() {
    return currentCommand;
  }

  /**
   * @param currentCommand the currentCommand to set
   */
  public final void setCurrentCommand(final PageRole currentCommand) {
    this.currentCommand = currentCommand;
  }

  /**
   * @return the cookieSession
   */
  public final String getCookieSession() {
    return cookieSession;
  }

  /**
   * @param cookieSession the cookieSession to set
   */
  public final void setCookieSession(final String cookieSession) {
    this.cookieSession = cookieSession;
  }

  /**
   * @return the filename
   */
  public final String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public final void setFilename(final String filename) {
    this.filename = filename;
  }

  @Override
  public String toString() {
    return "Command: " + currentCommand.name() + " Filename: " + filename +
           " LogId: " + logid;
  }
}
