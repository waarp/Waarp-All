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

import org.waarp.http.protocol.HttpSession;
import org.waarp.openr66.context.R66Session;

import java.util.Map;

/**
 * Interface for authentication used by the Servlet or other ways (according to
 * implementations)
 */
public interface HttpAuthent {
  public static final String FIELD_USER = "user";
  public static final String FIELD_KEY = "key";

  /**
   * Method to setup authentication using servlet arguments
   *
   * @param arguments Map of all arguments as String
   */
  void initializeAuthent(Map<String, String> arguments);

  /**
   * Method to check authentication
   *
   * @param httpSession
   * @param session
   *
   * @throws IllegalArgumentException
   */
  void checkAuthent(final HttpSession httpSession, final R66Session session)
      throws IllegalArgumentException;

  /**
   * @return the user Id
   */
  String getUserId();

  /**
   * Finalize the transfer according to status in sessions
   *
   * @param httpSession
   * @param session
   *
   * @throws IllegalArgumentException
   */
  void finalizeTransfer(final HttpSession httpSession, final R66Session session)
      throws IllegalArgumentException;
}
