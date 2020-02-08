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

package org.waarp.openr66.database.data;

import java.sql.Timestamp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;

import static org.junit.Assert.*;

public class DbTaskRunnerTest {
    @Test
    public void testJsonSerialisation() {
        Transfer transfer = new Transfer(12345L, "myrule", 2, "myfile.dat",
                "myoriginalfile.dat", "myfileinfo", true, 42, false,
                "me", "me", "other", "my-transfer-info",
                Transfer.TASKSTEP.POSTTASK, Transfer.TASKSTEP.TRANSFERTASK, 53,
                ErrorCode.MD5Error, ErrorCode.Unimplemented, 72,
                new Timestamp(1581092031000L),
                new Timestamp(1581092131000L), UpdatedInfo.RUNNING);
        DbTaskRunner dbTransfer = new DbTaskRunner(transfer);

        String expected = "{" +
            "\"@model\":\"DbTaskRunner\"," +
            "\"SPECIALID\":12345," +
            "\"RETRIEVEMODE\":false," +
            "\"IDRULE\":\"myrule\"," +
            "\"MODETRANS\":2," +
            "\"FILENAME\":\"myfile.dat\"," +
            "\"ORIGINALNAME\":\"myoriginalfile.dat\"," +
            "\"FILEINFO\":\"myfileinfo\"," +
            "\"ISMOVED\":true," +
            "\"BLOCKSZ\":42," +
            "\"OWNERREQ\":\"me\"," +
            "\"REQUESTER\":\"me\"," +
            "\"REQUESTED\":\"other\"," +
            "\"TRANSFERINFO\":\"my-transfer-info\"," +
            "\"GLOBALSTEP\":3," +
            "\"GLOBALLASTSTEP\":2," +
            "\"STEP\":53," +
            "\"STEPSTATUS\":\"M  \"," +
            "\"INFOSTATUS\":\"U  \"," +
            "\"RANK\":72," +
            "\"STARTTRANS\":1581092031000," +
            "\"STOPTRANS\":1581092131000," +
            "\"UPDATEDINFO\":5," +
            "\"ORIGINALSIZE\":-1" +
            "}";


        ObjectNode asJson = dbTransfer.getJson();
        String got = JsonHandler.writeAsString(asJson);

        assertEquals(expected, got);
    }
}
