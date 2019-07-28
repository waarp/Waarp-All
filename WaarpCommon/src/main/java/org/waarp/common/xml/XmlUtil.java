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
package org.waarp.common.xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Writer;
import java.util.List;

/**
 * XML utility that handles simple cases as:<br>
 * <ul>
 * <li>XPath as /x/y/z for a node and referring to root of Document</li>
 * <li>XPath as x/y/z for a node and referring to current referenced node</li>
 * <li>Any XPath can be a singleton (unique node referenced by the XPath) or
 * multiple nodes (same XPath)</li>
 * <li>Only Element as Node: no Attribute or any other type within XML</li>
 * <li>Any other path is not supported: //x /x@y ./ ../</li>
 * <li>Supports special SubXml tree as element (singleton or multiple)</li>
 * </ul>
 */
public final class XmlUtil {

  private static final String NODE_NOT_FOUND = "Node not found: ";

  private XmlUtil() {
  }

  /**
   * @param filename
   *
   * @return Existing Document from filename
   *
   * @throws IOException
   * @throws DocumentException
   */
  public static Document getDocument(String filename)
      throws IOException, DocumentException {
    final File file = new File(filename);
    if (!file.canRead()) {
      throw new IOException("File is not readable: " + filename);
    }
    return getDocument(file);
  }

  /**
   * @param file
   *
   * @return Existing Document from file
   *
   * @throws IOException
   * @throws DocumentException
   */
  public static Document getDocument(File file)
      throws IOException, DocumentException {
    if (!file.canRead()) {
      throw new IOException("File is not readable: " + file.getPath());
    }
    final SAXReader reader = new SAXReader();
    return reader.read(file);
  }

  /**
   * Read the document from the string
   *
   * @param document as String
   *
   * @return the Document
   *
   * @throws DocumentException
   */
  public static Document readDocument(String document)
      throws DocumentException {
    return DocumentHelper.parseText(document);
  }

  /**
   * @param document
   *
   * @return the document as an XML string
   */
  public static String writeToString(Document document) {
    return document.asXML();
  }

  /**
   * @param element
   *
   * @return the element as an XML string
   */
  public static String writeToString(Element element) {
    return element.asXML();
  }

  /**
   * @return an empty new Document
   */
  public static Document createEmptyDocument() {
    return DocumentHelper.createDocument();
  }

  /**
   * Save the document into the file
   *
   * @param filename
   * @param document
   *
   * @throws IOException
   */
  public static void saveDocument(String filename, Document document)
      throws IOException {
    final File file = new File(filename);
    saveDocument(file, document);
  }

  /**
   * Save the document into the file
   *
   * @param file
   * @param document
   *
   * @throws IOException
   */
  public static void saveDocument(File file, Document document)
      throws IOException {
    if (file.exists() && !file.canWrite()) {
      throw new IOException("File is not writable: " + file.getPath());
    }

    saveDocument(new FileWriter(file), document);
  }

  /**
   * Save the document into the Writer outWriter
   *
   * @param outWriter
   * @param document
   *
   * @throws IOException
   */
  public static void saveDocument(Writer outWriter, Document document)
      throws IOException {
    final OutputFormat format = OutputFormat.createPrettyPrint();
    format.setEncoding(WaarpStringUtils.UTF8.name());
    final XMLWriter writer = new XMLWriter(outWriter, format);
    writer.write(document);
    writer.flush();
    writer.close();
  }

  /**
   * Save the branch from element into the file
   *
   * @param filename
   * @param element
   *
   * @throws IOException
   */
  public static void saveElement(String filename, Element element)
      throws IOException {
    final File file = new File(filename);
    saveElement(file, element);
  }

  /**
   * Save the branch from element into the file
   *
   * @param file
   * @param element
   *
   * @throws IOException
   */
  public static void saveElement(File file, Element element)
      throws IOException {
    if (file.exists() && !file.canWrite()) {
      throw new IOException("File is not writable: " + file.getPath());
    }
    final OutputFormat format = OutputFormat.createPrettyPrint();
    format.setEncoding(WaarpStringUtils.UTF8.name());
    final XMLWriter writer = new XMLWriter(new FileWriter(file), format);
    writer.write(element);
    writer.flush();
    writer.close();
  }

  /**
   * @param document
   *
   * @return the root Element from the document
   */
  public static Element getRootElement(Document document) {
    return document.getRootElement();
  }

  /**
   * Add or Get (if already existing) an element given by the path relative to
   * the referent element and set the
   * value
   *
   * @param ref
   * @param path
   * @param value
   *
   * @return the new added or already existing element with new value
   */
  public static Element addOrSetElement(Element ref, String path,
                                        String value) {
    final Element current = addOrGetElement(ref, path);
    current.setText(value);
    return current;
  }

  /**
   * Add or Get (if already existing) an element given by the path relative to
   * the referent element
   *
   * @param ref
   * @param path
   *
   * @return the new added or already existing element
   */
  public static Element addOrGetElement(Element ref, String path) {
    final String[] pathes = path.split("/");
    Element current = ref;
    for (final String nodename : pathes) {
      if (!nodename.isEmpty()) {
        final Element exist = current.element(nodename);
        if (exist == null) {
          current = current.addElement(nodename);
        } else {
          current = exist;
        }
      }
    }
    return current;
  }

  /**
   * Add an element given by the path relative to the referent element and set
   * the value
   *
   * @param ref
   * @param path
   * @param value
   *
   * @return the new added element with value
   */
  public static Element addAndSetElementMultiple(Element ref, String path,
                                                 String value) {
    final Element current = addAndGetElementMultiple(ref, path);
    current.setText(value);
    return current;
  }

  /**
   * Add an element given by the path relative to the referent element
   *
   * @param ref
   * @param path
   *
   * @return the new added element
   */
  public static Element addAndGetElementMultiple(Element ref, String path) {
    final String[] pathes = path.split("/");
    Element current = ref;
    for (int i = 0; i < pathes.length - 1; i++) {
      final String nodename = pathes[i];
      if (!nodename.isEmpty()) {
        final Element exist = current.element(nodename);
        if (exist == null) {
          current = current.addElement(nodename);
        } else {
          current = exist;
        }
      }
    }
    final String nodename = pathes[pathes.length - 1];
    if (!nodename.isEmpty()) {
      current = current.addElement(nodename);
    }
    return current;
  }

  /**
   * @param ref
   * @param path
   *
   * @return the parent element associated with the path relatively to the
   *     referent element
   *
   * @throws DocumentException
   */
  public static Element getParentElement(Element ref, String path)
      throws DocumentException {
    String npath = path;
    while (npath.charAt(0) == '/') {
      npath = npath.substring(1);
    }
    final Element current = (Element) ref.selectSingleNode(npath);
    if (current == null) {
      throw new DocumentException(NODE_NOT_FOUND + path);
    }
    return current.getParent();
  }

  /**
   * @param ref
   * @param path
   *
   * @return the element associated with the path relatively to the referent
   *     element
   *
   * @throws DocumentException
   */
  public static Element getElement(Element ref, String path)
      throws DocumentException {
    String npath = path;
    while (npath.charAt(0) == '/') {
      npath = npath.substring(1);
    }
    final Element current = (Element) ref.selectSingleNode(npath);
    if (current == null) {
      throw new DocumentException(NODE_NOT_FOUND + path);
    }
    return current;
  }

  /**
   * @param ref
   * @param path
   *
   * @return the element associated with the path relatively to the referent
   *     element
   *
   * @throws DocumentException
   */
  @SuppressWarnings("unchecked")
  public static List<Element> getElementMultiple(Element ref, String path)
      throws DocumentException {
    String npath = path;
    while (npath.charAt(0) == '/') {
      npath = npath.substring(1);
    }
    final List<Element> list = ref.selectNodes(npath);
    if (list == null || list.isEmpty()) {
      throw new DocumentException("Nodes not found: " + path);
    }
    return list;
  }

  /**
   * Add or Get (if already existing) an element given by the path relative to
   * the document and set the value
   *
   * @param doc
   * @param path
   * @param value
   *
   * @return the new added or already existing element with new value
   */
  public static Element addOrSetElement(Document doc, String path,
                                        String value) {
    final Element current = addOrGetElement(doc, path);
    if (current != null) {
      current.setText(value);
    }
    return current;
  }

  /**
   * Add or Get (if already existing) an element given by the path relative to
   * the document
   *
   * @param doc
   * @param path
   *
   * @return the new added or already existing element
   */
  public static Element addOrGetElement(Document doc, String path) {
    final String[] pathes = path.split("/");
    int rank;
    for (rank = 0; rank < pathes.length; rank++) {
      if (!pathes[rank].isEmpty()) {
        break; // found
      }
    }
    if (rank >= pathes.length) {
      return null; // Should not be !
    }
    Element current = (Element) doc.selectSingleNode(pathes[rank]);
    if (current == null) {
      current = doc.addElement(pathes[rank]);
    }
    for (int i = rank + 1; i < pathes.length; i++) {
      final String nodename = pathes[i];
      if (!nodename.isEmpty()) {
        final Element exist = current.element(nodename);
        if (exist == null) {
          current = current.addElement(nodename);
        } else {
          current = exist;
        }
      }
    }
    return current;
  }

  /**
   * Add an element given by the path relative to the document and set the
   * value
   *
   * @param doc
   * @param path
   * @param value
   *
   * @return the new added element with value
   */
  public static Element addAndSetElementMultiple(Document doc, String path,
                                                 String value) {
    final Element current = addAndGetElementMultiple(doc, path);
    if (current != null) {
      current.setText(value);
    }
    return current;
  }

  /**
   * Add an element given by the path relative to the document
   *
   * @param doc
   * @param path
   *
   * @return the new added element
   */
  public static Element addAndGetElementMultiple(Document doc, String path) {
    final String[] pathes = path.split("/");
    int rank;
    for (rank = 0; rank < pathes.length; rank++) {
      if (!pathes[rank].isEmpty()) {
        break; // found
      }
    }
    if (rank >= pathes.length) {
      return null; // Should not be !
    }
    Element current = (Element) doc.selectSingleNode(pathes[rank]);
    if (current == null) {
      current = doc.addElement(pathes[rank]);
    }
    if (rank == pathes.length - 1) {
      // Last level is the root !!! No multiple root is allowed !!!
      // So just give back the root if it exists
      return current;
    }
    for (int i = rank + 1; i < pathes.length - 1; i++) {
      final String nodename = pathes[i];
      if (!nodename.isEmpty()) {
        final Element exist = current.element(nodename);
        if (exist == null) {
          current = current.addElement(nodename);
        } else {
          current = exist;
        }
      }
    }
    final String nodename = pathes[pathes.length - 1];
    if (!nodename.isEmpty()) {
      current = current.addElement(nodename);
    }
    return current;
  }

  /**
   * @param doc
   * @param path
   *
   * @return the Parent element associated with the path relatively to the
   *     document
   *
   * @throws DocumentException
   */
  public static Element getParentElement(Document doc, String path)
      throws DocumentException {
    final Element current = (Element) doc.selectSingleNode(path);
    if (current == null) {
      throw new DocumentException(NODE_NOT_FOUND + path);
    }
    return current.getParent();
  }

  /**
   * @param doc
   * @param path
   *
   * @return the element associated with the path relatively to the document
   *
   * @throws DocumentException
   */
  public static Element getElement(Document doc, String path)
      throws DocumentException {
    final Element current = (Element) doc.selectSingleNode(path);
    if (current == null) {
      throw new DocumentException(NODE_NOT_FOUND + path);
    }
    return current;
  }

  /**
   * @param doc
   * @param path
   *
   * @return the element associated with the path relatively to the document
   *
   * @throws DocumentException
   */
  @SuppressWarnings("unchecked")
  public static List<Element> getElementMultiple(Document doc, String path)
      throws DocumentException {
    final List<Element> list = doc.selectNodes(path);
    if (list == null || list.isEmpty()) {
      throw new DocumentException("Nodes not found: " + path);
    }
    return list;
  }

  /**
   * Remove extra space and tab, newline from beginning and end of String
   *
   * @param string
   *
   * @return the trimed string
   */
  public static String getExtraTrimed(String string) {
    return string.replaceAll("^[\\s]*|[\\s]*$", "");
  }

  /**
   * Create the XmlValues from the XmlDevls and the Document
   *
   * @param doc
   * @param decls
   *
   * @return XmlValues
   */
  public static XmlValue[] read(Document doc, XmlDecl[] decls) {
    XmlValue[] values;
    final int len = decls.length;
    values = new XmlValue[len];
    for (int i = 0; i < len; i++) {
      final XmlValue value = new XmlValue(decls[i]);
      values[i] = value;
      if (decls[i].isSubXml()) {
        if (decls[i].isMultiple()) {
          List<Element> elts;
          try {
            elts = getElementMultiple(doc, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          for (final Element element : elts) {
            final XmlValue[] newValue = read(element, decls[i].getSubXml());
            if (newValue == null) {
              continue;
            }
            try {
              value.addValue(newValue);
            } catch (final InvalidObjectException e) {
              // nothing
            }
          }
        } else {
          Element element;
          try {
            element = getElement(doc, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          final XmlValue[] newValue = read(element, decls[i].getSubXml());
          if (newValue == null) {
            continue;
          }
          try {
            value.setValue(newValue);
          } catch (final InvalidObjectException e) {
            // nothing
          }
        }
      } else if (decls[i].isMultiple()) {
        List<Element> elts;
        try {
          elts = getElementMultiple(doc, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        for (final Element element : elts) {
          final String svalue = element.getText();
          try {
            value.addFromString(getExtraTrimed(svalue));
          } catch (final InvalidObjectException e) {
            // nothing
          } catch (final InvalidArgumentException e) {
            // nothing
          }
        }
      } else {
        Element element;
        try {
          element = getElement(doc, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        final String svalue = element.getText();
        try {
          value.setFromString(getExtraTrimed(svalue));
        } catch (final InvalidArgumentException e) {
          // nothing
        }
      }
    }
    return values;
  }

  /**
   * Create the XmlValues from the XmlDevls and the reference Element
   *
   * @param ref
   * @param decls
   *
   * @return XmlValues
   */
  public static XmlValue[] read(Element ref, XmlDecl[] decls) {
    XmlValue[] values;
    final int len = decls.length;
    values = new XmlValue[len];
    for (int i = 0; i < len; i++) {
      final XmlValue value = new XmlValue(decls[i]);
      values[i] = value;
      if (decls[i].isSubXml()) {
        if (decls[i].isMultiple()) {
          List<Element> elts;
          try {
            elts = getElementMultiple(ref, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          for (final Element element : elts) {
            final XmlValue[] newValue = read(element, decls[i].getSubXml());
            if (newValue == null) {
              continue;
            }
            try {
              value.addValue(newValue);
            } catch (final InvalidObjectException e) {
              // nothing
            }
          }
        } else {
          Element element;
          try {
            element = getElement(ref, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          final XmlValue[] newValue = read(element, decls[i].getSubXml());
          if (newValue == null) {
            continue;
          }
          try {
            value.setValue(newValue);
          } catch (final InvalidObjectException e) {
            // nothing
          }
        }
      } else if (decls[i].isMultiple()) {
        List<Element> elts;
        try {
          elts = getElementMultiple(ref, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        for (final Element element : elts) {
          final String svalue = element.getText();
          try {
            value.addFromString(getExtraTrimed(svalue));
          } catch (final InvalidObjectException e) {
            // nothing
          } catch (final InvalidArgumentException e) {
            // nothing
          }
        }
      } else {
        Element element;
        try {
          element = getElement(ref, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        final String svalue = element.getText();
        try {
          value.setFromString(getExtraTrimed(svalue));
        } catch (final InvalidArgumentException e) {
          // nothing
        }
      }
    }
    return values;
  }

  /**
   * Add all nodes from XmlValues into Document
   *
   * @param doc
   * @param values
   */
  @SuppressWarnings("unchecked")
  public static void write(Document doc, XmlValue[] values) {
    for (final XmlValue value : values) {
      if (value != null) {
        if (value.isSubXml()) {
          if (value.isMultiple()) {
            final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
            for (final XmlValue[] object : list) {
              final Element ref =
                  addAndGetElementMultiple(doc, value.getXmlPath());
              write(ref, object);
            }
          } else {
            final Element ref = addOrGetElement(doc, value.getXmlPath());
            write(ref, value.getSubXml());
          }
        } else if (value.isMultiple()) {
          final List<?> list = value.getList();
          for (final Object object : list) {
            addAndSetElementMultiple(doc, value.getXmlPath(),
                                     object.toString());
          }
        } else {
          addOrSetElement(doc, value.getXmlPath(), value.getIntoString());
        }
      }
    }
  }

  /**
   * Add all nodes from XmlValues from the referenced Element
   *
   * @param ref
   * @param values
   */
  @SuppressWarnings("unchecked")
  public static void write(Element ref, XmlValue[] values) {
    for (final XmlValue value : values) {
      if (value != null) {
        if (value.isSubXml()) {
          if (value.isMultiple()) {
            final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
            for (final XmlValue[] object : list) {
              final Element newref =
                  addAndGetElementMultiple(ref, value.getXmlPath());
              write(newref, object);
            }
          } else {
            final Element newref = addOrGetElement(ref, value.getXmlPath());
            write(newref, value.getSubXml());
          }
        } else if (value.isMultiple()) {
          final List<?> list = value.getList();
          for (final Object object : list) {
            addAndSetElementMultiple(ref, value.getXmlPath(),
                                     object.toString());
          }
        } else {
          addOrSetElement(ref, value.getXmlPath(), value.getIntoString());
        }
      }
    }
  }

  /**
   * Write the given XML document to filename using the encoding
   *
   * @param filename
   * @param encoding if null, default encoding UTF-8 will be used
   * @param document
   *
   * @throws IOException
   */
  public static void writeXML(String filename, String encoding,
                              Document document) throws IOException {
    final OutputFormat format = OutputFormat.createPrettyPrint();
    if (encoding != null) {
      format.setEncoding(encoding);
    } else {
      format.setEncoding(WaarpStringUtils.UTF8.name());
    }
    XMLWriter writer;
    writer = new XMLWriter(new FileWriter(filename), format);
    writer.write(document);
    try {
      writer.close();
    } catch (final IOException ignored) {
      // nothing
    }
  }
}
