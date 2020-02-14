package org.waarp.openr66.pojo;

import org.junit.Test;

import org.waarp.common.json.JsonHandler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RuleTest {

	@Test
	public void testJson() {
        List<String> hosts = Arrays.asList("sup1");

        List<RuleTask> spretasks = new ArrayList<RuleTask>();
        spretasks.add(new RuleTask("LOG", "spretasks", 0));

        List<RuleTask> sposttasks = new ArrayList<RuleTask>();
        sposttasks.add(new RuleTask("LOG", "sposttasks", 0));

        List<RuleTask> serrortasks = new ArrayList<RuleTask>();
        serrortasks.add(new RuleTask("LOG", "serrortasks", 0));

        List<RuleTask> rpretasks = new ArrayList<RuleTask>();
        rpretasks.add(new RuleTask("LOG", "rpretasks", 0));

        List<RuleTask> rposttasks = new ArrayList<RuleTask>();
        rposttasks.add(new RuleTask("LOG", "rposttasks", 0));

        List<RuleTask> rerrortasks = new ArrayList<RuleTask>();
        rerrortasks.add(new RuleTask("LOG", "rerrortasks", 0));

		Rule tested = new Rule("rulename", 3, hosts, "recv-path", "send-path",
            "arch-path", "work-path", rpretasks, rposttasks, rerrortasks,
            spretasks, sposttasks, serrortasks, UpdatedInfo.TOSUBMIT);

        String expected = "{" +
            "\"IDRULE\":\"rulename\"," +
            "\"MODETRANS\":3," +
            "\"RECVPATH\":\"recv-path\"," +
            "\"SENDPATH\":\"send-path\"," +
            "\"ARCHIVEPATH\":\"arch-path\"," +
            "\"WORKPATH\":\"work-path\"," +
            "\"UPDATEDINFO\":3," +
            "\"HOSTIDS\":\"<hostids><hostid>sup1</hostid></hostids>\"," +
            "\"SPRETASKS\":\"<tasks><task><type>LOG</type><path>spretasks</path><delay>0</delay></task></tasks>\"," +
            "\"SERRORTASKS\":\"<tasks><task><type>LOG</type><path>serrortasks</path><delay>0</delay></task></tasks>\"," +
            "\"RPRETASKS\":\"<tasks><task><type>LOG</type><path>rpretasks</path><delay>0</delay></task></tasks>\"," +
            "\"RPOSTTASKS\":\"<tasks><task><type>LOG</type><path>rposttasks</path><delay>0</delay></task></tasks>\"," +
            "\"RERRORTASKS\":\"<tasks><task><type>LOG</type><path>rerrortasks</path><delay>0</delay></task></tasks>\"," +
            "\"SPOSTTASKS\":\"<tasks><task><type>LOG</type><path>sposttasks</path><delay>0</delay></task></tasks>\"" +
            "}";

		String got = JsonHandler.writeAsString(tested);

		assertEquals(expected, got);
	}
}
