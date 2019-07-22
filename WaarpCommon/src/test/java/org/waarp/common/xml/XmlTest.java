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

package org.waarp.common.xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.waarp.common.exception.InvalidArgumentException;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.*;

public class XmlTest {
  public static final String ROOT = "rule";
  public static final String XIDRULE = "idrule";
  public static final String XTASKS = "tasks";
  public static final String XTASK = "task";

  @Test
  public void testStructure() {
    final String TASK = "/task";
    final XmlDecl[] taskDecl = {
        new XmlDecl(XmlType.STRING, "string"),
        new XmlDecl(XmlType.BOOLEAN, "boolean"),
        new XmlDecl(XmlType.LONG, "long"),
        new XmlDecl(XmlType.DOUBLE, "double"),
        new XmlDecl(XmlType.FLOAT, "float"),
        new XmlDecl(XmlType.BYTE, "byte"),
        new XmlDecl(XmlType.CHARACTER, "character"),
        new XmlDecl(XmlType.SHORT, "short"),
        new XmlDecl(XmlType.SQLDATE, "sqldate"),
        new XmlDecl(XmlType.TIMESTAMP, "timestamp"),
        new XmlDecl(XmlType.EMPTY, "empty"),
        new XmlDecl(XmlType.INTEGER, "integer")
    };
    final XmlDecl[] tasksDecl = {
        new XmlDecl(XTASK,
                    XmlType.XVAL, TASK,
                    taskDecl, true)
    };
    final XmlDecl[] subruleDecls = {
        new XmlDecl(XmlType.STRING, XIDRULE),
        new XmlDecl(XTASKS, XmlType.XVAL, XTASKS, tasksDecl, false)
    };
    assertTrue(taskDecl[0].isCompatible(taskDecl[0]));
    assertFalse(taskDecl[0].isMultiple());
    assertFalse(taskDecl[0].isSubXml());
    assertTrue(tasksDecl[0].isMultiple());
    assertTrue(tasksDecl[0].isSubXml());
  }

  @Test
  public void testXmlType() {
    assertTrue(XmlType.STRING.isNativelyCompatible("string"));
    assertTrue(XmlType.INTEGER.isNativelyCompatible(Integer.MIN_VALUE));
    assertFalse(XmlType.INTEGER.isNativelyCompatible("string"));
    assertTrue(XmlType.SHORT.isNativelyCompatible(Short.MIN_VALUE));
    assertTrue(XmlType.CHARACTER.isNativelyCompatible(Character.MIN_VALUE));
    assertTrue(XmlType.BYTE.isNativelyCompatible(Byte.MIN_VALUE));
    assertTrue(XmlType.FLOAT.isNativelyCompatible(Float.MIN_VALUE));
    assertTrue(XmlType.DOUBLE.isNativelyCompatible(Double.MIN_VALUE));
    assertTrue(XmlType.LONG.isNativelyCompatible(Long.MIN_VALUE));
    assertTrue(XmlType.BOOLEAN.isNativelyCompatible(Boolean.TRUE));
    assertTrue(XmlType.TIMESTAMP.isNativelyCompatible(new Timestamp(1)));
    assertTrue(XmlType.SQLDATE.isNativelyCompatible(new Date(1)));
    assertFalse(XmlType.EMPTY.isNativelyCompatible("string"));
  }

  @Test
  public void testXml() {
    final String TASK = "/task";
    final XmlDecl[] taskDecl = {
        new XmlDecl(XmlType.STRING, "string"),
        new XmlDecl(XmlType.BOOLEAN, "boolean"),
        new XmlDecl(XmlType.LONG, "long"),
        new XmlDecl(XmlType.DOUBLE, "double"),
        new XmlDecl(XmlType.FLOAT, "float"),
        new XmlDecl(XmlType.BYTE, "byte"),
        new XmlDecl(XmlType.CHARACTER, "character"),
        new XmlDecl(XmlType.SHORT, "short"),
        new XmlDecl(XmlType.SQLDATE, "sqldate"),
        new XmlDecl(XmlType.TIMESTAMP, "timestamp"),
        new XmlDecl(XmlType.EMPTY, "empty"),
        new XmlDecl(XmlType.INTEGER, "integer")
    };
    final XmlDecl[] tasksDecl = {
        new XmlDecl(XTASK,
                    XmlType.XVAL, TASK,
                    taskDecl, true)
    };
    final XmlDecl[] subruleDecls = {
        new XmlDecl(XmlType.STRING, XIDRULE),
        new XmlDecl(XTASKS, XmlType.XVAL, XTASKS, tasksDecl, false)
    };
    final XmlDecl[] ruleDecls = {
        new XmlDecl(ROOT, XmlType.XVAL,
                    ROOT, subruleDecls,
                    false)
    };

    SAXReader reader = new SAXReader();
    Document document = null;
    long time = System.currentTimeMillis();
    String source = "<" + ROOT + ">" +
                    "<" + XIDRULE + ">id" + "</" + XIDRULE + ">" +
                    "<" + XTASKS + ">" +
                    "<" + XTASK + ">" +
                    "<string>one string</string>" +
                    "<boolean>true</boolean>" +
                    "<long>123456</long>" +
                    "<double>12.35</double>" +
                    "<float>42.1</float>" +
                    "<byte>120</byte>" +
                    "<character>a</character>" +
                    "<short>12</short>" +
                    "<sqldate>" + new Date(time).toString() + "</sqldate>" +
                    "<timestamp>" + new Timestamp(time).toString() +
                    "</timestamp>" +
                    "<empty/>" +
                    "<integer>42</integer>" +
                    "</" + XTASK + ">" +
                    "<" + XTASK + ">" +
                    "<boolean>true</boolean>" +
                    "<string>one string</string>" +
                    "<long>123456</long>" +
                    "<float>42.1</float>" +
                    "<byte>120</byte>" +
                    "<sqldate>" + new Date(time).toString() + "</sqldate>" +
                    "<double>12.35</double>" +
                    "<character>a</character>" +
                    "<short>12</short>" +
                    "<timestamp>" + new Timestamp(time).toString() +
                    "</timestamp>" +
                    "<empty/>" +
                    "<integer>42</integer>" +
                    "</" + XTASK + ">" +
                    "</" + XTASKS + ">" +
                    "</" + ROOT + ">";
    try {
      document = XmlUtil.readDocument(source);
    } catch (DocumentException e) {
      e.printStackTrace();
      fail("Should not have an issue while reading document");
    }
    XmlValue[] root = XmlUtil.read(document, ruleDecls);
    XmlHash hash = new XmlHash(root);
    assertEquals(hash.size(), 4);
    assertFalse(hash.isEmpty());

    XmlValue value = hash.get(XIDRULE);
    if (value == null || (value.isEmpty()) || value.getString().isEmpty()) {
      fail("Unable to find in Rule field: " + XIDRULE);
    }
    String idrule = value.getString();
    value = hash.get(XTASKS);
    if (value != null && (!value.isEmpty()) && value.isSubXml() &&
        !value.isMultiple()) {
      XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        XmlValue item = subvalues[0];
        List<XmlValue[]> list = (List<XmlValue[]>) item.getList();
        if (list == null || list.isEmpty()) {
          fail("NoRule for " + value.getName());
        }
        for (XmlValue[] subvals : list) {
          XmlHash hashSub = new XmlHash(subvals);
          XmlValue valueItem = hashSub.get("string");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.STRING ||
              valueItem.getString().isEmpty()) {
            fail("should not");
          }
          valueItem = hashSub.get("boolean");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.BOOLEAN ||
              valueItem.getBoolean() != true) {
            fail("should not");
          }
          valueItem = hashSub.get("long");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.LONG ||
              valueItem.getLong() < 0) {
            fail("should not");
          }
          valueItem = hashSub.get("double");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.DOUBLE ||
              valueItem.getDouble() < 0.0) {
            fail("should not");
          }
          valueItem = hashSub.get("float");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.FLOAT ||
              valueItem.getFloat() < 0.0) {
            fail("should not");
          }
          valueItem = hashSub.get("byte");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.BYTE ||
              valueItem.getByte() == 0) {
            fail("should not");
          }
          valueItem = hashSub.get("character");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.CHARACTER ||
              valueItem.getCharacter() == 0) {
            fail("should not");
          }
          valueItem = hashSub.get("short");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.SHORT ||
              valueItem.getShort() < 0) {
            fail("should not");
          }
          valueItem = hashSub.get("sqldate");
          if (valueItem == null ||
              valueItem.getType() != XmlType.SQLDATE) {
            fail("should not");
          }
          valueItem = hashSub.get("timestamp");
          if (valueItem == null ||
              valueItem.getType() != XmlType.TIMESTAMP) {
            fail("should not");
          }
          valueItem = hashSub.get("empty");
          if (valueItem == null || (!valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.EMPTY ||
              !valueItem.isEmpty()) {
            fail("should not");
          }
          valueItem = hashSub.get("integer");
          if (valueItem == null || (valueItem.isEmpty()) ||
              valueItem.getType() != XmlType.INTEGER ||
              valueItem.getInteger() < 0) {
            fail("should not");
          }
          hashSub.clear();
        }
        list.clear();
      } else {
        fail("should not");
      }
    } else {
      fail("should not");
    }
    hash.clear();
    document.clearContent();
  }

  @Test
  public void testXmlRootHash() {
    XmlDecl[] decls = {
        new XmlDecl("n1", XmlType.BOOLEAN, "/root/bool", true),
        new XmlDecl("n2", XmlType.FLOAT, "/root/float", true),
        new XmlDecl("n3", XmlType.STRING, "/root/string", true),
        new XmlDecl("n4", XmlType.INTEGER, "/root/int", true),
        new XmlDecl("n5", XmlType.BOOLEAN, "/root/sub1/sub2/bool",
                    true),
        new XmlDecl("n6", XmlType.BOOLEAN, "/root/sub1/sub3/bool", true),
        new XmlDecl("n30", XmlType.LONG, "/root/long", true),
        new XmlDecl("n31", XmlType.DOUBLE, "/root/double", true),
        new XmlDecl("n32", XmlType.BYTE, "/root/byte", true),
        new XmlDecl("n33", XmlType.CHARACTER, "/root/character", true),
        new XmlDecl("n34", XmlType.EMPTY, "/root/empty", true),
        new XmlDecl("n35", XmlType.SHORT, "/root/short", true),
        new XmlDecl("n36", XmlType.SQLDATE, "/root/sqldate", true),
        new XmlDecl("n37", XmlType.TIMESTAMP, "/root/timestamp", true),
        new XmlDecl("n38", XmlType.XVAL, "/root/xval", true),
        new XmlDecl("n7", XmlType.BOOLEAN,
                    "/root/sub4/sub5/sub6/sub7/bool", true)
    };
    String xmltree =
        "<root><bool>true</bool><float>1.2</float><string>my string to read</string><int>5</int>"
        + "<sub1>"
        + "<sub2><bool>1</bool></sub2>"
        + "<sub3>"
        + "<bool>false</bool><bool>true</bool><bool>0</bool>"
        + "</sub3>"
        + "</sub1>"
        +
        "<sub4><sub5><sub6><sub7><bool>True</bool></sub7></sub6></sub5></sub4>"
        + "<bool>false</bool><float>1.3</float><string>name</string>"
        + "<int>3</int><long>12</long><long>13</long><double>1.2</double>" +
        "<double>1.2</double><byte>b</byte><character>c</character>" +
        "<empty/><empty/><short>3</short><short>5</short>"
        + "</root>";
    // Test with only single XmlType (no XVAL)
    Document document = null;
    try {
      document = XmlUtil.readDocument(xmltree);
    } catch (DocumentException e) {
      e.printStackTrace();
      return;
    }
    XmlValue[] values = XmlUtil.read(document, decls);
    XmlRootHash hash = new XmlRootHash(values);
    XmlValue value = hash.get("n1");
    assertNotNull(value);
  }

  @Test
  public void testUtil() {
    XmlDecl[] decls = {
        new XmlDecl("n1", XmlType.BOOLEAN, "/root/bool", false),
        new XmlDecl("n2", XmlType.FLOAT, "/root/float", false),
        new XmlDecl("n3", XmlType.STRING, "/root/string", false),
        new XmlDecl("n4", XmlType.INTEGER, "/root/int", false),
        new XmlDecl("n5", XmlType.BOOLEAN, "/root/sub1/sub2/bool",
                    false),
        new XmlDecl("n6", XmlType.BOOLEAN, "/root/sub1/sub3/bool", true),
        new XmlDecl("n7", XmlType.BOOLEAN,
                    "/root/sub4/sub5/sub6/sub7/bool", false)
    };
    String xmltree =
        "<root><bool>true</bool><float>1.2</float><string>my string to read</string><int>5</int>"
        + "<sub1>"
        + "<sub2><bool>1</bool></sub2>"
        + "<sub3>"
        + "<bool>false</bool><bool>true</bool><bool>0</bool>"
        + "</sub3>"
        + "</sub1>"
        +
        "<sub4><sub5><sub6><sub7><bool>True</bool></sub7></sub6></sub5></sub4>"
        + "</root>";
    // Test with only single XmlType (no XVAL)
    Document document = null;
    try {
      document = XmlUtil.readDocument(xmltree);
    } catch (DocumentException e) {
      e.printStackTrace();
      return;
    }
    try {
      XmlUtil.saveDocument("/tmp/test-src.xml", document);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    XmlValue[] values = XmlUtil.read(document, decls);
    Document newDoc = XmlUtil.createEmptyDocument();
    XmlUtil.write(newDoc, values);
    String text = XmlUtil.writeToString(newDoc);
    System.err.println(text);
    try {
      XmlUtil.saveDocument("/tmp/test.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      newDoc = XmlUtil.getDocument("/tmp/test.xml");
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    } catch (DocumentException e) {
      e.printStackTrace();
      fail("Should not");
    }
    values = XmlUtil.read(newDoc, decls);
    XmlHash hash = new XmlHash(values);
    XmlValue value = hash.get("n6");
    if (value != null) {
      if (value.isMultiple()) {
        try {
          value.addFromString("true");
        } catch (InvalidObjectException e) {
          e.printStackTrace();
          fail("Should not");
        } catch (InvalidArgumentException e) {
          e.printStackTrace();
          fail("Should not");
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
          fail("Should not");
        }
      }
    }
    newDoc = XmlUtil.createEmptyDocument();
    XmlUtil.write(newDoc, values);
    try {
      XmlUtil.saveDocument("/tmp/test2.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    Element elt = null;
    try {
      elt = XmlUtil.getElement(newDoc, "/root/sub4");
    } catch (DocumentException e1) {
      e1.printStackTrace();
      fail("Should not");
    }
    XmlValue[] oldValues = XmlUtil.read(document, decls);
    XmlUtil.write(elt, oldValues);
    try {
      XmlUtil.saveDocument("/tmp/test3.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    XmlUtil.write(newDoc, oldValues);
    try {
      XmlUtil.saveDocument("/tmp/test4.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      XmlUtil.writeXML("/tmp/test4.xml", "UTF-8", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      XmlUtil.writeXML("/tmp/test4.xml", null, newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      XmlUtil.saveElement("/tmp/test5.xml",
                          (Element) elt.selectSingleNode("root"));
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    // Now test XmlType.XVAL
    System.err.println("\n\nXVAL TESTING\n");
    XmlDecl[] subdecls1 = {
        new XmlDecl("n11", XmlType.BOOLEAN, "/n11/bool", false),
        new XmlDecl("n12", XmlType.FLOAT, "/n11/float", true)
    };
    XmlDecl[] subdecls2 = {
        new XmlDecl("n21", XmlType.BOOLEAN, "/n21/bool", false),
        new XmlDecl("n22", XmlType.FLOAT, "/n21/float", true)
    };
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
        new XmlDecl("n20", XmlType.XVAL, "/root/sub9", subdecls2, true)
    };
    xmltree =
        "<root><bool>true</bool><float>1.2</float><string>my string to read</string><int>5</int>"
        + "<sub1>"
        + "<sub2><bool>1</bool></sub2>"
        + "<sub3>"
        + "<bool>false</bool><bool>true</bool><bool>0</bool>"
        + "</sub3>"
        + "</sub1>"
        +
        "<sub4><sub5><sub6><sub7><bool>True</bool></sub7></sub6></sub5></sub4>"
        +
        "<sub8><n11><bool>true</bool><float>1.2</float><float>1.3</float><float>1.4</float></n11></sub8>"
        +
        "<sub9><n21><bool>true</bool><float>2.2</float><float>2.3</float><float>2.4</float></n21></sub9>"
        +
        "<sub9><n21><bool>false</bool><float>3.2</float><float>3.3</float><float>3.4</float></n21></sub9>"
        +
        "<sub9><n21><bool>true</bool><float>4.2</float><float>4.3</float><float>4.4</float></n21></sub9>"
        + "</root>";
    document = null;
    try {
      document = XmlUtil.readDocument(xmltree);
    } catch (DocumentException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      XmlUtil.saveDocument("/tmp/xtest-src.xml", document);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    values = XmlUtil.read(document, maindecls);
    newDoc = XmlUtil.createEmptyDocument();
    XmlUtil.write(newDoc, values);
    text = XmlUtil.writeToString(newDoc);
    System.err.println(text);
    try {
      XmlUtil.saveDocument("/tmp/xtest.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    try {
      newDoc = XmlUtil.getDocument("/tmp/xtest.xml");
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    } catch (DocumentException e) {
      e.printStackTrace();
      fail("Should not");
    }
    values = XmlUtil.read(newDoc, maindecls);
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
          fail("Should not");
        } catch (InvalidArgumentException e) {
          e.printStackTrace();
          fail("Should not");
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
          fail("Should not");
        }
      }
    }
    newDoc = XmlUtil.createEmptyDocument();
    XmlUtil.write(newDoc, values);
    try {
      XmlUtil.saveDocument("/tmp/xtest2.xml", newDoc);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
    elt = null;
    try {
      elt = XmlUtil.getElement(newDoc, "/root/sub9");
    } catch (DocumentException e1) {
      e1.printStackTrace();
      fail("Should not");
    }
    try {
      XmlUtil.saveElement("/tmp/xtest5.xml",
                          elt);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Should not");
    }
  }
}
