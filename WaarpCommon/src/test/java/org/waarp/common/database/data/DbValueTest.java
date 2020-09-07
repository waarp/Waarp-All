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

package org.waarp.common.database.data;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.TestWatcherJunit4;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.Assert.*;

public class DbValueTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  @Test
  public void testDbValue() throws WaarpDatabaseSqlException, IOException {
    DbValue valueS = new DbValue("string");
    DbValue valueVl = new DbValue("string", true);
    DbValue valueI = new DbValue(0);
    DbValue valueB = new DbValue((byte) 0);
    DbValue valueT = new DbValue(true);
    DbValue valueSh = new DbValue((short) 0);
    DbValue valueL = new DbValue(0L);
    DbValue valueF = new DbValue((float) 0.1);
    DbValue valueD = new DbValue(0.1D);
    DbValue valueA = new DbValue("string".getBytes());
    DbValue valueDa = new DbValue(new Date(0));
    DbValue valueTm = new DbValue(new Timestamp(0));
    DbValue valueDaS = new DbValue(new java.sql.Date(0));
    DbValue valueClob = new DbValue(new StringReader("clob"), null);
    DbValue valueBlob =
        new DbValue(new ByteArrayInputStream("blob".getBytes()), null);

    DbValue valueS2 = new DbValue("string", "S2");
    DbValue valueVl2 = new DbValue("string", "Vl2", true);
    DbValue valueI2 = new DbValue(0, "I2");
    DbValue valueB2 = new DbValue((byte) 0, "B2");
    DbValue valueT2 = new DbValue(true, "T2");
    DbValue valueSh2 = new DbValue((short) 0, "Sh2");
    DbValue valueL2 = new DbValue(0L, "L2");
    DbValue valueF2 = new DbValue((float) 0.1, "F2");
    DbValue valueD2 = new DbValue(0.1D, "F2");
    DbValue valueA2 = new DbValue("string".getBytes(), "A2");
    DbValue valueDa2 = new DbValue(new Date(0), "Da2");
    DbValue valueTm2 = new DbValue(new Timestamp(0), "Ta2");
    DbValue valueDaS2 = new DbValue(new java.sql.Date(0), "DaS2");
    DbValue valueClob2 = new DbValue(new StringReader("clob"), "clob2");
    DbValue valueBlob2 =
        new DbValue(new ByteArrayInputStream("blob".getBytes()), "blob2");

    assertEquals(valueS.getType(), valueS2.getType());
    assertEquals(valueVl.getType(), valueVl2.getType());
    assertEquals(valueI.getType(), valueI2.getType());
    assertEquals(valueB.getType(), valueB2.getType());
    assertEquals(valueT.getType(), valueT2.getType());
    assertEquals(valueSh.getType(), valueSh2.getType());
    assertEquals(valueL.getType(), valueL2.getType());
    assertEquals(valueF.getType(), valueF2.getType());
    assertEquals(valueD.getType(), valueD2.getType());
    assertEquals(valueA.getType(), valueA2.getType());
    assertEquals(valueDa.getType(), valueDa2.getType());
    assertEquals(valueTm.getType(), valueTm2.getType());
    assertEquals(valueDaS.getType(), valueDaS2.getType());
    assertEquals(valueClob.getType(), valueClob2.getType());
    assertEquals(valueBlob.getType(), valueBlob2.getType());

    assertEquals(valueS.getValue(), valueS2.getValue());
    assertEquals(valueVl.getValue(), valueVl2.getValue());
    assertEquals(valueI.getValue(), valueI2.getValue());
    assertEquals(valueB.getValue(), valueB2.getValue());
    assertEquals(valueT.getValue(), valueT2.getValue());
    assertEquals(valueSh.getValue(), valueSh2.getValue());
    assertEquals(valueL.getValue(), valueL2.getValue());
    assertEquals(valueF.getValue(), valueF2.getValue());
    assertEquals(valueD.getValue(), valueD2.getValue());
    assertArrayEquals((byte[]) valueA.getValue(), (byte[]) valueA2.getValue());
    assertEquals(valueDa.getValue(), valueDa2.getValue());
    assertEquals(valueTm.getValue(), valueTm2.getValue());
    assertEquals(valueDaS.getValue(), valueDaS2.getValue());

    assertEquals(valueS.getValueAsString(), valueS2.getValueAsString());
    assertEquals(valueVl.getValueAsString(), valueVl2.getValueAsString());
    assertEquals(valueI.getValueAsString(), valueI2.getValueAsString());
    assertEquals(valueB.getValueAsString(), valueB2.getValueAsString());
    assertEquals(valueT.getValueAsString(), valueT2.getValueAsString());
    assertEquals(valueSh.getValueAsString(), valueSh2.getValueAsString());
    assertEquals(valueL.getValueAsString(), valueL2.getValueAsString());
    assertEquals(valueF.getValueAsString(), valueF2.getValueAsString());
    assertEquals(valueD.getValueAsString(), valueD2.getValueAsString());
    assertEquals(valueA.getValueAsString(), valueA2.getValueAsString());
    assertEquals(valueDa.getValueAsString(), valueDa2.getValueAsString());
    assertEquals(valueTm.getValueAsString(), valueTm2.getValueAsString());
    assertEquals(valueDaS.getValueAsString(), valueDaS2.getValueAsString());
    assertEquals(valueClob.getValueAsString(), valueClob2.getValueAsString());
    assertEquals(valueBlob.getValueAsString(), valueBlob2.getValueAsString());


    valueS.setValue(valueS2.getValue());
    valueS.setValue((String) valueS2.getValue());
    valueVl.setValue((String) valueVl2.getValue());
    valueI.setValue((Integer) valueI2.getValue());
    valueB.setValue((Byte) valueB2.getValue());
    valueT.setValue((Boolean) valueT2.getValue());
    valueSh.setValue((Short) valueSh2.getValue());
    valueL.setValue((Long) valueL2.getValue());
    valueF.setValue((Float) valueF2.getValue());
    valueD.setValue((Double) valueD2.getValue());
    valueA.setValue((byte[]) valueA2.getValue());
    valueDa.setValue((Date) valueDa2.getValue());
    valueTm.setValue((Timestamp) valueTm2.getValue());
    valueDaS.setValue((java.sql.Date) valueDaS2.getValue());
    valueClob.setValue((Reader) valueClob2.getValue());
    valueBlob.setValue((InputStream) valueBlob2.getValue());

    assertEquals(valueS.getValue(), valueS2.getValue());
    assertEquals(valueVl.getValue(), valueVl2.getValue());
    assertEquals(valueI.getValue(), valueI2.getValue());
    assertEquals(valueB.getValue(), valueB2.getValue());
    assertEquals(valueT.getValue(), valueT2.getValue());
    assertEquals(valueSh.getValue(), valueSh2.getValue());
    assertEquals(valueL.getValue(), valueL2.getValue());
    assertEquals(valueF.getValue(), valueF2.getValue());
    assertEquals(valueD.getValue(), valueD2.getValue());
    assertEquals(valueA.getValue(), valueA2.getValue());
    assertEquals(valueDa.getValue(), valueDa2.getValue());
    assertEquals(valueTm.getValue(), valueTm2.getValue());
    assertEquals(valueDaS.getValue(), valueDaS2.getValue());
    assertEquals(valueClob.getValue(), valueClob2.getValue());
    assertEquals(valueBlob.getValue(), valueBlob2.getValue());

    valueS.setValueFromString(valueS2.getValueAsString());
    valueVl.setValueFromString(valueVl2.getValueAsString());
    valueI.setValueFromString(valueI2.getValueAsString());
    valueB.setValueFromString(valueB2.getValueAsString());
    valueT.setValueFromString(valueT2.getValueAsString());
    valueSh.setValueFromString(valueSh2.getValueAsString());
    valueL.setValueFromString(valueL2.getValueAsString());
    valueF.setValueFromString(valueF2.getValueAsString());
    valueD.setValueFromString(valueD2.getValueAsString());
    valueA.setValueFromString(valueA2.getValueAsString());
    valueDa.setValueFromString(valueDa2.getValueAsString());
    valueTm.setValueFromString(valueTm2.getValueAsString());
    valueDaS.setValueFromString(valueDaS2.getValueAsString());
    File file = new File("/tmp/clob.txt");
    FileOutputStream outputStream = new FileOutputStream(file);
    outputStream.write("content".getBytes());
    outputStream.flush();
    outputStream.close();
    valueClob.setValueFromString(file.getAbsolutePath());
    valueBlob.setValueFromString(file.getAbsolutePath());

    assertEquals(valueS.getValue(), valueS2.getValue());
    assertEquals(valueVl.getValue(), valueVl2.getValue());
    assertEquals(valueI.getValue(), valueI2.getValue());
    assertEquals(valueB.getValue(), valueB2.getValue());
    assertEquals(valueT.getValue(), valueT2.getValue());
    assertEquals(valueSh.getValue(), valueSh2.getValue());
    assertEquals(valueL.getValue(), valueL2.getValue());
    assertEquals(valueF.getValue(), valueF2.getValue());
    assertEquals(valueD.getValue(), valueD2.getValue());
    assertArrayEquals((byte[]) valueA.getValue(), (byte[]) valueA2.getValue());
    // Zone issue
    //assertEquals(valueDa.getValue(), valueDa2.getValue());
    //assertEquals(valueTm.getValue(), valueTm2.getValue());
    //assertEquals(valueDaS.getValue(), valueDaS2.getValue());

    assertNull(valueS.getColumn());
    assertNull(valueVl.getColumn());
    assertNull(valueI.getColumn());
    assertNull(valueB.getColumn());
    assertNull(valueT.getColumn());
    assertNull(valueSh.getColumn());
    assertNull(valueL.getColumn());
    assertNull(valueF.getColumn());
    assertNull(valueD.getColumn());
    assertNull(valueA.getColumn());
    assertNull(valueDa.getColumn());
    assertNull(valueTm.getColumn());
    assertNull(valueDaS.getColumn());

    assertNotNull(valueD2.getColumn());

  }
}
