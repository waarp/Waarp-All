/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/**
 *
 */
package org.waarp.openr66.protocol.junit;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.protocol.test.TestTasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author frederic
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AllTasksTest extends TestAbstract {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClassMinimal("Linux/config/config-serverInitB.xml");
        setUpDbBeforeClass();
    }

    @Test
    public void test_Tasks() throws IOException, OpenR66RunnerErrorException,
                                     CommandAbstractException {
        System.err.println("Start Tasks");
        File totest = new File("/tmp/R66/in/testTask.txt");
        FileWriter fileWriter = new FileWriter(totest);
        fileWriter.write("Test content");
        fileWriter.flush();
        fileWriter.close();
        TestTasks.main(new String[] {
                new File(dir, "config-serverA-minimal.xml").getAbsolutePath(),
                "/tmp/R66/in",
                "/tmp/R66/out", totest.getName()
        });
        System.err.println("End Tasks");
    }

}
