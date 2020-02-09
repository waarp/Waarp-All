package org.waarp.openr66.pojo;

import org.junit.Test;

import org.waarp.common.json.JsonHandler;

import static org.junit.Assert.*;

public class LimitTest {

	@Test
	public void testJson() {
		Limit limit = new Limit("hostid",10, 20, 30, 40, 50, UpdatedInfo.TOSUBMIT);

        String expected = "{" +
            "\"HOSTID\":\"hostid\"," +
            "\"READGLOBALLIMIT\":20," +
            "\"WRITEGLOBALLIMIT\":30," +
            "\"READSESSIONLIMIT\":40," +
            "\"WRITESESSIONLIMIT\":50," +
            "\"DELAYLIMIT\":10," +
            "\"UPDATEDINFO\":3" +
            "}";

		String got = JsonHandler.writeAsString(limit);

		assertEquals(expected, got);
	}
}
