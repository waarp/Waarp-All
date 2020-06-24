package org.waarp.openr66.pojo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class BusinessTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testJson() {
    Business tested =
        new Business("Test", "business", "roles", "aliases", "others",
                     UpdatedInfo.UNKNOWN);

    String expected =
        "{" + "\"HOSTID\":\"Test\"," + "\"BUSINESS\":\"business\"," +
        "\"ROLES\":\"roles\"," + "\"ALIASES\":\"aliases\"," +
        "\"OTHERS\":\"others\"," + "\"UPDATEDINFO\":0" + "}";

    String got = JsonHandler.writeAsString(tested);

    assertEquals(expected, got);
  }
}
