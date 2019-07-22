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

package org.waarp.openr66.dao.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XMLUtils {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(XMLUtils.class);

  public static Node createNode(Document document, String tag, String text) {
    final Node res = document.createElement(tag);
    res.setTextContent(text);
    return res;
  }

  public static void writeToFile(File file, Document document) {
    final TransformerFactory transformerFactory =
        TransformerFactory.newInstance();
    try {
      final Transformer transformer = transformerFactory.newTransformer();
      final DOMSource domSource = new DOMSource(document);
      final StreamResult streamResult = new StreamResult(file);
      transformer.transform(domSource, streamResult);
    } catch (final TransformerException e) {
      logger.error("Error while writing document to file", e);
    }
  }
}
