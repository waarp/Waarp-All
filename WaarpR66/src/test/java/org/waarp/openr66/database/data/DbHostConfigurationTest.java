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
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.UpdatedInfo;

import static org.junit.Assert.*;

public class DbHostConfigurationTest {
    @Test
    public void testJsonSerialisation() {
		Business business = new Business("Test", "business", "roles", "aliases",
                "others", UpdatedInfo.UNKNOWN);
        DbHostConfiguration dbHostConf = new DbHostConfiguration(business);

        String expected = "{" +
            "\"@model\":\"DbHostConfiguration\"," +
            "\"HOSTID\":\"Test\"," +
            "\"BUSINESS\":\"business\"," +
            "\"ROLES\":\"roles\"," +
            "\"ALIASES\":\"aliases\"," +
            "\"OTHERS\":\"others\"," +
            "\"UPDATEDINFO\":0" +
            "}";


        ObjectNode asJson = dbHostConf.getJson();
        String got = JsonHandler.writeAsString(asJson);

        assertEquals(expected, got);
    }
}
