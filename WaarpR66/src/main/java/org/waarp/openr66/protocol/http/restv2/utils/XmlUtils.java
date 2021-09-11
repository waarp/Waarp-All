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

package org.waarp.openr66.protocol.http.restv2.utils;

import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrors;

import javax.ws.rs.InternalServerErrorException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static javax.xml.transform.OutputKeys.*;

/**
 * A series of utility methods for serializing and deserializing XML.
 */
public final class XmlUtils {

  /**
   * Prevents the default constructor from being called.
   */
  private XmlUtils() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ######################### PUBLIC METHODS #################################

  /**
   * Converts a serializable Java object into XML format as a String.
   *
   * @param object the object to convert to XML
   *
   * @return the object's representation in XML
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static String objectToXml(final XmlSerializable object) {
    try {
      final StringWriter writer = new StringWriter();
      final JAXBContext context = JAXBContext.newInstance(object.getClass());
      final Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
      marshaller.marshal(object, writer);

      return writer.toString();
    } catch (final JAXBException e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Converts an XML String into a serializable Java object.
   *
   * @param xml the string to convert into an object
   * @param clazz the class of the serializable object
   *
   * @return the deserialized Java object
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static <T extends XmlSerializable> T xmlToObject(final String xml,
                                                          final Class<T> clazz) {
    try {
      ParametersChecker.checkSanityString(xml);
    } catch (final InvalidArgumentException e) {
      throw new InternalServerErrorException(e);
    }
    try {
      final StringReader reader = new StringReader(xml);
      final StreamSource source = new StreamSource(reader);
      final JAXBContext context = JAXBContext.newInstance(clazz);
      final Unmarshaller unmarshaller = context.createUnmarshaller();

      return unmarshaller.unmarshal(source, clazz).getValue();
    } catch (final JAXBException e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Saves an XML String to a file at the given location.
   *
   * @param xml the XML String
   * @param filePath the path where to save the XML file
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static void saveXML(final String xml, final String filePath) {
    try {
      ParametersChecker.checkSanityString(xml);
    } catch (final InvalidArgumentException e) {
      throw new InternalServerErrorException(e);
    }
    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(filePath, false);
      final String formattedXML = pretty(xml);
      fileWriter.write(formattedXML);
      fileWriter.flush();
    } catch (final IOException e) {
      throw new InternalServerErrorException(e);
    } finally {
      FileUtils.close(fileWriter);
    }
  }

  /**
   * Loads an XML file into a String.
   *
   * @param filePath the path of the XML file to load
   *
   * @return the content of the XML file
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static String loadXML(final String filePath) {
    FileReader fr = null;
    BufferedReader buff = null;
    try {
      fr = new FileReader(filePath);
      buff = new BufferedReader(fr);
      final StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = buff.readLine()) != null) {
        stringBuilder.append(line.trim());
      }
      return stringBuilder.toString();
    } catch (final FileNotFoundException e) {
      throw new RestErrorException(RestErrors.FILE_NOT_FOUND(filePath));
    } catch (final IOException e) {
      throw new InternalServerErrorException(e);
    } finally {
      FileUtils.close(buff);
      FileUtils.close(fr);
    }
  }

  /**
   * Saves a serializable Java object to an XML file at the given location.
   *
   * @param object the object to save as XML
   * @param filePath the path where to save the XML file
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static void saveObject(final XmlSerializable object,
                                final String filePath) {

    final String xml = objectToXml(object);
    saveXML(xml, filePath);
  }

  /**
   * Loads the given XML file into a corresponding serializable Java object.
   *
   * @param filePath path of the file to load
   * @param clazz class of the target Java object
   *
   * @return the deserialized XML object
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static <T extends XmlSerializable> T loadObject(final String filePath,
                                                         final Class<T> clazz) {

    final String xml = loadXML(filePath);
    return xmlToObject(xml, clazz);
  }

  // ######################### PRIVATE METHODS #################################

  /**
   * Formats an unformatted XML String into a human readable one.
   *
   * @param input The unformatted XML String.
   *
   * @return The XML String in human readable format.
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  private static String pretty(final String input) {
    if (ParametersChecker.isEmpty(input)) {
      throw new InternalServerErrorException("Input empty but should not");
    }
    try {
      final Source xmlInput = new StreamSource(new StringReader(input));
      final StringWriter stringWriter = new StringWriter();
      final StreamResult xmlOutput = new StreamResult(stringWriter);
      final TransformerFactory factory =//NOSONAR
          TransformerFactory.newInstance();//NOSONAR
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setAttribute("indent-number", 2);
      final Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(INDENT, "yes");
      transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
      transformer.transform(xmlInput, xmlOutput);
      return xmlOutput.getWriter().toString();
    } catch (final TransformerConfigurationException e) {
      throw new InternalServerErrorException(e);
    } catch (final TransformerException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
