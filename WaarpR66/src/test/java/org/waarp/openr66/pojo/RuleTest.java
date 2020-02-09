package org.waarp.openr66.pojo;

import org.junit.Ignore;
import org.junit.Test;

import org.waarp.common.json.JsonHandler;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;

public class RuleTest {

	@Test
    @Ignore
	public void testJson() {
        List<String> hosts = Arrays.asList("sup1");
        List<RuleTask> tasks = Arrays.asList();
		Rule tested = new Rule("rulename", 3, hosts, "recv-path", "send-path",
            "arch-path", "work-path", tasks, tasks, tasks, tasks, tasks, tasks,
            UpdatedInfo.TOSUBMIT);

        String expected = "{" +
            "\"IDRULE\":\"rulename\"," +
            "\"MODETRANS\":3," +
            "\"RECVPATH\":\"recv-path\"," +
            "\"SENDPATH\":\"send-path\"," +
            "\"ARCHIVEPATH\":\"arch-path\"," +
            "\"WORKPATH\":\"work-path\"," +
            "\"UPDATEDINFO\":3," +
            "\"HOSTIDS\":\"<hostids><hostid>sup1</hostid></hostids>\"," +
            "\"RPRETASKS\":\"<tasks></tasks>\"," +
            "\"RPOSTTASKS\":\"<tasks></tasks>\"," +
            "\"SPRETASKS\":\"<tasks></tasks>\"," +
            "\"SPOSTTASKS\":\"<tasks></tasks>\"," +
            "\"RERRORTASKS\":\"<tasks></tasks>\"," +
            "\"SERRORTASKS\":\"<tasks></tasks>\"" +
            "}";

		String got = JsonHandler.writeAsString(tested);

		assertEquals(expected, got);
	}
}
