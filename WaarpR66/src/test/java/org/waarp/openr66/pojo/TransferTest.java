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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.database.data.DbTaskRunner;

import java.sql.Timestamp;
import java.util.Map;

import static org.junit.Assert.*;

public class TransferTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TransferTest.class);

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


  @Test
  public void testMapCapture() {
    String testNoMap = "test1 test 2";
    Map<String, Object> map = DbTaskRunner.getMapFromString(testNoMap);
    String nomap = DbTaskRunner.getOutOfMapFromString(testNoMap);
    assertEquals(testNoMap, nomap);
    assertEquals(0, map.size());
    logger.warn("{} = {} + {}", testNoMap, map, nomap);

    String testSingleMap = "{}";
    map = DbTaskRunner.getMapFromString(testSingleMap);
    nomap = DbTaskRunner.getOutOfMapFromString(testSingleMap);
    assertEquals("", nomap);
    assertEquals(0, map.size());
    logger.warn("{} = {} + {}", testSingleMap, map, nomap);

    String testSingleMapKey = "{'key': 'value'}";
    map = DbTaskRunner.getMapFromString(testSingleMapKey);
    nomap = DbTaskRunner.getOutOfMapFromString(testSingleMapKey);
    assertEquals("", nomap);
    assertEquals(1, map.size());
    logger.warn("{} = {} + {}", testSingleMapKey, map, nomap);

    String testMultipleMap = "{} {}";
    map = DbTaskRunner.getMapFromString(testMultipleMap);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMap);
    assertEquals(" ", nomap);
    assertEquals(0, map.size());
    logger.warn("{} = {} + {}", testMultipleMap, map, nomap);

    String testMultipleMapKey = "{'key': 'value'} {}";
    map = DbTaskRunner.getMapFromString(testMultipleMapKey);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMapKey);
    assertEquals(" ", nomap);
    assertEquals(1, map.size());
    logger.warn("{} = {} + {}", testMultipleMapKey, map, nomap);

    String testMultipleMapKeyReverse = "{} {'key': 'value'} ";
    map = DbTaskRunner.getMapFromString(testMultipleMapKeyReverse);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMapKeyReverse);
    assertEquals("  ", nomap);
    assertEquals(1, map.size());
    logger.warn("{} = {} + {}", testMultipleMapKeyReverse, map, nomap);

    String testMultipleMapKeys =
        "{} {'key': 'value'}{'key2': 'value'}{'key3': 'value'}";
    map = DbTaskRunner.getMapFromString(testMultipleMapKeys);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMapKeys);
    assertEquals(" ", nomap);
    assertEquals(3, map.size());
    logger.warn("{} = {} + {}", testMultipleMapKeys, map, nomap);

    String testMultipleMap2Key = "{'key': 'value'} {'key2': 'value2'}";
    map = DbTaskRunner.getMapFromString(testMultipleMap2Key);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMap2Key);
    assertEquals(" ", nomap);
    assertEquals(2, map.size());
    logger.warn("{} = {} + {}", testMultipleMap2Key, map, nomap);

    String testMultipleMap2SameKey = "{'key': 'value'} {'key': 'value2'}";
    map = DbTaskRunner.getMapFromString(testMultipleMap2SameKey);
    nomap = DbTaskRunner.getOutOfMapFromString(testMultipleMap2SameKey);
    assertEquals(" ", nomap);
    assertEquals(1, map.size());
    logger.warn("{} = {} + {}", testMultipleMap2SameKey, map, nomap);

    String testMultipleMap2SameKeyAndNotMap =
        "extra information {'key': 'value'} other" +
        "{'key2': 'value2'}and another one";
    map = DbTaskRunner.getMapFromString(testMultipleMap2SameKeyAndNotMap);
    nomap =
        DbTaskRunner.getOutOfMapFromString(testMultipleMap2SameKeyAndNotMap);
    assertEquals("extra information  other" + "and another one", nomap);
    assertEquals(2, map.size());
    logger.warn("{} = {} + {}", testMultipleMap2SameKeyAndNotMap, map, nomap);


    String testMultipleMap2SameKeyAndNotMapEnding =
        "extra information {'key': 'value'} other" + "{'key2': 'value2'}";
    map = DbTaskRunner.getMapFromString(testMultipleMap2SameKeyAndNotMapEnding);
    nomap = DbTaskRunner
        .getOutOfMapFromString(testMultipleMap2SameKeyAndNotMapEnding);
    assertEquals("extra information  other", nomap);
    assertEquals(2, map.size());
    logger.warn("{} = {} + {}", testMultipleMap2SameKeyAndNotMapEnding, map,
                nomap);

    String testMultipleMap2SameKeyAndTrueNotMapEnding =
        "extra information {'key': 'value'} other" + " {'key2': 'value2'";
    map = DbTaskRunner
        .getMapFromString(testMultipleMap2SameKeyAndTrueNotMapEnding);
    nomap = DbTaskRunner
        .getOutOfMapFromString(testMultipleMap2SameKeyAndTrueNotMapEnding);
    assertEquals("extra information  other {'key2': 'value2'", nomap);
    assertEquals(1, map.size());
    logger.warn("{} = {} + {}", testMultipleMap2SameKeyAndTrueNotMapEnding, map,
                nomap);
  }

}
