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


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DbRuleTest {
  @Test
  public void testJsonSerialisation() {
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

    Rule tested =
        new Rule("rulename", 3, hosts, "recv-path", "send-path", "arch-path",
                 "work-path", rpretasks, rposttasks, rerrortasks, spretasks,
                 sposttasks, serrortasks, UpdatedInfo.TOSUBMIT);
    DbRule dbRule = new DbRule(tested);

    String expected =
        "{" + "\"@model\":\"DbRule\"," + "\"IDRULE\":\"rulename\"," +
        "\"MODETRANS\":3," + "\"RECVPATH\":\"/recv-path\"," +
        "\"SENDPATH\":\"/send-path\"," + "\"ARCHIVEPATH\":\"/arch-path\"," +
        "\"WORKPATH\":\"/work-path\"," + "\"UPDATEDINFO\":3," +
        "\"HOSTIDS\":\"<hostids><hostid>sup1</hostid></hostids>\"," +
        "\"SPRETASKS\":\"<tasks><task><type>LOG</type><path>spretasks</path><delay>0</delay></task></tasks>\"," +
        "\"SERRORTASKS\":\"<tasks><task><type>LOG</type><path>serrortasks</path><delay>0</delay></task></tasks>\"," +
        "\"RPRETASKS\":\"<tasks><task><type>LOG</type><path>rpretasks</path><delay>0</delay></task></tasks>\"," +
        "\"RPOSTTASKS\":\"<tasks><task><type>LOG</type><path>rposttasks</path><delay>0</delay></task></tasks>\"," +
        "\"RERRORTASKS\":\"<tasks><task><type>LOG</type><path>rerrortasks</path><delay>0</delay></task></tasks>\"," +
        "\"SPOSTTASKS\":\"<tasks><task><type>LOG</type><path>sposttasks</path><delay>0</delay></task></tasks>\"" +
        "}";


    ObjectNode asJson = dbRule.getJson();
    String got = JsonHandler.writeAsString(asJson);

    assertEquals(expected, got);
  }
}
