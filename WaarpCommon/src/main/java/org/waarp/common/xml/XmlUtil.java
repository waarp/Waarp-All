/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Writer;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.utility.WaarpStringUtils;

/**
 * XML utility that handles simple cases as:<br>
 * <ul>
 * <li>XPath as /x/y/z for a node and referring to root of Document</li>
 * <li>XPath as x/y/z for a node and referring to current referenced node</li>
 * <li>Any XPath can be a singleton (unique node referenced by the XPath) or multiple nodes (same XPath)</li>
 * <li>Only Element as Node: no Attribute or any other type within XML</li>
 * <li>Any other path is not supported: //x /x@y ./ ../</li>
 * <li>Supports special SubXml tree as element (singleton or multiple)</li>
 * </ul>
 * 
 * @author Frederic Bregier
 * 
 */
public class XmlUtil {

    /**
     * 
     * @param filename
     * @return Existing Document from filename
     * @throws IOException
     * @throws DocumentException
     */
    static public Document getDocument(String filename) throws IOException,
            DocumentException {
        File file = new File(filename);
        if (!file.canRead()) {
            throw new IOException("File is not readable: " + filename);
        }
        return getDocument(file);
    }

    /**
     * 
     * @param file
     * @return Existing Document from file
     * @throws IOException
     * @throws DocumentException
     */
    static public Document getDocument(File file) throws IOException,
            DocumentException {
        if (!file.canRead()) {
            throw new IOException("File is not readable: " + file.getPath());
        }
        SAXReader reader = new SAXReader();
        return reader.read(file);
    }

    /**
     * Read the document from the string
     * 
     * @param document
     *            as String
     * @return the Document
     * @throws DocumentException
     */
    static public Document readDocument(String document)
            throws DocumentException {
        return DocumentHelper.parseText(document);
    }

    /**
     * 
     * @param document
     * @return the document as an XML string
     */
    static public String writeToString(Document document) {
        return document.asXML();
    }

    /**
     * 
     * @param element
     * @return the element as an XML string
     */
    static public String writeToString(Element element) {
        return element.asXML();
    }

    /**
     * 
     * @return an empty new Document
     */
    static public Document createEmptyDocument() {
        return DocumentHelper.createDocument();
    }

    /**
     * Save the document into the file
     * 
     * @param filename
     * @param document
     * @throws IOException
     */
    static public void saveDocument(String filename, Document document)
            throws IOException {
        File file = new File(filename);
        saveDocument(file, document);
    }

    /**
     * Save the document into the file
     * 
     * @param file
     * @param document
     * @throws IOException
     */
    static public void saveDocument(File file, Document document)
            throws IOException {
        if (file.exists() && (!file.canWrite())) {
            throw new IOException("File is not writable: " + file.getPath());
        }

        saveDocument(new FileWriter(file), document);
    }

    /**
     * Save the document into the Writer outWriter
     *
     * @param outWriter
     * @param document
     * @throws IOException
     */
    static public void saveDocument(Writer outWriter, Document document)
            throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding(WaarpStringUtils.UTF8.name());
        XMLWriter writer = new XMLWriter(outWriter, format);
        writer.write(document);
        writer.flush();
        writer.close();
    }

    /**
     * Save the branch from element into the file
     * 
     * @param filename
     * @param element
     * @throws IOException
     */
    static public void saveElement(String filename, Element element)
            throws IOException {
        File file = new File(filename);
        saveElement(file, element);
    }

    /**
     * Save the branch from element into the file
     * 
     * @param file
     * @param element
     * @throws IOException
     */
    static public void saveElement(File file, Element element)
            throws IOException {
        if (file.exists() && (!file.canWrite())) {
            throw new IOException("File is not writable: " + file.getPath());
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding(WaarpStringUtils.UTF8.name());
        XMLWriter writer = new XMLWriter(new FileWriter(file), format);
        writer.write(element);
        writer.flush();
        writer.close();
    }

    /**
     * 
     * @param document
     * @return the root Element from the document
     */
    static public Element getRootElement(Document document) {
        return document.getRootElement();
    }

    /**
     * Add or Get (if already existing) an element given by the path relative to the referent
     * element and set the value
     * 
     * @param ref
     * @param path
     * @param value
     * @return the new added or already existing element with new value
     */
    static public Element addOrSetElement(Element ref, String path, String value) {
        Element current = addOrGetElement(ref, path);
        current.setText(value);
        return current;
    }

    /**
     * Add or Get (if already existing) an element given by the path relative to the referent
     * element
     * 
     * @param ref
     * @param path
     * @return the new added or already existing element
     */
    static public Element addOrGetElement(Element ref, String path) {
        String[] pathes = path.split("/");
        Element current = ref;
        for (String nodename : pathes) {
            if (!nodename.isEmpty()) {
                Element exist = current.element(nodename);
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
     * Add an element given by the path relative to the referent element and set the value
     * 
     * @param ref
     * @param path
     * @param value
     * @return the new added element with value
     */
    static public Element addAndSetElementMultiple(Element ref, String path,
            String value) {
        Element current = addAndGetElementMultiple(ref, path);
        current.setText(value);
        return current;
    }

    /**
     * Add an element given by the path relative to the referent element
     * 
     * @param ref
     * @param path
     * @return the new added element
     */
    static public Element addAndGetElementMultiple(Element ref, String path) {
        String[] pathes = path.split("/");
        Element current = ref;
        for (int i = 0; i < pathes.length - 1; i++) {
            String nodename = pathes[i];
            if (!nodename.isEmpty()) {
                Element exist = current.element(nodename);
                if (exist == null) {
                    current = current.addElement(nodename);
                } else {
                    current = exist;
                }
            }
        }
        String nodename = pathes[pathes.length - 1];
        if (!nodename.isEmpty()) {
            current = current.addElement(nodename);
        }
        return current;
    }

    /**
     * 
     * @param ref
     * @param path
     * @return the parent element associated with the path relatively to the referent element
     * @throws DocumentException
     */
    static public Element getParentElement(Element ref, String path)
            throws DocumentException {
        String npath = path;
        while (npath.charAt(0) == '/') {// startsWith("/")) {
            npath = npath.substring(1);
        }
        Element current = (Element) ref.selectSingleNode(npath);
        if (current == null) {
            throw new DocumentException("Node not found: " + path);
        }
        return current.getParent();
    }

    /**
     * 
     * @param ref
     * @param path
     * @return the element associated with the path relatively to the referent element
     * @throws DocumentException
     */
    static public Element getElement(Element ref, String path)
            throws DocumentException {
        String npath = path;
        while (npath.charAt(0) == '/') {// .startsWith("/")) {
            npath = npath.substring(1);
        }
        Element current = (Element) ref.selectSingleNode(npath);
        if (current == null) {
            throw new DocumentException("Node not found: " + path);
        }
        return current;
    }

    /**
     * 
     * @param ref
     * @param path
     * @return the element associated with the path relatively to the referent element
     * @throws DocumentException
     */
    @SuppressWarnings("unchecked")
    static public List<Element> getElementMultiple(Element ref, String path)
            throws DocumentException {
        String npath = path;
        while (npath.charAt(0) == '/') {// .startsWith("/")) {
            npath = npath.substring(1);
        }
        List<Element> list = ref.selectNodes(npath);
        if (list == null || list.isEmpty()) {
            throw new DocumentException("Nodes not found: " + path);
        }
        return list;
    }

    /**
     * Add or Get (if already existing) an element given by the path relative to the document and
     * set the value
     * 
     * @param doc
     * @param path
     * @param value
     * @return the new added or already existing element with new value
     */
    static public Element addOrSetElement(Document doc, String path,
            String value) {
        Element current = addOrGetElement(doc, path);
        current.setText(value);
        return current;
    }

    /**
     * Add or Get (if already existing) an element given by the path relative to the document
     * 
     * @param doc
     * @param path
     * @return the new added or already existing element
     */
    static public Element addOrGetElement(Document doc, String path) {
        String[] pathes = path.split("/");
        int rank = 0;
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
            String nodename = pathes[i];
            if (!nodename.isEmpty()) {
                Element exist = current.element(nodename);
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
     * Add an element given by the path relative to the document and set the value
     * 
     * @param doc
     * @param path
     * @param value
     * @return the new added element with value
     */
    static public Element addAndSetElementMultiple(Document doc, String path,
            String value) {
        Element current = addAndGetElementMultiple(doc, path);
        current.setText(value);
        return current;
    }

    /**
     * Add an element given by the path relative to the document
     * 
     * @param doc
     * @param path
     * @return the new added element
     */
    static public Element addAndGetElementMultiple(Document doc, String path) {
        String[] pathes = path.split("/");
        int rank = 0;
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
            String nodename = pathes[i];
            if (!nodename.isEmpty()) {
                Element exist = current.element(nodename);
                if (exist == null) {
                    current = current.addElement(nodename);
                } else {
                    current = exist;
                }
            }
        }
        String nodename = pathes[pathes.length - 1];
        if (!nodename.isEmpty()) {
            current = current.addElement(nodename);
        }
        return current;
    }

    /**
     * 
     * @param doc
     * @param path
     * @return the Parent element associated with the path relatively to the document
     * @throws DocumentException
     */
    static public Element getParentElement(Document doc, String path)
            throws DocumentException {
        Element current = (Element) doc.selectSingleNode(path);
        if (current == null) {
            throw new DocumentException("Node not found: " + path);
        }
        return current.getParent();
    }

    /**
     * 
     * @param doc
     * @param path
     * @return the element associated with the path relatively to the document
     * @throws DocumentException
     */
    static public Element getElement(Document doc, String path)
            throws DocumentException {
        Element current = (Element) doc.selectSingleNode(path);
        if (current == null) {
            throw new DocumentException("Node not found: " + path);
        }
        return current;
    }

    /**
     * 
     * @param doc
     * @param path
     * @return the element associated with the path relatively to the document
     * @throws DocumentException
     */
    @SuppressWarnings("unchecked")
    static public List<Element> getElementMultiple(Document doc, String path)
            throws DocumentException {
        List<Element> list = doc.selectNodes(path);
        if (list == null || list.isEmpty()) {
            throw new DocumentException("Nodes not found: " + path);
        }
        return list;
    }

    /**
     * Create the XmlValues from the XmlDevls and the Document
     * 
     * @param doc
     * @param decls
     * @return XmlValues
     */
    static public XmlValue[] read(Document doc, XmlDecl[] decls) {
        XmlValue[] values = null;
        int len = decls.length;
        values = new XmlValue[len];
        for (int i = 0; i < len; i++) {
            XmlValue value = new XmlValue(decls[i]);
            values[i] = value;
            if (decls[i].isSubXml()) {
                if (decls[i].isMultiple()) {
                    List<Element> elts;
                    try {
                        elts = getElementMultiple(doc, decls[i].getXmlPath());
                    } catch (DocumentException e) {
                        continue;
                    }
                    for (Element element : elts) {
                        XmlValue[] newValue = read(element,
                                decls[i].getSubXml());
                        if (newValue == null) {
                            continue;
                        }
                        try {
                            value.addValue(newValue);
                        } catch (InvalidObjectException e) {
                            continue;
                        }
                    }
                } else {
                    Element element;
                    try {
                        element = getElement(doc, decls[i].getXmlPath());
                    } catch (DocumentException e) {
                        continue;
                    }
                    XmlValue[] newValue = read(element, decls[i].getSubXml());
                    if (newValue == null) {
                        continue;
                    }
                    try {
                        value.setValue(newValue);
                    } catch (InvalidObjectException e) {
                        continue;
                    }
                }
            } else if (decls[i].isMultiple()) {
                List<Element> elts;
                try {
                    elts = getElementMultiple(doc, decls[i].getXmlPath());
                } catch (DocumentException e) {
                    continue;
                }
                for (Element element : elts) {
                    String svalue = element.getText();
                    try {
                        value.addFromString(svalue);
                    } catch (InvalidObjectException e) {
                        continue;
                    } catch (InvalidArgumentException e) {
                        continue;
                    }
                }
            } else {
                Element element;
                try {
                    element = getElement(doc, decls[i].getXmlPath());
                } catch (DocumentException e) {
                    continue;
                }
                String svalue = element.getText();
                try {
                    value.setFromString(svalue);
                } catch (InvalidArgumentException e) {
                    continue;
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
     * @return XmlValues
     */
    static public XmlValue[] read(Element ref, XmlDecl[] decls) {
        XmlValue[] values = null;
        int len = decls.length;
        values = new XmlValue[len];
        for (int i = 0; i < len; i++) {
            XmlValue value = new XmlValue(decls[i]);
            values[i] = value;
            if (decls[i].isSubXml()) {
                if (decls[i].isMultiple()) {
                    List<Element> elts;
                    try {
                        elts = getElementMultiple(ref, decls[i].getXmlPath());
                    } catch (DocumentException e) {
                        continue;
                    }
                    for (Element element : elts) {
                        XmlValue[] newValue = read(element,
                                decls[i].getSubXml());
                        if (newValue == null) {
                            continue;
                        }
                        try {
                            value.addValue(newValue);
                        } catch (InvalidObjectException e) {
                            continue;
                        }
                    }
                } else {
                    Element element;
                    try {
                        element = getElement(ref, decls[i].getXmlPath());
                    } catch (DocumentException e) {
                        continue;
                    }
                    XmlValue[] newValue = read(element, decls[i].getSubXml());
                    if (newValue == null) {
                        continue;
                    }
                    try {
                        value.setValue(newValue);
                    } catch (InvalidObjectException e) {
                        continue;
                    }
                }
            } else if (decls[i].isMultiple()) {
                List<Element> elts;
                try {
                    elts = getElementMultiple(ref, decls[i].getXmlPath());
                } catch (DocumentException e) {
                    continue;
                }
                for (Element element : elts) {
                    String svalue = element.getText();
                    try {
                        value.addFromString(svalue);
                    } catch (InvalidObjectException e) {
                        continue;
                    } catch (InvalidArgumentException e) {
                        continue;
                    }
                }
            } else {
                Element element;
                try {
                    element = getElement(ref, decls[i].getXmlPath());
                } catch (DocumentException e) {
                    continue;
                }
                String svalue = element.getText();
                try {
                    value.setFromString(svalue);
                } catch (InvalidArgumentException e) {
                    continue;
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
        int len = values.length;
        for (int i = 0; i < len; i++) {
            if (values[i] != null) {
                if (values[i].isSubXml()) {
                    if (values[i].isMultiple()) {
                        List<XmlValue[]> list = (List<XmlValue[]>) values[i]
                                .getList();
                        for (XmlValue[] object : list) {
                            Element ref = addAndGetElementMultiple(doc,
                                    values[i].getXmlPath());
                            write(ref, object);
                        }
                    } else {
                        Element ref = addOrGetElement(doc,
                                values[i].getXmlPath());
                        write(ref, values[i].getSubXml());
                    }
                } else if (values[i].isMultiple()) {
                    List<?> list = values[i].getList();
                    for (Object object : list) {
                        addAndSetElementMultiple(doc, values[i].getXmlPath(),
                                object.toString());
                    }
                } else {
                    addOrSetElement(doc, values[i].getXmlPath(),
                            values[i].getIntoString());
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
        int len = values.length;
        for (int i = 0; i < len; i++) {
            if (values[i] != null) {
                if (values[i].isSubXml()) {
                    if (values[i].isMultiple()) {
                        List<XmlValue[]> list = (List<XmlValue[]>) values[i]
                                .getList();
                        for (XmlValue[] object : list) {
                            Element newref = addAndGetElementMultiple(ref,
                                    values[i].getXmlPath());
                            write(newref, object);
                        }
                    } else {
                        Element newref = addOrGetElement(ref,
                                values[i].getXmlPath());
                        write(newref, values[i].getSubXml());
                    }
                } else if (values[i].isMultiple()) {
                    List<?> list = values[i].getList();
                    for (Object object : list) {
                        addAndSetElementMultiple(ref, values[i].getXmlPath(),
                                object.toString());
                    }
                } else {
                    addOrSetElement(ref, values[i].getXmlPath(),
                            values[i].getIntoString());
                }
            }
        }
    }

    /**
     * Example (run test)
     * 
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        XmlDecl[] decls = {
                new XmlDecl("n1", XmlType.BOOLEAN, "/root/bool", false),
                new XmlDecl("n2", XmlType.FLOAT, "/root/float", false),
                new XmlDecl("n3", XmlType.STRING, "/root/string", false),
                new XmlDecl("n4", XmlType.INTEGER, "/root/int", false),
                new XmlDecl("n5", XmlType.BOOLEAN, "/root/sub1/sub2/bool",
                        false),
                new XmlDecl("n6", XmlType.BOOLEAN, "/root/sub1/sub3/bool", true),
                new XmlDecl("n7", XmlType.BOOLEAN,
                        "/root/sub4/sub5/sub6/sub7/bool", false) };
        String xmltree = "<root><bool>true</bool><float>1.2</float><string>my string to read</string><int>5</int>"
                + "<sub1>"
                + "<sub2><bool>1</bool></sub2>"
                + "<sub3>"
                + "<bool>false</bool><bool>true</bool><bool>0</bool>"
                + "</sub3>"
                + "</sub1>"
                + "<sub4><sub5><sub6><sub7><bool>True</bool></sub7></sub6></sub5></sub4>"
                + "</root>";
        // Test with only single XmlType (no XVAL)
        Document document = null;
        try {
            document = readDocument(xmltree);
        } catch (DocumentException e) {
            e.printStackTrace();
            return;
        }
        try {
            saveDocument("D:\\Tools\\test-src.xml", document);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        XmlValue[] values = read(document, decls);
        Document newDoc = createEmptyDocument();
        write(newDoc, values);
        String text = writeToString(newDoc);
        System.err.println(text);
        try {
            saveDocument("D:\\Tools\\test.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            newDoc = getDocument("D:\\Tools\\test.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (DocumentException e) {
            e.printStackTrace();
            return;
        }
        values = read(newDoc, decls);
        XmlHash hash = new XmlHash(values);
        XmlValue value = hash.get("n6");
        if (value != null) {
            if (value.isMultiple()) {
                try {
                    value.addFromString("true");
                } catch (InvalidObjectException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        value = hash.get("n3");
        if (value != null) {
            if (!value.isMultiple()) {
                try {
                    value.setValue("New String as replacement");
                } catch (InvalidObjectException e) {
                    e.printStackTrace();
                }
            }
        }
        newDoc = createEmptyDocument();
        write(newDoc, values);
        try {
            saveDocument("D:\\Tools\\test2.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Element elt = null;
        try {
            elt = getElement(newDoc, "/root/sub4");
        } catch (DocumentException e1) {
            e1.printStackTrace();
            return;
        }
        XmlValue[] oldValues = read(document, decls);
        write(elt, oldValues);
        try {
            saveDocument("D:\\Tools\\test3.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        write(newDoc, oldValues);
        try {
            saveDocument("D:\\Tools\\test4.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            saveElement("D:\\Tools\\test5.xml",
                    (Element) elt.selectSingleNode("root"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // Now test XmlType.XVAL
        System.err.println("\n\nXVAL TESTING\n");
        XmlDecl[] subdecls1 = {
                new XmlDecl("n11", XmlType.BOOLEAN, "/n11/bool", false),
                new XmlDecl("n12", XmlType.FLOAT, "/n11/float", true) };
        XmlDecl[] subdecls2 = {
                new XmlDecl("n21", XmlType.BOOLEAN, "/n21/bool", false),
                new XmlDecl("n22", XmlType.FLOAT, "/n21/float", true) };
        XmlDecl[] maindecls = {
                new XmlDecl("n1", XmlType.BOOLEAN, "/root/bool", false),
                new XmlDecl("n2", XmlType.FLOAT, "/root/float", false),
                new XmlDecl("n3", XmlType.STRING, "/root/string", false),
                new XmlDecl("n4", XmlType.INTEGER, "/root/int", false),
                new XmlDecl("n5", XmlType.BOOLEAN, "/root/sub1/sub2/bool",
                        false),
                new XmlDecl("n6", XmlType.BOOLEAN, "/root/sub1/sub3/bool", true),
                new XmlDecl("n7", XmlType.BOOLEAN,
                        "/root/sub4/sub5/sub6/sub7/bool", false),
                new XmlDecl("n10", XmlType.XVAL, "/root/sub8", subdecls1, false),
                new XmlDecl("n20", XmlType.XVAL, "/root/sub9", subdecls2, true) };
        xmltree = "<root><bool>true</bool><float>1.2</float><string>my string to read</string><int>5</int>"
                + "<sub1>"
                + "<sub2><bool>1</bool></sub2>"
                + "<sub3>"
                + "<bool>false</bool><bool>true</bool><bool>0</bool>"
                + "</sub3>"
                + "</sub1>"
                + "<sub4><sub5><sub6><sub7><bool>True</bool></sub7></sub6></sub5></sub4>"
                + "<sub8><n11><bool>true</bool><float>1.2</float><float>1.3</float><float>1.4</float></n11></sub8>"
                + "<sub9><n21><bool>true</bool><float>2.2</float><float>2.3</float><float>2.4</float></n21></sub9>"
                + "<sub9><n21><bool>false</bool><float>3.2</float><float>3.3</float><float>3.4</float></n21></sub9>"
                + "<sub9><n21><bool>true</bool><float>4.2</float><float>4.3</float><float>4.4</float></n21></sub9>"
                + "</root>";
        document = null;
        try {
            document = readDocument(xmltree);
        } catch (DocumentException e) {
            e.printStackTrace();
            return;
        }
        try {
            saveDocument("D:\\Tools\\xtest-src.xml", document);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        values = read(document, maindecls);
        newDoc = createEmptyDocument();
        write(newDoc, values);
        text = writeToString(newDoc);
        System.err.println(text);
        try {
            saveDocument("D:\\Tools\\xtest.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            newDoc = getDocument("D:\\Tools\\xtest.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (DocumentException e) {
            e.printStackTrace();
            return;
        }
        values = read(newDoc, maindecls);
        hash = new XmlHash(values);
        value = hash.get("n10");
        if (value != null) {
            if (!value.isMultiple()) {
                XmlValue[] subvalues = value.getSubXml();
                XmlHash subhash = new XmlHash(subvalues);
                value = subhash.get("n12");
                try {
                    value.addFromString("10.3");
                } catch (InvalidObjectException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        value = hash.get("n20");
        if (value != null) {
            if (value.isMultiple()) {
                List<XmlValue[]> listvalues = (List<XmlValue[]>) value.getList();
                XmlValue[] subvalues = listvalues.get(0);
                XmlValue[] subvalues2 = new XmlValue[subvalues.length];
                // Create a perfect clone
                for (int i = 0; i < subvalues.length; i++) {
                    subvalues2[i] = new XmlValue(subvalues[i]);
                }
                // Add it at the end
                listvalues.add(subvalues2);
                XmlHash subhash = new XmlHash(subvalues2);
                value = subhash.get("n21");
                try {
                    value.setValue(Boolean.FALSE);
                } catch (InvalidObjectException e) {
                    e.printStackTrace();
                }
            }
        }
        newDoc = createEmptyDocument();
        write(newDoc, values);
        try {
            saveDocument("D:\\Tools\\xtest2.xml", newDoc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        elt = null;
        try {
            elt = getElement(newDoc, "/root/sub9");
        } catch (DocumentException e1) {
            e1.printStackTrace();
            return;
        }
        try {
            saveElement("D:\\Tools\\xtest5.xml",
                    elt);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Write the given XML document to filename using the encoding
     * 
     * @param filename
     * @param encoding
     *            if null, default encoding UTF-8 will be used
     * @param document
     * @throws IOException
     */
    public static void writeXML(String filename, String encoding, Document document)
            throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        if (encoding != null) {
            format.setEncoding(encoding);
        } else {
            format.setEncoding(WaarpStringUtils.UTF8.name());
        }
        XMLWriter writer = null;
        writer = new XMLWriter(new FileWriter(filename), format);
        writer.write(document);
        try {
            writer.close();
        } catch (IOException e) {
        }
    }
}
