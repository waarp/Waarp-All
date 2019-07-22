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
package org.waarp.gateway.kernel.http.saplink;

import org.waarp.gateway.kernel.AbstractHttpField;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.DefaultHttpField;
import org.waarp.gateway.kernel.HttpBusinessFactory;
import org.waarp.gateway.kernel.HttpPage;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.HttpPageHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 *
 */
public abstract class HttpSapBusinessFactory extends HttpBusinessFactory {

  public static String sapUrl = "/saplink";

  /**
   * All functions for SapArg: some could be not implemented. Note that create
   * and update exist in two modes
   * (Post and Put)
   */
  public enum SapFunction {
    info, get, docGet, createPost, createPut, mCreate, append, updatePost,
    updatePut, delete
  }

  /**
   * All fields for SapArg
   */
  public enum SapField {
    contRep, docId, compId, pVersion, resultAs, accessMode, authId, expiration,
    secKey, fromOffset, toOffset, Content_Type("Content-Type"), charset,
    version, Content_Length("Content-Length"), docProt, pattern, caseSensitive,
    numResults, X_compId("X-compId"), X_docId("X-docId"),
    X_docStatus("X-docStatus"), X_contentRep("X-contentRep"),
    X_pVersion("X-pVersion"), X_dateC("X-dateC"), X_timeC("X-timeC"),
    X_dateM("X-dateM"), X_timeM("X-timeM"), X_numberComps("X-numberComps"),
    X_Content_Type("X-Content-Type"), X_Content_Length("X-Content-Length"),
    X_compStatus("X-compStatus"), X_compDateC("X-compDateC"),
    X_compTimeC("X-compTimeC"), X_compDateM("X-compDateM"),
    X_compTimeM("X-compTimeM"), X_numComps("X-numComps"),
    X_contRep("X-contRep"), Filename, last_none;

    /**
     * True String as Field
     */
    public String value;

    SapField(String value) {
      this.value = value;
    }

    SapField() {
      value = name();
    }

    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * Array of status: one entry by SapFunction, each entry has in that order
   * UrlMandatory, UrlOptional,
   * HeaderMandatory, HeaderOptional,BodyMandatory, BodyOptional,
   * SecurityOptional. If one SapArg is in the
   * array, it has the status associated with the rank. One SapArg can appears
   * in multiple rank.
   */
  private static final SapField[][][] allStatus = {
      // info
      {
          { SapField.contRep, SapField.docId, SapField.pVersion },
          { SapField.compId, SapField.resultAs }, {}, {}, {}, {}, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // get
      {
          { SapField.contRep, SapField.docId, SapField.pVersion },
          { SapField.compId, SapField.fromOffset, SapField.toOffset }, {}, {},
          {}, {}, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // docGet (potential multiple get)
      {
          { SapField.contRep, SapField.docId, SapField.pVersion }, {}, {}, {},
          {}, {}, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // createPost
      {
          { SapField.contRep, SapField.docId, SapField.pVersion },
          { SapField.docProt }, { SapField.Content_Length }, {},
          { SapField.compId, SapField.Content_Length }, {
          SapField.Content_Type, SapField.charset, SapField.version
      }, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // createPut
      {
          {
              SapField.contRep, SapField.docId, SapField.compId,
              SapField.pVersion
          }, { SapField.docProt }, { SapField.Content_Length }, {}, {}, {
          SapField.Content_Type, SapField.charset, SapField.version
      }, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // mCreate
      {
          { SapField.contRep, SapField.docId, SapField.pVersion },
          { SapField.docProt }, {}, {}, {
          SapField.X_compId, SapField.X_docId, SapField.Content_Length
      }, {
          SapField.Content_Type, SapField.charset, SapField.version
      }, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // append
      {
          {
              SapField.contRep, SapField.docId, SapField.compId,
              SapField.pVersion
          }, {}, {}, {}, {}, {}, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // updatePost
      {
          { SapField.contRep, SapField.docId, SapField.pVersion }, {}, {}, {},
          { SapField.compId, SapField.Content_Length }, {
          SapField.Content_Type, SapField.charset, SapField.version
      }, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // updatePut
      {
          {
              SapField.contRep, SapField.docId, SapField.compId,
              SapField.pVersion
          }, {}, { SapField.Content_Length }, {}, {}, {
          SapField.Content_Type, SapField.charset, SapField.version
      }, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
      // delete
      {
          { SapField.contRep, SapField.docId, SapField.pVersion },
          { SapField.compId }, {}, {}, {}, {}, {
          SapField.secKey, SapField.accessMode, SapField.authId,
          SapField.expiration
      }
      },
  };

  public HttpSapBusinessFactory() {
  }

  public static HttpPageHandler initializeHttpPageHandler() {
    // manual creation
    final HashMap<String, HttpPage> pages = new HashMap<String, HttpPage>();
    String pagename, header, footer, beginform, endform, nextinform, uri,
        errorpage, classname;
    PageRole pageRole;
    LinkedHashMap<String, AbstractHttpField> linkedHashMap;

    try {
      // Need as default error pages: 400, 401, 403, 404, 406, 500
      final HttpPageHandler pageHandler = new HttpPageHandler(pages);
      if (!HttpBusinessFactory.addDefaultErrorPages(pageHandler, "SAP ERROR",
                                                    HttpSapBusinessFactory.class)) {
        throw new IllegalAccessException("Cannot build default error pages");
      }

      classname = HttpSapBusinessFactory.class.getName();

      // info
      pageRole = PageRole.GETDOWNLOAD;
      pagename = SapFunction.info.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.info.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // get
      pageRole = PageRole.GETDOWNLOAD;
      pagename = SapFunction.get.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.get.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // docGet
      pageRole = PageRole.GETDOWNLOAD;
      pagename = SapFunction.docGet.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.docGet.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // createPost
      pageRole = PageRole.POSTUPLOAD;
      pagename = SapFunction.createPost.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap,
                       allStatus[SapFunction.createPost.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // createPut
      pageRole = PageRole.PUT;
      pagename = SapFunction.createPut.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap,
                       allStatus[SapFunction.createPut.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // mCreate
      pageRole = PageRole.POSTUPLOAD;
      pagename = SapFunction.mCreate.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.mCreate.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // append
      pageRole = PageRole.PUT;
      pagename = SapFunction.append.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.append.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // updatePost
      pageRole = PageRole.POSTUPLOAD;
      pagename = SapFunction.updatePost.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap,
                       allStatus[SapFunction.updatePost.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // updatePut
      pageRole = PageRole.PUT;
      pagename = SapFunction.updatePut.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap,
                       allStatus[SapFunction.updatePut.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
      // delete
      pageRole = PageRole.DELETE;
      pagename = SapFunction.delete.name();
      uri = sapUrl;
      header = null;
      footer = null;
      beginform = null;
      endform = null;
      nextinform = null;
      errorpage = "404";
      linkedHashMap = new LinkedHashMap<String, AbstractHttpField>();
      addDefaultFields(linkedHashMap, allStatus[SapFunction.delete.ordinal()]);
      pages.put(uri,
                new HttpPage(pagename, null, header, footer, beginform, endform,
                             nextinform, uri, pageRole, errorpage, classname,
                             linkedHashMap));
    } catch (final ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new HttpPageHandler(pages);
  }

  private static void addDefaultFields(
      LinkedHashMap<String, AbstractHttpField> linkedHashMap,
      SapField[][] fields) {
    String fieldname, fieldinfo, fieldvalue;
    FieldRole fieldRole;
    boolean fieldvisibility, fieldmandatory, fieldcookieset;
    int fieldrank;
    /*
     * UrlMandatory, UrlOptional, HeaderMandatory, HeaderOptional,BodyMandatory, BodyOptional, SecurityOptional.
     */
    int nb = 0;
    int rank = 0;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = true;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.URL,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = false;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.URL,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = true;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.HEADER,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = false;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.HEADER,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = true;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.BODY,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = false;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.BODY,
                                             fieldrank));
    }
    rank++;
    for (int j = 0; j < fields[rank].length; j++) {
      final SapField field = fields[rank][j];
      fieldname = field.name();
      fieldinfo = field.toString();
      fieldvalue = null;
      fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
      fieldvisibility = true;
      fieldmandatory = false;
      fieldcookieset = false;
      fieldrank = ++nb;
      linkedHashMap.put(fieldname,
                        new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                             fieldvalue, fieldvisibility,
                                             fieldmandatory, fieldcookieset,
                                             true, FieldPosition.ANY,
                                             fieldrank));
    }
  }

}
