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

package org.waarp.openr66.protocol.junit;

import org.junit.Test;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.openr66.context.task.FtpArgs;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

import static org.junit.Assert.*;

/**
 * FtpArgs Tester.
 */
public class FtpArgsTest {


  /**
   * Method: getFtpArgs(String[] args)
   */
  @Test
  public void testGetFtpArgs() throws Exception {
    String[] args0 = {
        "-file", "filesource", "-to", "host", "-port", "a1234"
    };
    FtpArgs ftpArgs;
    try {
      ftpArgs = FtpArgs.getFtpArgs(args0);
      fail("Should raised an exception");
    } catch (OpenR66RunnerErrorException e) {
      //ignore
    }
    String[] args01 = {
        "-file", "filesource", "-to", "host", "-port", "a1234", "-user",
        "userftp", "-pwd", "userpwd"
    };
    try {
      ftpArgs = FtpArgs.getFtpArgs(args01);
      fail("Should raised an exception");
    } catch (OpenR66RunnerErrorException e) {
      //ignore
    }
    String[] args02 = {
        "-file", "filesource", "-to", "host", "-port", "1234", "-user",
        "userftp", "-pwd", "userpwd", "-command", "fake"
    };
    try {
      ftpArgs = FtpArgs.getFtpArgs(args02);
      fail("Should raised an exception");
    } catch (OpenR66RunnerErrorException e) {
      //ignore
    }
    String[] args = {
        "-file", "filesource", "-to", "host", "-port", "1234", "-user",
        "userftp", "-pwd", "userpwd", "-account", "accountftp", "-mode",
        "active", "-ssl", "no", "-cwd", "remotepath", "-digest", "sha256",
        "-pre", "torun,new,arg", "-command", "get", "-post",
        "torunafter,new2,arg2"
    };
    ftpArgs = FtpArgs.getFtpArgs(args);
    assertEquals(ftpArgs.getFilepath(), "filesource");
    assertEquals(ftpArgs.getRequested(), "host");
    assertEquals(ftpArgs.getUser(), "userftp");
    assertEquals(ftpArgs.getPwd(), "userpwd");
    assertEquals(ftpArgs.getAcct(), "accountftp");
    assertEquals(ftpArgs.getCwd(), "remotepath");
    assertEquals(ftpArgs.getPreArgs(), "torun new arg");
    assertEquals(ftpArgs.getPostArgs(), "torunafter new2 arg2");
    assertEquals(ftpArgs.getCommand(), "get");
    assertEquals(ftpArgs.getDigest(), DigestAlgo.SHA256);
    assertEquals(ftpArgs.getPort(), 1234);
    assertEquals(ftpArgs.getSsl(), 0);
    assertEquals(ftpArgs.getCodeCommand(), -1);
    assertTrue(!ftpArgs.isPassive());
    String[] args2 = {
        "-file", "filesource", "-to", "host", "-port", "1234", "-user",
        "userftp", "-pwd", "userpwd", "-account", "accountftp", "-mode",
        "passive", "-ssl", "implicit", "-cwd", "remotepath", "-digest",
        "sha512", "-pre", "torun,new,arg", "-command", "put", "-post",
        "torunafter,new2,arg2"
    };
    ftpArgs = FtpArgs.getFtpArgs(args2);
    assertEquals(ftpArgs.getFilepath(), "filesource");
    assertEquals(ftpArgs.getRequested(), "host");
    assertEquals(ftpArgs.getUser(), "userftp");
    assertEquals(ftpArgs.getPwd(), "userpwd");
    assertEquals(ftpArgs.getAcct(), "accountftp");
    assertEquals(ftpArgs.getCwd(), "remotepath");
    assertEquals(ftpArgs.getPreArgs(), "torun new arg");
    assertEquals(ftpArgs.getPostArgs(), "torunafter new2 arg2");
    assertEquals(ftpArgs.getCommand(), "put");
    assertEquals(ftpArgs.getDigest(), DigestAlgo.SHA512);
    assertEquals(ftpArgs.getPort(), 1234);
    assertEquals(ftpArgs.getSsl(), -1);
    assertEquals(ftpArgs.getCodeCommand(), 1);
    assertTrue(ftpArgs.isPassive());
    String[] args3 = {
        "-file", "filesource", "-to", "host", "-port", "1234", "-user",
        "userftp", "-pwd", "userpwd", "-command", "append", "-ssl", "explicit"
    };
    ftpArgs = FtpArgs.getFtpArgs(args3);
    assertEquals(ftpArgs.getFilepath(), "filesource");
    assertEquals(ftpArgs.getRequested(), "host");
    assertEquals(ftpArgs.getUser(), "userftp");
    assertEquals(ftpArgs.getPwd(), "userpwd");
    assertEquals(ftpArgs.getAcct(), null);
    assertEquals(ftpArgs.getCwd(), null);
    assertEquals(ftpArgs.getPreArgs(), null);
    assertEquals(ftpArgs.getPostArgs(), null);
    assertEquals(ftpArgs.getCommand(), "append");
    assertEquals(ftpArgs.getDigest(), null);
    assertEquals(ftpArgs.getPort(), 1234);
    assertEquals(ftpArgs.getSsl(), 1);
    assertEquals(ftpArgs.getCodeCommand(), 2);
    assertTrue(ftpArgs.isPassive());

  }

} 
