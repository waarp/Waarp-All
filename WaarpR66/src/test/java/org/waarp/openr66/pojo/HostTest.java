package org.waarp.openr66.pojo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class HostTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testJson() throws WaarpDatabaseSqlException {
    byte[] rawbytes = { 0xA, 0x2 };
    Host host =
        new Host("hostid", "127.0.0.1", 6666, rawbytes, false, true, true, true,
                 true, UpdatedInfo.UNKNOWN);

    String expected =
        "{" + "\"HOSTID\":\"hostid\"," + "\"ADDRESS\":\"127.0.0.1\"," +
        "\"PORT\":6666," + "\"HOSTKEY\":\"CgI=\"," + "\"ISSSL\":false," +
        "\"ISCLIENT\":true," + "\"ISPROXIFIED\":true," + "\"ADMINROLE\":true," +
        "\"ISACTIVE\":true," + "\"UPDATEDINFO\":0" + "}";

    String got = JsonHandler.writeAsString(host);

    assertEquals(expected, got);
  }

  @Test
  public void testNegativePort() throws WaarpDatabaseSqlException {
    byte[] rawbytes = { 0xA, 0x2 };
    Host host =
        new Host("hostid", "127.0.0.1", -42, rawbytes, false, true, true, true,
                 true, UpdatedInfo.UNKNOWN);

    assertEquals(-42, host.getPort());
  }
}
