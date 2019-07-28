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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 */
public final class HttpXmlDefinition {
  private static final String UNABLE_TO_LINK_VALUE_OF_FIELD =
      "Unable to link value of field: ";

  private static final String UNABLE_TO_FIND_FIELD = "Unable to find field: ";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpXmlDefinition.class);

  /*
   * pagename, fileform, header, footer, beginform, endform, nextinform, uri, pagerole, errorpage, classname,
   * <br> <fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility, fieldmandatory, fieldcookieset,
   * fieldtovalidate, fieldposition, fieldrank>*
   */
  /**
   * HTTP global root
   */
  private static final String XML_ROOT_NAME = "root";
  /**
   * HTTP global root
   */
  private static final String XML_HTTP_ROOT = '/' + XML_ROOT_NAME + '/';
  /**
   * HTTP Pages
   */
  private static final String XML_HTTP_PAGES = "pages";
  /**
   * HTTP Page root
   */
  private static final String XML_HTTP_PAGE = "page";
  /**
   * HTTP Pagename
   */
  private static final String XML_HTTP_PAGENAME = "pagename";
  /**
   * HTTP Pagename
   */
  private static final String XML_HTTP_FILEFORM = "fileform";
  /**
   * HTTP Header
   */
  private static final String XML_HTTP_HEADER = "header";
  /**
   * HTTP Footer
   */
  private static final String XML_HTTP_FOOTER = "footer";
  /**
   * HTTP begin form
   */
  private static final String XML_HTTP_BEGINFORM = "beginform";
  /**
   * HTTP end form
   */
  private static final String XML_HTTP_ENDFORM = "endform";
  /**
   * HTTP next in form
   */
  private static final String XML_HTTP_NEXTINFORM = "nextinform";
  /**
   * HTTP uri
   */
  private static final String XML_HTTP_URI = "uri";
  /**
   * HTTP Page role
   */
  private static final String XML_HTTP_PAGEROLE = "pagerole";
  /**
   * HTTP error page (uri as reference to access to HttpPage Object)
   */
  private static final String XML_HTTP_ERRORPAGE = "errorpage";
  /**
   * HTTP Class name
   */
  private static final String XML_HTTP_CLASSNAME = "classname";
  /*
   * fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility, fieldmandatory, fieldcookieset,
   * fieldtovalidate, fieldposition, fieldrank
   */
  /**
   * HTTP fields list
   */
  private static final String XML_HTTP_FIELDS = "fields";
  /**
   * HTTP field definition
   */
  private static final String XML_HTTP_FIELD = "field";
  /**
   * HTTP fieldname
   */
  private static final String XML_HTTP_FIELDNAME = "fieldname";
  /**
   * HTTP fieldtype
   */
  private static final String XML_HTTP_FIELDTYPE = "fieldtype";
  /**
   * HTTP fieldinfo
   */
  private static final String XML_HTTP_FIELDINFO = "fieldinfo";
  /**
   * HTTP fieldvalue
   */
  private static final String XML_HTTP_FIELDVALUE = "fieldvalue";
  /**
   * HTTP fieldvisibility
   */
  private static final String XML_HTTP_FIELDVISIBILITY = "fieldvisibility";
  /**
   * HTTP fieldmandatory
   */
  private static final String XML_HTTP_FIELDMANDATORY = "fieldmandatory";
  /**
   * HTTP fieldcookieset
   */
  private static final String XML_HTTP_FIELDCOOKIESET = "fieldcookieset";
  /**
   * HTTP fieldtovalidate
   */
  private static final String XML_HTTP_FIELDTOVALIDATE = "fieldtovalidate";
  /**
   * HTTP fieldposition
   */
  private static final String XML_HTTP_FIELDPOSITION = "fieldposition";
  /**
   * HTTP fieldrank
   */
  private static final String XML_HTTP_FIELDRANK = "fieldrank";

  /**
   * Structure of the Configuration: Field
   */
  private static final XmlDecl[] configHttpField = {
      // 1 Field
      new XmlDecl(XmlType.STRING, XML_HTTP_FIELDNAME),
      new XmlDecl(XmlType.STRING, XML_HTTP_FIELDTYPE),
      new XmlDecl(XmlType.STRING, XML_HTTP_FIELDINFO),
      new XmlDecl(XmlType.STRING, XML_HTTP_FIELDVALUE),
      new XmlDecl(XmlType.BOOLEAN, XML_HTTP_FIELDVISIBILITY),
      new XmlDecl(XmlType.BOOLEAN, XML_HTTP_FIELDMANDATORY),
      new XmlDecl(XmlType.BOOLEAN, XML_HTTP_FIELDCOOKIESET),
      new XmlDecl(XmlType.BOOLEAN, XML_HTTP_FIELDTOVALIDATE),
      new XmlDecl(XmlType.STRING, XML_HTTP_FIELDPOSITION),
      new XmlDecl(XmlType.INTEGER, XML_HTTP_FIELDRANK)
  };

  /**
   * Structure of the Configuration: Page
   */
  private static final XmlDecl[] configHttpPage = {
      // 1 Page
      new XmlDecl(XmlType.STRING, XML_HTTP_PAGENAME),
      new XmlDecl(XmlType.STRING, XML_HTTP_FILEFORM),
      new XmlDecl(XmlType.STRING, XML_HTTP_HEADER),
      new XmlDecl(XmlType.STRING, XML_HTTP_FOOTER),
      new XmlDecl(XmlType.STRING, XML_HTTP_BEGINFORM),
      new XmlDecl(XmlType.STRING, XML_HTTP_ENDFORM),
      new XmlDecl(XmlType.STRING, XML_HTTP_NEXTINFORM),
      new XmlDecl(XmlType.STRING, XML_HTTP_URI),
      new XmlDecl(XmlType.STRING, XML_HTTP_PAGEROLE),
      new XmlDecl(XmlType.STRING, XML_HTTP_ERRORPAGE),
      new XmlDecl(XmlType.STRING, XML_HTTP_CLASSNAME),
      // all fields
      new XmlDecl(XML_HTTP_FIELD, XmlType.XVAL,
                  XML_HTTP_FIELDS + '/' + XML_HTTP_FIELD, configHttpField, true)
  };

  /**
   * Structure of the Configuration: Pages
   * <p>
   * from root => Pages.Page
   */
  private static final XmlDecl[] configHttpPages = {
      // all pages
      new XmlDecl(XML_HTTP_PAGE, XmlType.XVAL,
                  XML_HTTP_ROOT + XML_HTTP_PAGES + '/' + XML_HTTP_PAGE,
                  configHttpPage, true)
  };

  private HttpXmlDefinition() {
  }

  static AbstractHttpField loadHttpPage(XmlValue[] xmlValue)
      throws InvalidArgumentException {
    final XmlHash hash = new XmlHash(xmlValue);
    XmlValue value = hash.get(XML_HTTP_FIELDNAME);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDNAME);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDNAME);
    }
    final String fieldname = value.getString();
    value = hash.get(XML_HTTP_FIELDTYPE);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDTYPE);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDTYPE);
    }
    final String fieldtype = value.getString();
    FieldRole fieldRole;
    try {
      fieldRole = FieldRole.valueOf(fieldtype);
    } catch (final IllegalArgumentException e) {
      logger.error(UNABLE_TO_LINK_VALUE_OF_FIELD + XML_HTTP_FIELDTYPE);
      throw new InvalidArgumentException(
          UNABLE_TO_LINK_VALUE_OF_FIELD + XML_HTTP_FIELDTYPE);
    }
    value = hash.get(XML_HTTP_FIELDINFO);
    String fieldinfo = fieldname;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldinfo = value.getString();
    }
    value = hash.get(XML_HTTP_FIELDVALUE);
    String fieldvalue = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldvalue = value.getString();
    }
    value = hash.get(XML_HTTP_FIELDVISIBILITY);
    boolean fieldvisibility = true;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldvisibility = value.getBoolean();
    }
    value = hash.get(XML_HTTP_FIELDMANDATORY);
    boolean fieldmandatory = true;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldmandatory = value.getBoolean();
    }
    value = hash.get(XML_HTTP_FIELDCOOKIESET);
    boolean fieldcookieset = false;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldcookieset = value.getBoolean();
    }
    value = hash.get(XML_HTTP_FIELDTOVALIDATE);
    boolean fieldtovalidate = false;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldtovalidate = value.getBoolean();
    }
    value = hash.get(XML_HTTP_FIELDPOSITION);
    FieldPosition fieldposition = FieldPosition.ANY;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fieldposition = FieldPosition.valueOf(value.getString());
    }
    value = hash.get(XML_HTTP_FIELDRANK);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDRANK);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_FIELDRANK);
    }
    final int fieldrank = value.getInteger();
    return new DefaultHttpField(fieldname, fieldRole, fieldinfo, fieldvalue,
                                fieldvisibility, fieldmandatory, fieldcookieset,
                                fieldtovalidate, fieldposition, fieldrank);
  }

  static HttpPage loadHttpConfiguration(XmlValue[] xmlValue)
      throws InvalidArgumentException, ClassNotFoundException,
             InstantiationException, IllegalAccessException {
    final XmlHash hash = new XmlHash(xmlValue);
    XmlValue value = hash.get(XML_HTTP_PAGENAME);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_PAGENAME);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_PAGENAME);
    }
    final String pagename = value.getString();
    value = hash.get(XML_HTTP_FILEFORM);
    String fileform = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      fileform = value.getString();
    }
    value = hash.get(XML_HTTP_HEADER);
    String header = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      header = value.getString();
    }
    value = hash.get(XML_HTTP_FOOTER);
    String footer = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      footer = value.getString();
    }
    value = hash.get(XML_HTTP_BEGINFORM);
    String beginform = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      beginform = value.getString();
    }
    value = hash.get(XML_HTTP_ENDFORM);
    String endform = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      endform = value.getString();
    }
    value = hash.get(XML_HTTP_NEXTINFORM);
    String nextinform = null;
    if (value != null && !value.isEmpty() && value.getString().length() != 0) {
      nextinform = value.getString();
    }
    value = hash.get(XML_HTTP_URI);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_URI);
      throw new InvalidArgumentException(UNABLE_TO_FIND_FIELD + XML_HTTP_URI);
    }
    final String uri = value.getString();
    value = hash.get(XML_HTTP_PAGEROLE);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_PAGEROLE);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_PAGEROLE);
    }
    final String pagerole = value.getString();
    PageRole pageRole;
    try {
      pageRole = PageRole.valueOf(pagerole);
    } catch (final IllegalArgumentException e) {
      logger.error(UNABLE_TO_LINK_VALUE_OF_FIELD + XML_HTTP_PAGEROLE);
      throw new InvalidArgumentException(
          UNABLE_TO_LINK_VALUE_OF_FIELD + XML_HTTP_PAGEROLE);
    }
    value = hash.get(XML_HTTP_ERRORPAGE);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_ERRORPAGE);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_ERRORPAGE);
    }
    final String errorpage = value.getString();
    value = hash.get(XML_HTTP_CLASSNAME);
    if (value == null || value.isEmpty() || value.getString().length() == 0) {
      logger.error(UNABLE_TO_FIND_FIELD + XML_HTTP_CLASSNAME);
      throw new InvalidArgumentException(
          UNABLE_TO_FIND_FIELD + XML_HTTP_CLASSNAME);
    }
    final String classname = value.getString();
    // now getting Fields
    value = hash.get(XML_HTTP_FIELD);
    @SuppressWarnings("unchecked")
    List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    List<AbstractHttpField> listFields =
        new ArrayList<AbstractHttpField>(list.size());
    // Now read the configuration
    for (final XmlValue[] fieldValue : list) {
      final AbstractHttpField field = loadHttpPage(fieldValue);
      listFields.add(field.getFieldrank(), field);
    }
    list.clear();
    final LinkedHashMap<String, AbstractHttpField> linkedHashMap =
        new LinkedHashMap<String, AbstractHttpField>(listFields.size());
    for (final AbstractHttpField abstractHttpField : listFields) {
      linkedHashMap.put(abstractHttpField.getFieldname(), abstractHttpField);
    }
    listFields.clear();
    return new HttpPage(pagename, fileform, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
  }

  /**
   * Initiate the configuration from the xml file for Http server
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
  public static HttpPageHandler setConfigurationHttpServerFromXml(
      String filename) throws InvalidArgumentException, ClassNotFoundException,
                              InstantiationException, IllegalAccessException {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error("Unable to read the XML Config file: " + filename, e);
      throw new InvalidArgumentException(
          "Unable to read XML file: " + filename);
    }
    if (document == null) {
      logger.error("Unable to read the XML Config file: " + filename);
      throw new InvalidArgumentException(
          "Unable to parse XML file: " + filename);
    }
    XmlValue[] values = XmlUtil.read(document, configHttpPages);
    if (values.length <= 0) {
      throw new InvalidArgumentException("XML file is empty");
    }
    final XmlValue value = values[0];
    @SuppressWarnings("unchecked")
    List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    final HashMap<String, HttpPage> pages =
        new HashMap<String, HttpPage>(list.size());
    // Now read the configuration
    for (final XmlValue[] xmlValue : list) {
      final HttpPage page = loadHttpConfiguration(xmlValue);
      pages.put(page.getUri(), page);
    }
    list.clear();
    return new HttpPageHandler(pages);
  }

  /**
   * Construct a new Element with value
   *
   * @param name
   * @param value
   *
   * @return the new Element
   */
  private static Element newElement(String name, String value) {
    final Element node = new DefaultElement(name);
    if (value != null && value.length() > 0) {
      node.addText(value);
    }
    return node;
  }

  static void addToField(Element root, AbstractHttpField field) {
    root.add(newElement(XML_HTTP_FIELDNAME, field.getFieldname()));
    root.add(newElement(XML_HTTP_FIELDTYPE, field.getFieldtype().name()));
    root.add(newElement(XML_HTTP_FIELDINFO, field.getFieldinfo()));
    root.add(newElement(XML_HTTP_FIELDVALUE, field.fieldvalue));
    root.add(newElement(XML_HTTP_FIELDVISIBILITY,
                        Boolean.toString(field.isFieldvisibility())));
    root.add(newElement(XML_HTTP_FIELDMANDATORY,
                        Boolean.toString(field.isFieldmandatory())));
    root.add(newElement(XML_HTTP_FIELDCOOKIESET,
                        Boolean.toString(field.isFieldcookieset())));
    root.add(newElement(XML_HTTP_FIELDTOVALIDATE,
                        Boolean.toString(field.isFieldtovalidate())));
    root.add(
        newElement(XML_HTTP_FIELDPOSITION, field.getFieldposition().name()));
    root.add(
        newElement(XML_HTTP_FIELDRANK, Integer.toString(field.getFieldrank())));
  }

  static void addToElement(Element root, HttpPage page) {
    root.add(newElement(XML_HTTP_PAGENAME, page.getPagename()));
    root.add(newElement(XML_HTTP_FILEFORM, page.getFileform()));
    root.add(newElement(XML_HTTP_HEADER, page.getHeader()));
    root.add(newElement(XML_HTTP_FOOTER, page.getFooter()));
    root.add(newElement(XML_HTTP_BEGINFORM, page.getBeginform()));
    root.add(newElement(XML_HTTP_ENDFORM, page.getEndform()));
    root.add(newElement(XML_HTTP_NEXTINFORM, page.getNextinform()));
    root.add(newElement(XML_HTTP_URI, page.getUri()));
    root.add(newElement(XML_HTTP_PAGEROLE, page.getPagerole().name()));
    root.add(newElement(XML_HTTP_ERRORPAGE, page.getErrorpage()));
    root.add(newElement(XML_HTTP_CLASSNAME, page.getClassname()));
    final Element element = root.addElement(XML_HTTP_FIELDS);
    for (final AbstractHttpField field : page.getFields().values()) {
      final Element subroot = element.addElement(XML_HTTP_FIELD);
      addToField(subroot, field);
    }
  }

  public static void exportConfiguration(HttpPageHandler httpPageHandler,
                                         String filename)
      throws HttpIncorrectRequestException {
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement(XML_ROOT_NAME);
    final Element subroot = root.addElement(XML_HTTP_PAGES);
    for (final HttpPage page : httpPageHandler.getHashmap().values()) {
      final Element element = subroot.addElement(XML_HTTP_PAGE);
      addToElement(element, page);
    }
    try {
      XmlUtil.writeXML(filename, null, document);
    } catch (final IOException e) {
      throw new HttpIncorrectRequestException("Cannot write file: " + filename,
                                              e);
    }
  }
}
