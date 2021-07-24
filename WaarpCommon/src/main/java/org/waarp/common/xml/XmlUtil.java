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
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.utility.WaarpStringUtils;
import org.xml.sax.SAXException;

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
   * @return the newly created SAXReader
   */
  public static SAXReader getNewSaxReader() {
    final SAXReader saxReader = new SAXReader();
    try {
      saxReader.setFeature(
          "http://apache.org/xml/features/disallow-doctype-decl", true);
      saxReader.setFeature(
          "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
          false);
      saxReader.setFeature(
          "http://apache.org/xml/features/nonvalidating/load-external-dtd",
          false);
      saxReader.setFeature("http://xml.org/sax/features/resolve-dtd-uris",
                           false);
      saxReader.setFeature(
          "http://xml.org/sax/features/external-general-entities", false);
      saxReader.setFeature(
          "http://xml.org/sax/features/external-parameter-entities", false);
      saxReader.setFeature(
          "http://apache.org/xml/features/validation/id-idref-checking", false);
    } catch (final SAXException e) {
      //Parse with external resources downloading allowed.
    }
    return saxReader;
  }

  /**
   * @param filename
   *
   * @return Existing Document from filename
   *
   * @throws IOException
   * @throws DocumentException
   */
  public static Document getDocument(final String filename)
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
  public static Document getDocument(final File file)
      throws IOException, DocumentException {
    if (!file.canRead()) {
      throw new IOException("File is not readable: " + file.getPath());
    }
    final SAXReader reader = getNewSaxReader();
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
  public static Document readDocument(final String document)
      throws DocumentException {
    return DocumentHelper.parseText(document);
  }

  /**
   * @param document
   *
   * @return the document as an XML string
   */
  public static String writeToString(final Document document) {
    return document.asXML();
  }

  /**
   * @param element
   *
   * @return the element as an XML string
   */
  public static String writeToString(final Element element) {
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
  public static void saveDocument(final String filename,
                                  final Document document) throws IOException {
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
  public static void saveDocument(final File file, final Document document)
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
  public static void saveDocument(final Writer outWriter,
                                  final Document document) throws IOException {
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
  public static void saveElement(final String filename, final Element element)
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
  public static void saveElement(final File file, final Element element)
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
  public static Element addOrSetElement(final Element ref, final String path,
                                        final String value) {
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
  public static Element addOrGetElement(final Element ref, final String path) {
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
  public static Element addAndSetElementMultiple(final Element ref,
                                                 final String path,
                                                 final String value) {
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
  public static Element addAndGetElementMultiple(final Element ref,
                                                 final String path) {
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
  public static Element getParentElement(final Element ref, final String path)
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
  public static Element getElement(final Element ref, final String path)
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
  public static List<Node> getElementMultiple(final Element ref,
                                              final String path)
      throws DocumentException {
    String npath = path;
    while (npath.charAt(0) == '/') {
      npath = npath.substring(1);
    }
    final List<Node> list = ref.selectNodes(npath);
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
  public static Element addOrSetElement(final Document doc, final String path,
                                        final String value) {
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
  public static Element addOrGetElement(final Document doc, final String path) {
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
  public static Element addAndSetElementMultiple(final Document doc,
                                                 final String path,
                                                 final String value) {
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
  public static Element addAndGetElementMultiple(final Document doc,
                                                 final String path) {
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
  public static Element getParentElement(final Document doc, final String path)
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
  public static Element getElement(final Document doc, final String path)
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
  public static List<Node> getElementMultiple(final Document doc,
                                              final String path)
      throws DocumentException {
    final List<Node> list = doc.selectNodes(path);
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
  public static String getExtraTrimed(final String string) {
    return string.replaceAll("[\\s]+", " ").trim();
    // was ("^[\\s]*|[\\s]*$ ", "")
  }

  /**
   * Create the XmlValues from the XmlDevls and the Document
   *
   * @param doc
   * @param decls
   *
   * @return XmlValues
   */
  public static XmlValue[] read(final Document doc, final XmlDecl[] decls) {
    final XmlValue[] values;
    final int len = decls.length;
    values = new XmlValue[len];
    for (int i = 0; i < len; i++) {
      final XmlValue value = new XmlValue(decls[i]);
      values[i] = value;
      if (decls[i].isSubXml()) {
        if (decls[i].isMultiple()) {
          final List<Node> elts;
          try {
            elts = getElementMultiple(doc, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          addValueToNodes(value, elts, decls[i]);
        } else {
          final Element element;
          try {
            element = getElement(doc, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          setValueToElement(value, read(element, decls[i].getSubXml()));
        }
      } else if (decls[i].isMultiple()) {
        final List<Node> elts;
        try {
          elts = getElementMultiple(doc, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        addFromStringToElements(value, elts);
      } else {
        final Element element;
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
  public static XmlValue[] read(final Element ref, final XmlDecl[] decls) {
    final XmlValue[] values;
    final int len = decls.length;
    values = new XmlValue[len];
    for (int i = 0; i < len; i++) {
      final XmlValue value = new XmlValue(decls[i]);
      values[i] = value;
      if (decls[i].isSubXml()) {
        if (decls[i].isMultiple()) {
          final List<Node> elts;
          try {
            elts = getElementMultiple(ref, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          addValueToNodes(value, elts, decls[i]);
        } else {
          final Element element;
          try {
            element = getElement(ref, decls[i].getXmlPath());
          } catch (final DocumentException e) {
            continue;
          }
          setValueToElement(value, read(element, decls[i].getSubXml()));
        }
      } else if (decls[i].isMultiple()) {
        final List<Node> elts;
        try {
          elts = getElementMultiple(ref, decls[i].getXmlPath());
        } catch (final DocumentException e) {
          continue;
        }
        addFromStringToElements(value, elts);
      } else {
        final Element element;
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

  private static void addFromStringToElements(final XmlValue value,
                                              final List<Node> elts) {
    for (final Node element : elts) {
      final String svalue = element.getText();
      try {
        value.addFromString(getExtraTrimed(svalue));
      } catch (final InvalidObjectException e) {
        // nothing
      } catch (final InvalidArgumentException e) {
        // nothing
      }
    }
  }

  private static void setValueToElement(final XmlValue value,
                                        final XmlValue[] read) {
    if (read == null) {
      return;
    }
    try {
      value.setValue(read);
    } catch (final InvalidObjectException e) {
      // nothing
    }
  }

  private static void addValueToNodes(final XmlValue value,
                                      final List<Node> elts,
                                      final XmlDecl decl) {
    for (final Node element : elts) {
      final XmlValue[] newValue = read((Element) element, decl.getSubXml());
      try {
        value.addValue(newValue);
      } catch (final InvalidObjectException e) {
        // nothing
      }
    }
  }

  /**
   * Add all nodes from XmlValues into Document
   *
   * @param doc
   * @param values
   */
  @SuppressWarnings("unchecked")
  public static void write(final Document doc, final XmlValue[] values) {
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
  public static void write(final Element ref, final XmlValue[] values) {
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
  public static void writeXML(final String filename, final String encoding,
                              final Document document) throws IOException {
    final OutputFormat format = OutputFormat.createPrettyPrint();
    if (encoding != null) {
      format.setEncoding(encoding);
    } else {
      format.setEncoding(WaarpStringUtils.UTF8.name());
    }
    final XMLWriter writer;
    writer = new XMLWriter(new FileWriter(filename), format);
    writer.write(document);
    try {
      writer.close();
    } catch (final IOException ignored) {
      // nothing
    }
  }
}
