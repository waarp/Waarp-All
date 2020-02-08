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

import static org.junit.Assert.*;


public class DbHostAuthTest {
    @Test
    public void testJsonSerialisation() {
        DbHostAuth dbHostConf = new DbHostAuth("hostid", "127.0.0.1", 6666,
                false, null, true, true);

        String expected = "{" +
                "\"@model\":\"DbHostAuth\"," +
                "\"HOSTID\":\"hostid\"," +
                "\"ADDRESS\":\"127.0.0.1\"," +
                "\"PORT\":6666," +
                "\"HOSTKEY\":null," +
                "\"ISSSL\":false," +
                "\"ISCLIENT\":true," +
                "\"ISPROXIFIED\":false," +
                "\"ADMINROLE\":true," +
                "\"ISACTIVE\":true," +
                "\"UPDATEDINFO\":0" +
            "}";


        ObjectNode asJson = dbHostConf.getJson();
        String got = JsonHandler.writeAsString(asJson);

        assertEquals(expected, got);
    }
}
