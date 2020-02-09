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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class DbRuleTest {
    @Test
    public void testJsonSerialisation() {
        List<String> hosts = Arrays.asList("sup1");
        List<RuleTask> tasks = Arrays.asList();
		Rule tested = new Rule("rulename", 3, hosts, "recv-path", "send-path",
            "arch-path", "work-path", tasks, tasks, tasks, tasks, tasks, tasks,
            UpdatedInfo.TOSUBMIT);
        DbRule dbRule = new DbRule(tested);

        String expected = "{" +
            "\"@model\":\"DbRule\"," +
            "\"IDRULE\":\"rulename\"," +
            "\"MODETRANS\":3," +
            "\"RECVPATH\":\"/recv-path\"," +
            "\"SENDPATH\":\"/send-path\"," +
            "\"ARCHIVEPATH\":\"/arch-path\"," +
            "\"WORKPATH\":\"/work-path\"," +
            "\"UPDATEDINFO\":3," +
            "\"HOSTIDS\":\"<hostids><hostid>sup1</hostid></hostids>\"," +
            "\"RPRETASKS\":\"<tasks></tasks>\"," +
            "\"RPOSTTASKS\":\"<tasks></tasks>\"," +
            "\"SPRETASKS\":\"<tasks></tasks>\"," +
            "\"SPOSTTASKS\":\"<tasks></tasks>\"," +
            "\"RERRORTASKS\":\"<tasks></tasks>\"," +
            "\"SERRORTASKS\":\"<tasks></tasks>\"" +
            "}";


        ObjectNode asJson = dbRule.getJson();
        String got = JsonHandler.writeAsString(asJson);

        assertEquals(expected, got);
    }
}
