package org.waarp.openr66.pojo;

import org.junit.Test;

import org.waarp.common.json.JsonHandler;

import static org.junit.Assert.*;

public class BusinessTest {

	@Test
	public void testJson() {
		Business tested = new Business("Test", "business", "roles", "aliases", "others", UpdatedInfo.UNKNOWN);

        String expected = "{" +
            "\"HOSTID\":\"Test\"," +
            "\"BUSINESS\":\"business\"," +
            "\"ROLES\":\"roles\"," +
            "\"ALIASES\":\"aliases\"," +
            "\"OTHERS\":\"others\"," +
            "\"UPDATEDINFO\":0" +
            "}";

		String got = JsonHandler.writeAsString(tested);
		
		assertEquals(expected, got);
	}
}
