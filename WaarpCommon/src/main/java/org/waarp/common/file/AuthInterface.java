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
package org.waarp.common.file;

import org.waarp.common.command.NextCommandReply;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply530Exception;

/**
 * Interface for Authentication
 *
 *
 */
public interface AuthInterface {
  /**
   * @return the Ftp SessionInterface
   */
  SessionInterface getSession();

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
  NextCommandReply setUser(String user)
      throws Reply421Exception, Reply530Exception;

  /**
   * @return the user
   */
  String getUser();

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
  NextCommandReply setPassword(String password)
      throws Reply421Exception, Reply530Exception;

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
  boolean isIdentified();

  /**
   * @return True if the current authentication has an admin right (shutdown,
   *     bandwidth limitation)
   */
  boolean isAdmin();

  /**
   * Is the given complete relative Path valid from Authentication/Business
   * point of view.
   *
   * @param newPath
   *
   * @return True if it is Valid
   */
  boolean isBusinessPathValid(String newPath);

  /**
   * Return the relative path for this account according to the Business
   * (without true root of mount).<br>
   *
   * @return Return the relative path for this account
   */
  String getBusinessPath();

  /**
   * Return the mount point
   *
   * @return the mount point
   */
  String getBaseDirectory();

  /**
   * Return the relative path from a file (without mount point)
   *
   * @param file (full path with mount point)
   *
   * @return the relative path from a file
   */
  String getRelativePath(String file);

  /**
   * Clean object
   */
  void clear();
}
