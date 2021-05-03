package org.waarp.openr66.pojo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class LimitTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testJson() throws WaarpDatabaseSqlException {
    Limit limit = new Limit("hostid", 10, 20, 30, 40, 50, UpdatedInfo.TOSUBMIT);

    String expected =
        "{" + "\"HOSTID\":\"hostid\"," + "\"READGLOBALLIMIT\":20," +
        "\"WRITEGLOBALLIMIT\":30," + "\"READSESSIONLIMIT\":40," +
        "\"WRITESESSIONLIMIT\":50," + "\"DELAYLIMIT\":10," +
        "\"UPDATEDINFO\":3" + "}";

    String got = JsonHandler.writeAsString(limit);

    assertEquals(expected, got);
  }
}
