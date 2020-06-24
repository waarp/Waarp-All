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

package org.waarp.openr66.protocol.localhandler.packet.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class TransferRequestJsonPacketTest extends JsonPacket {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testItIsDeserialized() {
    try {
      final String json =
          "{\"requested\": \"server1\", \"@class\": \"org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket\", \"rulename\": \"send\", \"filename\": \"test.dat\"}";
      final Object res =
          new ObjectMapper().readValue(json, TransferRequestJsonPacket.class);
      assertEquals(TransferRequestJsonPacket.class, res.getClass());
    } catch (final Exception e) {
      fail("Got unexpected exception" + e);
    }
  }

  @Test
  public void testItIsDeserializedFromJsonPacket() {
    try {
      final String json =
          "{\"requested\": \"server1\", \"@class\": \"org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket\", \"rulename\": \"send\", \"filename\": \"test.dat\"}";
      final Object res = new ObjectMapper().readValue(json, JsonPacket.class);
      assertEquals(TransferRequestJsonPacket.class, res.getClass());
    } catch (final Exception e) {
      fail("Got unexpected exception" + e);
    }
  }

  @Test
  public void testSetFileInformation() {
    final TransferRequestJsonPacket packet = new TransferRequestJsonPacket();

    packet.setFileInformation("foo");
    assertEquals("The given value should have been set", "foo",
                 packet.getFileInformation());

    packet.setFileInformation((String) null);
    assertEquals("null should be set as an empty string", "",
                 packet.getFileInformation());
  }

  @Test
  public void testFileInformationShouldBeDeserialized() {
    System.out.println("in should be deserialized");
    try {
      final String json =
          "{\"fileInformation\": \"foo\", \"@class\": \"org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket\"}";
      final TransferRequestJsonPacket res =
          new ObjectMapper().readValue(json, TransferRequestJsonPacket.class);
      assertEquals("fileInformation should be deserialized", "foo",
                   res.getFileInformation());
    } catch (final Exception e) {
      fail("Got unexpected exception" + e);
    }
  }

  @Test
  public void testFileInformationShouldNotBeNullAfterDeserialization() {
    System.out.println("in should not be null");
    try {
      final String json =
          "{\"@class\": \"org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket\"}";
      final TransferRequestJsonPacket res =
          new ObjectMapper().readValue(json, TransferRequestJsonPacket.class);
      assertEquals("fileInformation should not be null", "",
                   res.getFileInformation());
    } catch (final Exception e) {
      fail("Got unexpected exception" + e);
    }
  }
}
