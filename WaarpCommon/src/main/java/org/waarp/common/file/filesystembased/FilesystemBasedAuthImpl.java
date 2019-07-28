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
package org.waarp.common.file.filesystembased;

import org.waarp.common.command.NextCommandReply;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.AuthInterface;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.SessionInterface;

import java.util.regex.Pattern;

/**
 * Authentication implementation for Filesystem Based
 */
public abstract class FilesystemBasedAuthImpl implements AuthInterface {
  /**
   * DOUBLE SLASH pattern
   */
  private static final Pattern DOUBLE_SLASH = Pattern.compile("//");
  /**
   * User name
   */
  protected String user;

  /**
   * Password
   */
  protected String password;

  /**
   * Is Identified
   */
  protected boolean isIdentified;

  /**
   * SessionInterface
   */
  protected final SessionInterface session;

  /**
   * Relative Path after Authentication
   */
  protected String rootFromAuth;

  /**
   * @param session
   */
  protected FilesystemBasedAuthImpl(SessionInterface session) {
    this.session = session;
    isIdentified = false;
  }

  /**
   * @return the session
   */
  @Override
  public SessionInterface getSession() {
    return session;
  }

  /**
   * Set the user according to any implementation and could set the
   * rootFromAuth. If NOOP is returned,
   * isIdentifed must be TRUE.
   *
   * @param user
   *
   * @return (NOOP, 230) if the user is OK, else return the following command
   *     that must follow (usually PASS) and
   *     the associated reply
   *
   * @throws Reply421Exception if there is a problem during the
   *     authentication
   * @throws Reply530Exception if there is a problem during the
   *     authentication
   */
  protected abstract NextCommandReply setBusinessUser(String user)
      throws Reply421Exception, Reply530Exception;

  /**
   * @param user the user to set
   *
   * @return (NOOP, 230) if the user is OK, else return the following command
   *     that must follow (usually PASS) and
   *     the associated reply
   *
   * @throws Reply421Exception if there is a problem during the
   *     authentication
   * @throws Reply530Exception if there is a problem during the
   *     authentication
   */
  @Override
  public NextCommandReply setUser(String user)
      throws Reply421Exception, Reply530Exception {
    final NextCommandReply next = setBusinessUser(user);
    this.user = user;
    if (next.reply == ReplyCode.REPLY_230_USER_LOGGED_IN) {
      setRootFromAuth();
      session.getDir().initAfterIdentification();
    }
    return next;
  }

  /**
   * @return the user
   */
  @Override
  public String getUser() {
    return user;
  }

  /**
   * Set the password according to any implementation and could set the
   * rootFromAuth. If NOOP is returned,
   * isIdentifed must be TRUE.
   *
   * @param password
   *
   * @return (NOOP, 230) if the Password is OK, else return the following
   *     command that must follow (usually ACCT)
   *     and the associated reply
   *
   * @throws Reply421Exception if there is a problem during the
   *     authentication
   * @throws Reply530Exception if there is a problem during the
   *     authentication
   */
  protected abstract NextCommandReply setBusinessPassword(String password)
      throws Reply421Exception, Reply530Exception;

  /**
   * @param password the password to set
   *
   * @return (NOOP, 230) if the Password is OK, else return the following
   *     command that must follow (usually ACCT)
   *     and the associated reply
   *
   * @throws Reply421Exception if there is a problem during the
   *     authentication
   * @throws Reply530Exception if there is a problem during the
   *     authentication
   */
  @Override
  public NextCommandReply setPassword(String password)
      throws Reply421Exception, Reply530Exception {
    final NextCommandReply next = setBusinessPassword(password);
    this.password = password;
    if (next.reply == ReplyCode.REPLY_230_USER_LOGGED_IN) {
      setRootFromAuth();
      session.getDir().initAfterIdentification();
    }
    return next;
  }

  /**
   * Set the Authentication to Identified or Not
   *
   * @param isIdentified
   */
  protected void setIsIdentified(boolean isIdentified) {
    this.isIdentified = isIdentified;
  }

  /**
   * Is the current Authentication OK for full identification. It must be true
   * after a correct sequence of
   * identification: At most, it is true when setAccount is OK. It could be
   * positive before (user name only,
   * user+password only).<br>
   * In the current implementation, as USER+PASS+ACCT are needed, it will be
   * true only after a correct ACCT.
   *
   * @return True if the user has a positive login, else False
   */
  @Override
  public boolean isIdentified() {
    return isIdentified;
  }

  /**
   * @return the root relative path from authentication if any or null if the
   *     default is used (default is /user
   *     or /user/account)
   *
   * @throws Reply421Exception if the business root is not available
   */
  protected abstract String setBusinessRootFromAuth() throws Reply421Exception;

  /**
   * Set the root relative Path from current status of Authentication (should
   * be
   * the highest level for the
   * current authentication). If setBusinessRootFromAuth returns null, by
   * default set /user or /user/account.
   *
   * @throws Reply421Exception if the business root is not available
   */
  protected void setRootFromAuth() throws Reply421Exception {
    rootFromAuth = setBusinessRootFromAuth();
    if (rootFromAuth == null) {
      rootFromAuth = DirInterface.SEPARATOR + user;
    }
  }

  @Override
  public String getBusinessPath() {
    return rootFromAuth;
  }

  /**
   * Business implementation of clean
   */
  protected abstract void businessClean();

  /**
   * Clean object
   */
  @Override
  public void clear() {
    businessClean();
    user = null;
    password = null;
    rootFromAuth = null;
    isIdentified = false;
  }

  /**
   * Return the full path as a String (with mount point).
   *
   * @param path relative path including business one (may be null or
   *     empty)
   *
   * @return the full path as a String
   */
  public String getAbsolutePath(String path) {
    if (path == null || path.isEmpty()) {
      return getBaseDirectory();
    }
    return AbstractDir
        .normalizePath(getBaseDirectory() + DirInterface.SEPARATOR + path);
  }

  /**
   * Return the relative path from a file (without mount point)
   *
   * @param file (full path with mount point)
   *
   * @return the relative path from a file
   */
  @Override
  public String getRelativePath(String file) {
    // Work around Windows path '\'
    return DOUBLE_SLASH.matcher(
        file.replaceFirst(AbstractDir.normalizePath(getBaseDirectory()), ""))
                       .replaceAll("/");
  }
}
