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

package org.waarp.openr66.pojo;

import org.junit.Test;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.context.ErrorCode;

import java.sql.Timestamp;

import static org.junit.Assert.*;

public class TransferTest {
  @Test
  public void testJsonSerialisation() {
    Transfer transfer =
        new Transfer(12345, "myrule", 2, "myfile.dat", "myoriginalfile.dat",
                     "myfileinfo", true, 42, false, "me", "me", "other",
                     "my-transfer-info", Transfer.TASKSTEP.POSTTASK,
                     Transfer.TASKSTEP.TRANSFERTASK, 53, ErrorCode.MD5Error,
                     ErrorCode.Unimplemented, 72, new Timestamp(1581092031000L),
                     new Timestamp(1581092131000L), UpdatedInfo.RUNNING);

    //String expected = "{" +
    //    "\"@model\":\"DbTaskRunner\"," +
    //    "\"BLOCKSZ\":42," +
    //    "\"FILEINFO\":\"myfileinfo\"," +
    //    "\"FILENAME\":\"myfile.dat\"," +
    //    "\"GLOBALLASTSTEP\":2," +
    //    "\"GLOBALSTEP\":3," +
    //    "\"IDRULE\":\"myrule\"," +
    //    "\"INFOSTATUS\":\"U  \"," +
    //    "\"ISMOVED\":true," +
    //    "\"MODETRANS\":2," +
    //    "\"ORIGINALNAME\":\"myoriginalfile.dat\"," +
    //    "\"ORIGINALSIZE\":-1," +
    //    "\"OWNERREQ\":\"me\"," +
    //    "\"RANK\":72," +
    //    "\"REQUESTED\":\"other\"," +
    //    "\"REQUESTER\":\"me\"," +
    //    "\"RETRIEVEMODE\":false," +
    //    "\"SPECIALID\":12345," +
    //    "\"STARTTRANS\":1581092031000," +
    //    "\"STEP\":53," +
    //    "\"STEPSTATUS\":\"M  \"," +
    //    "\"STOPTRANS\":1581092131000," +
    //    "\"TRANSFERINFO\":\"my-transfer-info\"," +
    //    "\"UPDATEDINFO\":5" +
    //    "}";

    String expected = "{" + "\"SPECIALID\":12345," + "\"RETRIEVEMODE\":false," +
                      "\"IDRULE\":\"myrule\"," + "\"MODETRANS\":2," +
                      "\"FILENAME\":\"myfile.dat\"," +
                      "\"ORIGINALNAME\":\"myoriginalfile.dat\"," +
                      "\"FILEINFO\":\"myfileinfo\"," + "\"ISMOVED\":true," +
                      "\"BLOCKSZ\":42," + "\"OWNERREQ\":\"me\"," +
                      "\"REQUESTER\":\"me\"," + "\"REQUESTED\":\"other\"," +
                      "\"TRANSFERINFO\":\"my-transfer-info\"," +
                      "\"GLOBALSTEP\":3," + "\"GLOBALLASTSTEP\":2," +
                      "\"STEP\":53," + "\"STEPSTATUS\":\"M  \"," +
                      "\"INFOSTATUS\":\"U  \"," + "\"RANK\":72," +
                      "\"STARTTRANS\":1581092031000," +
                      "\"STOPTRANS\":1581092131000," + "\"UPDATEDINFO\":5" +
                      "}";


    String got = JsonHandler.writeAsString(transfer);

    assertEquals(expected, got);
  }
}
