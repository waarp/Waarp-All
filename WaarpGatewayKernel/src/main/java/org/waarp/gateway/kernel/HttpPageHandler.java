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
package org.waarp.gateway.kernel;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.waarp.common.database.DbConstant;
import org.waarp.gateway.kernel.database.WaarpActionLogger;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.session.HttpSession;

import java.util.Map;

/**
 *
 */
public class HttpPageHandler {
  /*
   * Need as default error pages: 400, 401, 403, 404, 406, 500
   */

  private static final String INCORRECT_PAGE = "Incorrect Page: ";

  public static String hostid;

  private Map<String, HttpPage> hashmap;

  /**
   * @param hashmap
   */
  public HttpPageHandler(Map<String, HttpPage> hashmap) {
    setHashmap(hashmap);
  }

  /**
   * @param code
   *
   * @return an HttpPage according to the error code (400, 404, 500, ...)
   */
  public HttpPage getHttpPage(int code) {
    final String scode = Integer.toString(code);
    return getHashmap().get(scode);
  }

  /**
   * @param uri
   * @param method
   * @param session
   *
   * @return the associated HttpPage if any
   *
   * @throws HttpIncorrectRequestException
   */
  public HttpPage getHttpPage(String uri, String method, HttpSession session)
      throws HttpIncorrectRequestException {
    HttpPage page = getHashmap().get(uri);
    if (page == null) {
      return null;
    }
    switch (page.getPagerole()) {
      case DELETE:
        if (!"DELETE".equalsIgnoreCase(method)) {
          // error
          WaarpActionLogger
              .logErrorAction(DbConstant.admin.getSession(), session,
                              INCORRECT_PAGE + page.getPagerole(),
                              HttpResponseStatus.BAD_REQUEST);
          if (page.getErrorpage() != null || page.getErrorpage().length() > 1) {
            page = getHashmap().get(page.getErrorpage());
          } else {
            page = null;
          }
        }
        break;
      case HTML:
      case MENU:
        // no check
        break;
      case GETDOWNLOAD:
        if (!"GET".equalsIgnoreCase(method)) {
          // error
          WaarpActionLogger
              .logErrorAction(DbConstant.admin.getSession(), session,
                              INCORRECT_PAGE + page.getPagerole(),
                              HttpResponseStatus.BAD_REQUEST);
          if (page.getErrorpage() != null || page.getErrorpage().length() > 1) {
            page = getHashmap().get(page.getErrorpage());
          } else {
            page = null;
          }
        }
        break;
      case POST:
      case POSTUPLOAD:
        if (!"POST".equalsIgnoreCase(method)) {
          // error
          WaarpActionLogger
              .logErrorAction(DbConstant.admin.getSession(), session,
                              INCORRECT_PAGE + page.getPagerole(),
                              HttpResponseStatus.BAD_REQUEST);
          if (page.getErrorpage() != null || page.getErrorpage().length() > 1) {
            page = getHashmap().get(page.getErrorpage());
          } else {
            page = null;
          }
        }
        break;
      case PUT:
        if (!"PUT".equalsIgnoreCase(method)) {
          // error
          WaarpActionLogger
              .logErrorAction(DbConstant.admin.getSession(), session,
                              INCORRECT_PAGE + page.getPagerole(),
                              HttpResponseStatus.BAD_REQUEST);
          if (page.getErrorpage() != null || page.getErrorpage().length() > 1) {
            page = getHashmap().get(page.getErrorpage());
          } else {
            page = null;
          }
        }
        break;
      case ERROR:
        break;
      default:
        // error
        WaarpActionLogger.logErrorAction(DbConstant.admin.getSession(), session,
                                         INCORRECT_PAGE + page.getPagerole(),
                                         HttpResponseStatus.BAD_REQUEST);
        if (page.getErrorpage() != null || page.getErrorpage().length() > 1) {
          page = getHashmap().get(page.getErrorpage());
        } else {
          page = null;
        }
    }
    if (page == null) {
      throw new HttpIncorrectRequestException("No Page found");
    }
    return page;
  }

  /**
   * @return the hashmap
   */
  public Map<String, HttpPage> getHashmap() {
    return hashmap;
  }

  /**
   * @param hashmap the hashmap to set
   */
  private void setHashmap(Map<String, HttpPage> hashmap) {
    this.hashmap = hashmap;
  }
}
