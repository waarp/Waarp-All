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
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.HttpPage.PageRole;

import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public abstract class HttpBusinessFactory {
  public static final HttpDataFactory factory =
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
  // Disk if size exceed MINSIZE = 16K, but for FileUpload from Ark directly
  // XXX FIXME TODO to setup outside !
  public static String TempPath = "J:/GG/ARK/TMP";

  /**
   * Initialize the Disk support
   */
  public static void initialize(final String tempPath) {
    TempPath = tempPath;
    DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
    // on exit (in normal
    // exit)
    DiskFileUpload.baseDirectory = TempPath; // system temp
    // directory
    DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
    // exit (in normal exit)
    DiskAttribute.baseDirectory = TempPath; // system temp directory
  }

  /**
   * It returns the AbstractHttpBusinessRequest to use during a new request.
   * <p>
   * Note that fields given in parameter should be updated according to their
   * values if needed.
   *
   * @param remoteAddress the remote SocketAddress in use
   * @param fields the fields linked hashmap (to preserver order) to
   *     set for
   *     the new request
   * @param page source HttpPage
   *
   * @return the AbstractHttpBusinessRequest to use during a new request
   */
  public abstract AbstractHttpBusinessRequest getNewHttpBusinessRequest(
      SocketAddress remoteAddress, Map<String, AbstractHttpField> fields,
      HttpPage page);

  /**
   * @param pages
   * @param title
   * @param clasz
   *
   * @return True if the default error pages are correctly added
   */
  public static boolean addDefaultErrorPages(final HttpPageHandler pages,
                                             final String title,
                                             final Class<?> clasz) {
    String pagename;
    final String header;
    final String footer;
    final String beginform;
    final String endform;
    final String nextinform;
    String uri;
    final String errorpage;
    final String classname;
    final PageRole pageRole;
    LinkedHashMap<String, AbstractHttpField> linkedHashMap;
    final String fieldname;
    final String fieldinfo;
    String fieldvalue;
    final FieldRole fieldRole;
    final boolean fieldvisibility;
    final boolean fieldmandatory;
    final boolean fieldcookieset;
    final boolean fieldtovalidate;
    final int fieldrank;

    // Need as default error pages: 400, 401, 403, 404, 406, 500
    try {
      pageRole = PageRole.ERROR;
      pagename = "400";
      uri = "400";
      header = "<HTML><HEAD><TITLE>" + title + "</TITLE></HEAD><BODY>";
      footer = "</BODY></HTML>";
      beginform = "<table border=\"0\"><tr><td><h1>" + title + "</h1>";
      endform =
          "</td></tr></table><br><CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>";
      nextinform = "</td></tr><tr><td>";
      classname = clasz.getName();
      errorpage = null;
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldname = AbstractHttpField.ERRORINFO;
      fieldinfo = "Error information";
      fieldvalue = HttpResponseStatus.BAD_REQUEST.reasonPhrase();
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = false;
      fieldcookieset = false;
      fieldtovalidate = false;
      fieldrank = 1;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));

      pagename = "401";
      uri = "401";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldvalue = HttpResponseStatus.UNAUTHORIZED.reasonPhrase();
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));

      pagename = "403";
      uri = "403";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldvalue = HttpResponseStatus.FORBIDDEN.reasonPhrase();
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));

      pagename = "404";
      uri = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldvalue = HttpResponseStatus.NOT_FOUND.reasonPhrase();
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));

      pagename = "406";
      uri = "406";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldvalue = HttpResponseStatus.NOT_ACCEPTABLE.reasonPhrase();
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));

      pagename = "500";
      uri = "500";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      fieldvalue = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase();
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             fieldtovalidate,
                                             FieldPosition.BODY, fieldrank));
      pages.getHashmap().put(uri, new HttpPage(pagename, null, header, footer,
                                               beginform, endform, nextinform,
                                               uri, pageRole, errorpage,
                                               classname, linkedHashMap));
      return true;
    } catch (final ClassNotFoundException ignored) {
      // nothing
    } catch (final InstantiationException ignored) {
      // nothing
    } catch (final IllegalAccessException ignored) {
      // nothing
    }
    return false;
  }
}
