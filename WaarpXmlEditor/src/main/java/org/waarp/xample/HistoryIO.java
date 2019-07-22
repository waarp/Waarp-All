/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.waarp.xample;

/*
 * Copyright (c) 2003 Felix Golubov
 */

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * A helper class for the XAmple application which reads and writes history of
 * accessed XSD and XML files. The history
 * is stored in the XML format.
 *
 * @author Felix Golubov
 * @version 2.0
 */

public class HistoryIO {
  public static final String HISTORY_FILE_NAME = "history.xml";
  public static final String HISTORY = "history";
  public static final String XSD_DOC = "xsd-doc";
  public static final String XML_DOC = "xml-doc";
  public static final String FILE = "file";

  static void populateHistory(History history, Element element) {
    NodeList lst = element.getChildNodes();
    history.items = new ArrayList();
    for (int i = lst.getLength() - 1; i >= 0; i--) {
      if (!(lst.item(i) instanceof Element)) {
        continue;
      }
      Element child = (Element) lst.item(i);
      String path = child.getAttribute(FILE);
      if (path != null && path.length() > 0) {
        History childHistory = history.put(path);
        populateHistory(childHistory, child);
      }
    }
  }

  public static void load(History history) {
    File file = new File(HISTORY_FILE_NAME);
    if (!file.exists()) {
      return;
    }
    Element root = null;
    try {
      DOMParser parser = new DOMParser();
      parser.parse(file.toURI().toURL().toString());
      Document doc = parser.getDocument();
      root = doc.getDocumentElement();
      populateHistory(history, root);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void save(History history) {
    Document doc = new DocumentImpl();
    Element root = doc.createElement(HISTORY);
    doc.appendChild(root);
    if (history.items != null) {
      for (int i = 0; i < history.items.size(); i++) {
        History hXSD = (History) history.items.get(i);
        Element eXSD = doc.createElement(XSD_DOC);
        eXSD.setAttribute(FILE, hXSD.path);
        root.appendChild(eXSD);
        if (hXSD.items == null) {
          continue;
        }
        for (int j = 0; j < hXSD.items.size(); j++) {
          History hXML = (History) hXSD.items.get(j);
          Element eXML = doc.createElement(XML_DOC);
          eXML.setAttribute(FILE, hXML.path);
          eXSD.appendChild(eXML);
        }
      }
    }
    File file = new File(HISTORY_FILE_NAME);
    FileOutputStream out = null;
    try {
      file.createNewFile();
      out = new FileOutputStream(file);
      OutputFormat format = new OutputFormat(doc, "UTF-8", true);
      Writer writer = new OutputStreamWriter(out);
      XMLSerializer serial = new XMLSerializer(writer, format);
      serial.asDOMSerializer();
      serial.serialize(doc);
    } catch (Exception ex) {
    } finally {
      try {
        out.close();
      } catch (Exception ignore) {
      }
    }
  }

}
