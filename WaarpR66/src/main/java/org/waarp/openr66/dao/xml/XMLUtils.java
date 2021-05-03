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
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public final class XMLUtils {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(XMLUtils.class);

  private XMLUtils() {
  }

  public static Node createNode(final Document document, final String tag,
                                final String text) {
    final Node res = document.createElement(tag);
    res.setTextContent(text);
    return res;
  }

  public static void writeToFile(final File file, final Document document) {
    final TransformerFactory factory =//NOSONAR
        TransformerFactory.newInstance();//NOSONAR
    FileOutputStream outputStream = null;
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      final Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final DOMSource domSource = new DOMSource(document);
      outputStream = new FileOutputStream(file);
      final StreamResult streamResult = new StreamResult(outputStream);
      transformer.transform(domSource, streamResult);
    } catch (final TransformerException e) {
      logger.error("Error while writing document to file: {}", e.getMessage());
    } catch (FileNotFoundException e) {
      logger.error("Error while writing document to file: {}", e.getMessage());
    } finally {
      if (outputStream != null) {
        FileUtils.close(outputStream);
      }
    }
  }
}
