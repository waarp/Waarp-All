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

package org.waarp.openr66.client.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.client.utils.OutputFormat.OUTPUTFORMAT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OutputFormatTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  @Test
  public void testJson() {
    final String[] args = {
        "FakeArg1", "FakeArg2"
    };
    OutputFormat outputFormat = new OutputFormat("FakeCommand", args);
    OutputFormat.setDefaultOutput(OUTPUTFORMAT.JSON);
    assertTrue(OutputFormat.isJson());
    assertFalse(outputFormat.getFormat() == OUTPUTFORMAT.JSON);
    outputFormat.setFormat(OUTPUTFORMAT.JSON);
    assertTrue(outputFormat.getFormat() == OUTPUTFORMAT.JSON);
    outputFormat.setValue("INT", 12345);
    outputFormat.setValue("LONG", 1234567890123456789L);
    outputFormat.setValue("BOOLEAN", true);
    outputFormat.setValue("DOUBLE", 123.45);
    outputFormat.setValue("STRING", "SimpleString");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("KEY1", "VALUE1");
    map.put("KEYOBJ2", new Date(0));
    outputFormat.setValue(map);
    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("KEY3", "VALUE3");
    outputFormat.setValueString(mapString);
    assertTrue(
        outputFormat.exist("INT", "LONG", "BOOLEAN", "DOUBLE", "STRING", "KEY1",
                           "KEYOBJ2", "KEY3"));
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\"}",
                 outputFormat.loggerOut());
    assertEquals("{\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\"}", outputFormat.toString());

    outputFormat.setValue("WITHCRLF", "Test\nTest2\r");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\"}", outputFormat.loggerOut());
    assertEquals("{\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\"}", outputFormat.toString());

    outputFormat.setValue("WITHPOINTCOMMA", "Test;Test2");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"}",
                 outputFormat.loggerOut());
    assertEquals("{\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"}",
                 outputFormat.toString());

    outputFormat.setValue("WITHXML", "<Test3>Test4&</Test3>");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"}",
                 outputFormat.loggerOut());
    assertEquals("{\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"}",
                 outputFormat.toString());

    outputFormat.setValue("WITJSON", "{\"Test5\":\"value5\",'Test6':true}");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"," +
                 "\"WITJSON\":\"{\\\"Test5\\\":\\\"value5\\\",'Test6':true}\"}",
                 outputFormat.loggerOut());
    assertEquals("{\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"," +
                 "\"WITJSON\":\"{\\\"Test5\\\":\\\"value5\\\"," +
                 "'Test6':true}\"}", outputFormat.toString());
    SysErrLogger.FAKE_LOGGER.sysout("Exemple:\n" + outputFormat.toString());
  }


  @Test
  public void testProperty() {
    final String[] args = {
        "FakeArg1", "FakeArg2"
    };
    OutputFormat outputFormat = new OutputFormat("FakeCommand", args);
    OutputFormat.setDefaultOutput(OUTPUTFORMAT.PROPERTY);
    assertTrue(OutputFormat.isProperty());
    assertFalse(outputFormat.getFormat() == OUTPUTFORMAT.PROPERTY);
    outputFormat.setFormat(OUTPUTFORMAT.PROPERTY);
    assertTrue(outputFormat.getFormat() == OUTPUTFORMAT.PROPERTY);
    outputFormat.setValue("INT", 12345);
    outputFormat.setValue("LONG", 1234567890123456789L);
    outputFormat.setValue("BOOLEAN", true);
    outputFormat.setValue("DOUBLE", 123.45);
    outputFormat.setValue("STRING", "SimpleString");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("KEY1", "VALUE1");
    map.put("KEYOBJ2", new Date(0));
    outputFormat.setValue(map);
    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("KEY3", "VALUE3");
    outputFormat.setValueString(mapString);
    assertTrue(
        outputFormat.exist("INT", "LONG", "BOOLEAN", "DOUBLE", "STRING", "KEY1",
                           "KEYOBJ2", "KEY3"));
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\"}",
                 outputFormat.loggerOut());
    assertEquals(
        "command=FakeCommand\n" + "args=FakeArg1 FakeArg2 \n" + "INT=12345\n" +
        "LONG=1234567890123456789\n" + "BOOLEAN=true\n" + "DOUBLE=123.45\n" +
        "STRING=SimpleString\n" + "KEY1=VALUE1\n" + "KEYOBJ2=0\n" +
        "KEY3=VALUE3", outputFormat.toString());

    outputFormat.setValue("WITHCRLF", "Test\nTest2\r");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\"}", outputFormat.loggerOut());
    assertEquals(
        "command=FakeCommand\n" + "args=FakeArg1 FakeArg2 \n" + "INT=12345\n" +
        "LONG=1234567890123456789\n" + "BOOLEAN=true\n" + "DOUBLE=123.45\n" +
        "STRING=SimpleString\n" + "KEY1=VALUE1\n" + "KEYOBJ2=0\n" +
        "KEY3=VALUE3\n" + "WITHCRLF=Test Test2 ", outputFormat.toString());

    outputFormat.setValue("WITHPOINTCOMMA", "Test;Test2");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"}",
                 outputFormat.loggerOut());
    assertEquals(
        "command=FakeCommand\n" + "args=FakeArg1 FakeArg2 \n" + "INT=12345\n" +
        "LONG=1234567890123456789\n" + "BOOLEAN=true\n" + "DOUBLE=123.45\n" +
        "STRING=SimpleString\n" + "KEY1=VALUE1\n" + "KEYOBJ2=0\n" +
        "KEY3=VALUE3\n" + "WITHCRLF=Test Test2 \n" +
        "WITHPOINTCOMMA=Test;Test2", outputFormat.toString());

    outputFormat.setValue("WITHXML", "<Test3>Test4&</Test3>");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"}",
                 outputFormat.loggerOut());
    assertEquals(
        "command=FakeCommand\n" + "args=FakeArg1 FakeArg2 \n" + "INT=12345\n" +
        "LONG=1234567890123456789\n" + "BOOLEAN=true\n" + "DOUBLE=123.45\n" +
        "STRING=SimpleString\n" + "KEY1=VALUE1\n" + "KEYOBJ2=0\n" +
        "KEY3=VALUE3\n" + "WITHCRLF=Test Test2 \n" +
        "WITHPOINTCOMMA=Test;Test2\n" + "WITHXML=<Test3>Test4&</Test3>",
        outputFormat.toString());

    outputFormat.setValue("WITJSON", "{\"Test5\":\"value5\",'Test6':true}");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"," +
                 "\"WITJSON\":\"{\\\"Test5\\\":\\\"value5\\\",'Test6':true}\"}",
                 outputFormat.loggerOut());
    assertEquals(
        "command=FakeCommand\n" + "args=FakeArg1 FakeArg2 \n" + "INT=12345\n" +
        "LONG=1234567890123456789\n" + "BOOLEAN=true\n" + "DOUBLE=123.45\n" +
        "STRING=SimpleString\n" + "KEY1=VALUE1\n" + "KEYOBJ2=0\n" +
        "KEY3=VALUE3\n" + "WITHCRLF=Test Test2 \n" +
        "WITHPOINTCOMMA=Test;Test2\n" + "WITHXML=<Test3>Test4&</Test3>\n" +
        "WITJSON={\"Test5\":\"value5\",'Test6':true}", outputFormat.toString());
    SysErrLogger.FAKE_LOGGER.sysout("Exemple:\n" + outputFormat.toString());
  }


  @Test
  public void testXml() {
    final String[] args = {
        "FakeArg1", "FakeArg2"
    };
    OutputFormat outputFormat = new OutputFormat("FakeCommand", args);
    OutputFormat.setDefaultOutput(OUTPUTFORMAT.XML);
    assertTrue(OutputFormat.isXml());
    assertFalse(outputFormat.getFormat() == OUTPUTFORMAT.XML);
    outputFormat.setFormat(OUTPUTFORMAT.XML);
    assertTrue(outputFormat.getFormat() == OUTPUTFORMAT.XML);
    outputFormat.setValue("INT", 12345);
    outputFormat.setValue("LONG", 1234567890123456789L);
    outputFormat.setValue("BOOLEAN", true);
    outputFormat.setValue("DOUBLE", 123.45);
    outputFormat.setValue("STRING", "SimpleString");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("KEY1", "VALUE1");
    map.put("KEYOBJ2", new Date(0));
    outputFormat.setValue(map);
    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("KEY3", "VALUE3");
    outputFormat.setValueString(mapString);
    assertTrue(
        outputFormat.exist("INT", "LONG", "BOOLEAN", "DOUBLE", "STRING", "KEY1",
                           "KEYOBJ2", "KEY3"));
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\"}",
                 outputFormat.loggerOut());
    assertEquals("<xml><command>FakeCommand</command><args>FakeArg1 FakeArg2 " +
                 "</args><INT>12345</INT><LONG>1234567890123456789</LONG" +
                 "><BOOLEAN>true</BOOLEAN><DOUBLE>123" +
                 ".45</DOUBLE><STRING>SimpleString</STRING><KEY1>VALUE1</KEY1" +
                 "><KEYOBJ2>0</KEYOBJ2><KEY3>VALUE3</KEY3></xml>",
                 outputFormat.toString());

    outputFormat.setValue("WITHCRLF", "Test\nTest2\r");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\"}", outputFormat.loggerOut());
    assertEquals("<xml><command>FakeCommand</command><args>FakeArg1 FakeArg2 " +
                 "</args><INT>12345</INT><LONG>1234567890123456789</LONG" +
                 "><BOOLEAN>true</BOOLEAN><DOUBLE>123" +
                 ".45</DOUBLE><STRING>SimpleString</STRING><KEY1>VALUE1</KEY1" +
                 "><KEYOBJ2>0</KEYOBJ2><KEY3>VALUE3</KEY3><WITHCRLF>Test\n" +
                 "Test2\r" + "</WITHCRLF></xml>", outputFormat.toString());

    outputFormat.setValue("WITHPOINTCOMMA", "Test;Test2");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"}",
                 outputFormat.loggerOut());
    assertEquals("<xml><command>FakeCommand</command><args>FakeArg1 FakeArg2 " +
                 "</args><INT>12345</INT><LONG>1234567890123456789</LONG" +
                 "><BOOLEAN>true</BOOLEAN><DOUBLE>123" +
                 ".45</DOUBLE><STRING>SimpleString</STRING><KEY1>VALUE1</KEY1" +
                 "><KEYOBJ2>0</KEYOBJ2><KEY3>VALUE3</KEY3><WITHCRLF>Test\n" +
                 "Test2\r" + "</WITHCRLF><WITHPOINTCOMMA>Test;" +
                 "Test2</WITHPOINTCOMMA></xml>", outputFormat.toString());

    outputFormat.setValue("WITHXML", "<Test3>Test4&</Test3>");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"}",
                 outputFormat.loggerOut());
    assertEquals("<xml><command>FakeCommand</command><args>FakeArg1 FakeArg2 " +
                 "</args><INT>12345</INT><LONG>1234567890123456789</LONG" +
                 "><BOOLEAN>true</BOOLEAN><DOUBLE>123" +
                 ".45</DOUBLE><STRING>SimpleString</STRING><KEY1>VALUE1</KEY1" +
                 "><KEYOBJ2>0</KEYOBJ2><KEY3>VALUE3</KEY3><WITHCRLF>Test\n" +
                 "Test2\r" + "</WITHCRLF><WITHPOINTCOMMA>Test;" +
                 "Test2</WITHPOINTCOMMA><WITHXML>[Test3]Test4:[/Test3" +
                 "]</WITHXML></xml>", outputFormat.toString());

    outputFormat.setValue("WITJSON", "{\"Test5\":\"value5\",'Test6':true}");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"," +
                 "\"WITJSON\":\"{\\\"Test5\\\":\\\"value5\\\",'Test6':true}\"}",
                 outputFormat.loggerOut());
    assertEquals("<xml><command>FakeCommand</command><args>FakeArg1 FakeArg2 " +
                 "</args><INT>12345</INT><LONG>1234567890123456789</LONG" +
                 "><BOOLEAN>true</BOOLEAN><DOUBLE>123" +
                 ".45</DOUBLE><STRING>SimpleString</STRING><KEY1>VALUE1</KEY1" +
                 "><KEYOBJ2>0</KEYOBJ2><KEY3>VALUE3</KEY3><WITHCRLF>Test\n" +
                 "Test2\r" + "</WITHCRLF><WITHPOINTCOMMA>Test;" +
                 "Test2</WITHPOINTCOMMA><WITHXML>[Test3]Test4:[/Test3" +
                 "]</WITHXML><WITJSON>{\"Test5\":\"value5\"," +
                 "'Test6':true}</WITJSON></xml>", outputFormat.toString());
    SysErrLogger.FAKE_LOGGER.sysout("Exemple:\n" + outputFormat.toString());
  }


  @Test
  public void testCsv() {
    final String[] args = {
        "FakeArg1", "FakeArg2"
    };
    OutputFormat outputFormat = new OutputFormat("FakeCommand", args);
    OutputFormat.setDefaultOutput(OUTPUTFORMAT.CSV);
    assertTrue(OutputFormat.isCsv());
    assertFalse(outputFormat.getFormat() == OUTPUTFORMAT.CSV);
    outputFormat.setFormat(OUTPUTFORMAT.CSV);
    assertTrue(outputFormat.getFormat() == OUTPUTFORMAT.CSV);
    outputFormat.setValue("INT", 12345);
    outputFormat.setValue("LONG", 1234567890123456789L);
    outputFormat.setValue("BOOLEAN", true);
    outputFormat.setValue("DOUBLE", 123.45);
    outputFormat.setValue("STRING", "SimpleString");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("KEY1", "VALUE1");
    map.put("KEYOBJ2", new Date(0));
    outputFormat.setValue(map);
    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("KEY3", "VALUE3");
    outputFormat.setValueString(mapString);
    assertTrue(
        outputFormat.exist("INT", "LONG", "BOOLEAN", "DOUBLE", "STRING", "KEY1",
                           "KEYOBJ2", "KEY3"));
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\"}",
                 outputFormat.loggerOut());
    assertEquals(
        "command;args;INT;LONG;BOOLEAN;DOUBLE;STRING;KEY1;KEYOBJ2;" + "KEY3\n" +
        "FakeCommand;FakeArg1 FakeArg2 ;12345;1234567890123456789;" +
        "true;123.45;SimpleString;VALUE1;0;VALUE3", outputFormat.toString());

    outputFormat.setValue("WITHCRLF", "Test\nTest2\r");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\"}", outputFormat.loggerOut());
    assertEquals("command;args;INT;LONG;BOOLEAN;DOUBLE;STRING;KEY1;KEYOBJ2;" +
                 "KEY3;WITHCRLF\n" +
                 "FakeCommand;FakeArg1 FakeArg2 ;12345;1234567890123456789;" +
                 "true;123.45;SimpleString;VALUE1;0;VALUE3;Test Test2 ",
                 outputFormat.toString());

    outputFormat.setValue("WITHPOINTCOMMA", "Test;Test2");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"}",
                 outputFormat.loggerOut());
    assertEquals("command;args;INT;LONG;BOOLEAN;DOUBLE;STRING;KEY1;KEYOBJ2;" +
                 "KEY3;WITHCRLF;WITHPOINTCOMMA\n" +
                 "FakeCommand;FakeArg1 FakeArg2 ;12345;1234567890123456789;" +
                 "true;123.45;SimpleString;VALUE1;0;VALUE3;Test Test2 ;" +
                 "\"Test;Test2\"", outputFormat.toString());

    outputFormat.setValue("WITHXML", "<Test3>Test4&</Test3>");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"}",
                 outputFormat.loggerOut());
    assertEquals("command;args;INT;LONG;BOOLEAN;DOUBLE;STRING;KEY1;KEYOBJ2;" +
                 "KEY3;WITHCRLF;WITHPOINTCOMMA;WITHXML\n" +
                 "FakeCommand;FakeArg1 FakeArg2 ;12345;1234567890123456789;" +
                 "true;123.45;SimpleString;VALUE1;0;VALUE3;Test Test2 ;" +
                 "\"Test;Test2\";<Test3>Test4&</Test3>",
                 outputFormat.toString());

    outputFormat.setValue("WITJSON", "{\"Test5\":\"value5\",'Test6':true}");
    assertEquals("[FakeCommand]  => {\"command\":\"FakeCommand\"," +
                 "\"args\":\"FakeArg1 FakeArg2 \",\"INT\":12345," +
                 "\"LONG\":1234567890123456789,\"BOOLEAN\":true,\"DOUBLE\":123.45," +
                 "\"STRING\":\"SimpleString\",\"KEY1\":\"VALUE1\"," +
                 "\"KEYOBJ2\":0,\"KEY3\":\"VALUE3\",\"WITHCRLF\":\"Test\\n" +
                 "Test2\\r\",\"WITHPOINTCOMMA\":\"Test;Test2\"," +
                 "\"WITHXML\":\"<Test3>Test4&</Test3>\"," +
                 "\"WITJSON\":\"{\\\"Test5\\\":\\\"value5\\\",'Test6':true}\"}",
                 outputFormat.loggerOut());
    assertEquals("command;args;INT;LONG;BOOLEAN;DOUBLE;STRING;KEY1;KEYOBJ2;" +
                 "KEY3;WITHCRLF;WITHPOINTCOMMA;WITHXML;WITJSON\n" +
                 "FakeCommand;FakeArg1 FakeArg2 ;12345;1234567890123456789;" +
                 "true;123.45;SimpleString;VALUE1;0;VALUE3;Test Test2 ;" +
                 "\"Test;Test2\";<Test3>Test4&</Test3>;{\"Test5\":\"value5\"," +
                 "'Test6':true}", outputFormat.toString());
    SysErrLogger.FAKE_LOGGER.sysout("Exemple:\n" + outputFormat.toString());
  }
}
