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

package org.waarp.http.protocol;

import org.waarp.http.protocol.servlet.HttpAuthent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for all sessions (upload only)
 */
public class HttpSessions {
  private static final HttpSessions httpSessions = new HttpSessions();

  private final ConcurrentHashMap<String, HttpResumableSession> sessions =
      new ConcurrentHashMap<String, HttpResumableSession>();

  private HttpSessions() {
    // Empty and private
  }

  /**
   * @return the current Factory for HttpSession
   */
  public static HttpSessions getInstance() {
    return httpSessions;
  }

  /**
   * Get a already existing HttpResumableSession or create a new one if not
   * exists
   *
   * @param resumableInfo
   * @param rulename
   * @param comment
   * @param authent
   *
   * @return the HttpResumableSession
   */
  public synchronized HttpResumableSession getOrCreateResumableSession(
      final HttpResumableInfo resumableInfo, final String rulename,
      final String comment, final HttpAuthent authent) {
    HttpResumableSession session = sessions.get(resumableInfo.getIdentifier());
    if (session == null) {
      session =
          new HttpResumableSession(resumableInfo, rulename, comment, authent);
      sessions.put(resumableInfo.getIdentifier(), session);
    }
    return session;
  }

  /**
   * Check if session is already existing
   *
   * @param resumableInfo
   *
   * @return True if contained
   */
  protected boolean contains(final HttpResumableInfo resumableInfo) {
    return sessions.containsKey(resumableInfo.getIdentifier());
  }

  /**
   * Removes the current session
   *
   * @param resumableInfo
   *
   * @return True if found
   */
  public boolean removeSession(final HttpResumableInfo resumableInfo) {
    return sessions.remove(resumableInfo.getIdentifier()) != null;
  }

  /**
   * Removes the current session
   *
   * @param resumableSession
   *
   * @return True if found
   */
  public boolean removeSession(final HttpResumableSession resumableSession) {
    return sessions.remove(
        resumableSession.getHttpResumableInfo().getIdentifier()) != null;
  }
}
