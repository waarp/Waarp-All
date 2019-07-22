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
package org.waarp.thrift.test;

import org.apache.thrift.TException;
import org.waarp.thrift.r66.ErrorCode;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Result;
import org.waarp.thrift.r66.R66Service;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class R66ServiceImpl implements R66Service.Iface {

  @Override
  public R66Result transferRequestQuery(R66Request request) throws TException {
    final R66Result result =
        new R66Result(request.getMode(), ErrorCode.CompleteOk, "Test only");
    result.setFromuid(request.getFromuid());
    result.setDestuid(request.getDestuid());
    result.setFile("Target_" + request.getFile());
    result.setRule(request.getRule());
    result.setTid(System.currentTimeMillis());
    result.setGloballaststep(2);
    result.setGlobalstep(2);
    result.setStep(1);
    result.setRank(1023);
    result.setRetrievemode(true);
    result.setIsmoved(false);
    result.setOriginalfilename(request.getFile());
    result.setModetransfer(4);
    result.setStart("2009-08-13 12:26:43.209");
    result.setStart("2009-08-13 12:26:46.079");
    return result;
  }

  @Override
  public R66Result infoTransferQuery(R66Request request) throws TException {
    final R66Result result =
        new R66Result(request.getMode(), ErrorCode.CompleteOk,
                      "Test Info only");
    result.setDestuid(request.getDestuid());
    result.setTid(request.getTid());
    result.setAction(request.getAction());
    result.setGloballaststep(2);
    result.setGlobalstep(2);
    result.setStep(1);
    result.setRank(1023);
    result.setRetrievemode(true);
    result.setIsmoved(false);
    result.setModetransfer(4);
    result.setStart("2009-08-13 12:26:43.209");
    result.setStart("2009-08-13 12:26:46.079");
    return result;
  }

  @Override
  public boolean isStillRunning(String fromuid, String touid, long tid)
      throws TException {
    return true;
  }

  @Override
  public List<String> infoListQuery(R66Request request) throws TException {
    final List<String> list = new ArrayList<String>(2);
    list.add("listing file1 test 000");
    list.add("listing file12 test 123");
    return list;
  }

}
