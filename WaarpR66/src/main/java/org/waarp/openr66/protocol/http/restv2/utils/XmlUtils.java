/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 * Copyright 2009, Waarp SAS, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */

package org.waarp.openr66.protocol.http.restv2.utils;

import org.waarp.openr66.protocol.http.restv2.errors.RestErrors;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import javax.ws.rs.InternalServerErrorException;
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

import static com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.INDENT_NUMBER;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;


/** A series of utility methods for serializing and deserializing XML. */
public final class XmlUtils {

    /** Prevents the default constructor from being called. */
    private XmlUtils() throws InstantiationException {
        throw new InstantiationException(this.getClass().getName() +
                " cannot be instantiated.");
    }


    //######################### PUBLIC METHODS #################################

    /**
     * Converts a serializable Java object into XML format as a String.
     *
     * @param object the object to convert to XML
     * @return       the object's representation in XML
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static String objectToXml(XmlSerializable object) {
        try {
            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(object, writer);

            return writer.toString();
        } catch (JAXBException e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Converts an XML String into a serializable Java object.
     *
     * @param xml  the string to convert into an object
     * @param clazz the class of the serializable object
     * @return     the deserialized Java object
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static <T extends XmlSerializable> T xmlToObject(String xml,
                                                            Class<T> clazz) {
        try {
            StringReader reader = new StringReader(xml);
            StreamSource source = new StreamSource(reader);
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return unmarshaller.unmarshal(source, clazz).getValue();
        } catch (JAXBException e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Saves an XML String to a file at the given location.
     *
     * @param xml      the XML String
     * @param filePath the path where to save the XML file
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static void saveXML(String xml, String filePath) {
        try {
            FileWriter fileWriter = new FileWriter(filePath, false);
            String formattedXML = pretty(xml);
            fileWriter.write(formattedXML);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Loads an XML file into a String.
     *
     * @param filePath the path of the XML file to load
     * @return         the content of the XML file
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static String loadXML(String filePath) {
        try {
            BufferedReader buff = new BufferedReader(new FileReader(filePath));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = buff.readLine()) != null) {
                stringBuilder.append(line.trim());
            }
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            throw new RestErrorException(RestErrors.FILE_NOT_FOUND(filePath));
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Saves a serializable Java object to an XML file at the given location.
     *
     * @param object    the object to save as XML
     * @param filePath  the path where to save the XML file
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static void saveObject(XmlSerializable object, String filePath) {

        String xml = objectToXml(object);
        saveXML(xml, filePath);
    }

    /**
     * Loads the given XML file into a corresponding serializable Java object.
     *
     * @param filePath path of the file to load
     * @param clazz    class of the target Java object
     * @return         the deserialized XML object
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    public static <T extends XmlSerializable> T loadObject(String filePath,
                                                           Class<T> clazz) {

        String xml = loadXML(filePath);
        return xmlToObject(xml, clazz);
    }

    //######################### PRIVATE METHODS #################################

    /**
     * Formats an unformatted XML String into a human readable one.
     *
     * @param input   The unformatted XML String.
     * @return      The XML String in human readable format.
     * @throws InternalServerErrorException if an unexpected error occurred
     */
    private static String pretty(String input) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(INDENT_NUMBER, 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(INDENT, "yes");
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");

            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (TransformerConfigurationException e) {
            throw new InternalServerErrorException(e);
        } catch (TransformerException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
