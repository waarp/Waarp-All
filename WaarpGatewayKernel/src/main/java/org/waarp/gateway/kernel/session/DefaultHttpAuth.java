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

import org.waarp.common.command.NextCommandReply;
import org.waarp.common.file.SessionInterface;

/**
 * Allow all default implementation
 * <p>
 * If an authentication is needed, then it will have to use an implementation
 * compatible with the Security of
 * the IT that needs it.
 */
public class DefaultHttpAuth implements HttpAuthInterface {
  /**
   * User name
   */
  protected String user = "user";

  /**
   * Password
   */
  protected String password = "password"; // NOSONAR

  /**
   * Account name
   */
  protected String account = "account";

  /**
   * Is Identified
   */
  protected final boolean isIdentified;

  /**
   * SessionInterface
   */
  protected final SessionInterface session;

  /**
   * @param session
   */
  public DefaultHttpAuth(final SessionInterface session) {
    this.session = session;
    isIdentified = true;
  }

  @Override
  public final SessionInterface getSession() {
    return session;
  }

  @Override
  public final NextCommandReply setUser(final String user) {
    this.user = user;
    return null;
  }

  @Override
  public final String getUser() {
    return user;
  }

  @Override
  public final NextCommandReply setPassword(final String password) {
    this.password = password;
    return null;
  }

  @Override
  public final boolean isIdentified() {
    return isIdentified;
  }

  @Override
  public final boolean isAdmin() {
    return false;
  }

  @Override
  public final boolean isBusinessPathValid(final String newPath) {
    return true;
  }

  @Override
  public final String getBusinessPath() {
    return "";
  }

  @Override
  public final String getBaseDirectory() {
    return "/";
  }

  @Override
  public final String getRelativePath(final String file) {
    return file;
  }

  @Override
  public final void clear() {
    // nothing
  }

  @Override
  public final String getAccount() {
    return account;
  }

  @Override
  public final NextCommandReply setAccount(final String account) {
    this.account = account;
    return null;
  }

  @Override
  public final CommandExecutorInterface getCommandExecutor() {
    // TO DO Auto-generated method stub
    return null;
  }

}
