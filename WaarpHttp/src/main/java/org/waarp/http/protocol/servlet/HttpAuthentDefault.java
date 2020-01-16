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

import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.http.protocol.HttpSession;
import org.waarp.openr66.context.R66Session;

import java.util.Map;

/**
 * Default implementation of HttpAuthent based on standard Waarp R66
 * authentication
 */
public class HttpAuthentDefault implements HttpAuthent {
  private static final String FIELD_USER = "user";
  private static final String FIELD_KEY = "key";

  /**
   * Package protected for the tests
   */
  protected String user;
  protected byte[] key;

  /**
   * Empty for instantiation from class name
   */
  public HttpAuthentDefault() {
    // Empty
  }

  @Override
  public void initializeAuthent(Map<String, String> arguments) {
    user = arguments.get(FIELD_USER);
    String skey = arguments.get(FIELD_KEY);
    key = org.waarp.common.digest.FilesystemBasedDigest.getFromHex(skey);
  }

  @Override
  public void checkAuthent(final HttpSession httpSession,
                           final R66Session session)
      throws IllegalArgumentException {
    try {
      session.getAuth().connection(user, key, false);
    } catch (Reply530Exception e) {
      httpSession.error(e, session.getBusinessObject());
    } catch (Reply421Exception e) {
      httpSession.error(e, session.getBusinessObject());
    }
  }

  @Override
  public String getUserId() {
    return user;
  }

  @Override
  public void finalizeTransfer(final HttpSession httpSession,
                               final R66Session session)
      throws IllegalArgumentException {
    // Do nothing
  }
}
