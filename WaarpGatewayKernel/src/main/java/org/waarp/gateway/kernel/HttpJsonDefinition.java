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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 */
public class HttpJsonDefinition {
  private static final String CANNOT_WRITE_FILE = "Cannot write file: ";
  private static final String UNABLE_TO_READ_JSON_FILE =
      "Unable to read JSON file: ";
  private static final String UNABLE_TO_READ_THE_JSON_CONFIG_FILE =
      "Unable to read the JSON Config file: ";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpJsonDefinition.class);

  private HttpJsonDefinition() {
  }

  /*
   * pagename, fileform, header, footer, beginform, endform, nextinform, uri, pagerole, errorpage, classname,
   * <br> <fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility, fieldmandatory, fieldcookieset,
   * fieldtovalidate, fieldposition, fieldrank>*
   */
  /*
   * fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility, fieldmandatory, fieldcookieset,
   * fieldtovalidate, fieldposition, fieldrank
   */

  /**
   * Structure of the Configuration: Field
   */
  private static final class ConfigHttpField {
    // 1 field
    public String FIELDNAME;
    public FieldRole FIELDTYPE;
    public String FIELDINFO;
    public String FIELDVALUE;
    public boolean FIELDVISIBILITY;
    public boolean FIELDMANDATORY;
    public boolean FIELDCOOKIESET;
    public boolean FIELDTOVALIDATE;
    public FieldPosition FIELDPOSITION;
    public int FIELDRANK;
  }

  /**
   * Structure of the Configuration: Page
   */
  private static final class ConfigHttpPage {
    // 1 Page
    public String PAGENAME;
    public String FILEFORM;
    public String HEADER;
    public String FOOTER;
    public String BEGINFORM;
    public String ENDFORM;
    public String NEXTINFORM;
    public String URI;
    public PageRole PAGEROLE;
    public String ERRORPAGE;
    public String CLASSNAME;
    // all fields
    public List<ConfigHttpField> FIELD;
  }

  /**
   * Structure of the Configuration: Pages
   * <p>
   * from root => Pages.Page
   */
  private static final class ConfigHttpPages {
    // all pages
    public List<ConfigHttpPage> PAGE;
  }

  protected static AbstractHttpField loadHttpPage(
      final ConfigHttpField fieldValue) throws InvalidArgumentException {
    return new DefaultHttpField(fieldValue.FIELDNAME, fieldValue.FIELDTYPE,
                                fieldValue.FIELDINFO, fieldValue.FIELDVALUE,
                                fieldValue.FIELDVISIBILITY,
                                fieldValue.FIELDMANDATORY,
                                fieldValue.FIELDCOOKIESET,
                                fieldValue.FIELDTOVALIDATE,
                                fieldValue.FIELDPOSITION, fieldValue.FIELDRANK);
  }

  protected static HttpPage loadHttpConfiguration(final ConfigHttpPage cpage)
      throws InvalidArgumentException, ClassNotFoundException,
             InstantiationException, IllegalAccessException {
    List<ConfigHttpField> list = cpage.FIELD;
    final LinkedHashMap<String, AbstractHttpField> linkedHashMap =
        new LinkedHashMap<String, AbstractHttpField>(list.size());
    // Now read the configuration
    for (final ConfigHttpField fieldValue : list) {
      final AbstractHttpField field = loadHttpPage(fieldValue);
      linkedHashMap.put(field.getFieldname(), field);
    }
    list.clear();
    list = null;
    return new HttpPage(cpage.PAGENAME, cpage.FILEFORM, cpage.HEADER,
                        cpage.FOOTER, cpage.BEGINFORM, cpage.ENDFORM,
                        cpage.NEXTINFORM, cpage.URI, cpage.PAGEROLE,
                        cpage.ERRORPAGE, cpage.CLASSNAME, linkedHashMap);
  }

  /**
   * Initiate the configuration from the json file for Http server
   *
   * @param filename
   *
   * @return the List<HttpPage> if OK
   *
   * @throws InvalidArgumentException
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public static HttpPageHandler setConfigurationHttpServerFromJson(
      final String filename)
      throws InvalidArgumentException, ClassNotFoundException,
             InstantiationException, IllegalAccessException {
    final File file = new File(filename);
    final ConfigHttpPages cpages;
    try {
      cpages = JsonHandler.mapper.readValue(file, ConfigHttpPages.class);
    } catch (final JsonParseException e) {
      logger.error(UNABLE_TO_READ_THE_JSON_CONFIG_FILE + filename, e);
      throw new InvalidArgumentException(UNABLE_TO_READ_JSON_FILE + filename,
                                         e);
    } catch (final JsonMappingException e) {
      logger.error(UNABLE_TO_READ_THE_JSON_CONFIG_FILE + filename, e);
      throw new InvalidArgumentException(UNABLE_TO_READ_JSON_FILE + filename,
                                         e);
    } catch (final IOException e) {
      logger.error(UNABLE_TO_READ_THE_JSON_CONFIG_FILE + filename, e);
      throw new InvalidArgumentException(UNABLE_TO_READ_JSON_FILE + filename,
                                         e);
    }
    final HashMap<String, HttpPage> pages =
        new HashMap<String, HttpPage>(cpages.PAGE.size());
    // Now read the configuration
    for (final ConfigHttpPage cpage : cpages.PAGE) {
      final HttpPage page = loadHttpConfiguration(cpage);
      pages.put(page.getUri(), page);
    }
    cpages.PAGE.clear();
    return new HttpPageHandler(pages);
  }

  protected static void addToField(final List<ConfigHttpField> fields,
                                   final AbstractHttpField field) {
    final ConfigHttpField cfield = new ConfigHttpField();
    cfield.FIELDNAME = field.getFieldname();
    cfield.FIELDTYPE = field.getFieldtype();
    cfield.FIELDINFO = field.getFieldinfo();
    cfield.FIELDVALUE = field.fieldvalue;
    cfield.FIELDVISIBILITY = field.isFieldvisibility();
    cfield.FIELDMANDATORY = field.isFieldmandatory();
    cfield.FIELDCOOKIESET = field.isFieldcookieset();
    cfield.FIELDTOVALIDATE = field.isFieldtovalidate();
    cfield.FIELDPOSITION = field.getFieldposition();
    cfield.FIELDRANK = field.getFieldrank();
    fields.add(cfield);
  }

  protected static void addToElement(final List<ConfigHttpPage> pages,
                                     final HttpPage page) {
    final ConfigHttpPage cpage = new ConfigHttpPage();
    cpage.PAGENAME = page.getPagename();
    cpage.FILEFORM = page.getFileform();
    cpage.HEADER = page.getHeader();
    cpage.FOOTER = page.getFooter();
    cpage.BEGINFORM = page.getBeginform();
    cpage.ENDFORM = page.getEndform();
    cpage.NEXTINFORM = page.getNextinform();
    cpage.URI = page.getUri();
    cpage.PAGEROLE = page.getPagerole();
    cpage.ERRORPAGE = page.getErrorpage();
    cpage.CLASSNAME = page.getClassname();
    cpage.FIELD = new ArrayList<ConfigHttpField>();
    for (final AbstractHttpField field : page.getFields().values()) {
      addToField(cpage.FIELD, field);
    }
    pages.add(cpage);
  }

  public static void exportConfiguration(final HttpPageHandler httpPageHandler,
                                         final String filename)
      throws HttpIncorrectRequestException {
    final ConfigHttpPages cpages = new ConfigHttpPages();
    cpages.PAGE = new ArrayList<ConfigHttpPage>();
    for (final HttpPage page : httpPageHandler.getHashmap().values()) {
      addToElement(cpages.PAGE, page);
    }
    final File file = new File(filename);
    try {
      JsonHandler.mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(file, cpages);
    } catch (final JsonGenerationException e) {
      throw new HttpIncorrectRequestException(CANNOT_WRITE_FILE + filename, e);
    } catch (final JsonMappingException e) {
      throw new HttpIncorrectRequestException(CANNOT_WRITE_FILE + filename, e);
    } catch (final IOException e) {
      throw new HttpIncorrectRequestException(CANNOT_WRITE_FILE + filename, e);
    }
  }
}
