/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context.task;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.exec.CommandLine;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Transfer task:<br>
 * 
 * Result of arguments will be as r66send command.<br>
 * Format is like r66send command in any order except "-info" which should be the last item, and "-copyinfo"
 * will copy at first place the original transfer information as the new one, while still having the possibility to add new informations through "-info":<br>
 * "-file filepath -to requestedHost -rule rule [-md5] [-start yyyyMMddHHmmss or -delay (delay or +delay)] [-copyinfo] [-info information]" <br>
 * <br>
 * INFO is the only one field that can contains blank character.<br>
 * 
 * @author Frederic Bregier
 * 
 */
public class TransferTask extends AbstractExecTask {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(TransferTask.class);

    /**
     * @param argRule
     * @param delay
     * @param argTransfer
     * @param session
     */
    public TransferTask(String argRule, int delay, String argTransfer,
            R66Session session) {
        super(TaskType.TRANSFER, delay, argRule, argTransfer, session);
    }

    @Override
    public void run() {
        logger.info("Transfer with " + argRule + ":" + argTransfer + " and {}",
                session);
        String finalname = applyTransferSubstitutions(argRule);

        finalname = finalname.replaceAll("#([A-Z]+)#", "\\${$1}");
        CommandLine commandLine = new CommandLine("dummy");
        commandLine.setSubstitutionMap(getSubstitutionMap());
        commandLine.addArguments(finalname, false);
        String[] args = commandLine.getArguments();

        if (args.length < 6) {
            futureCompletion.setFailure(
                    new OpenR66RunnerErrorException("Not enough argument in Transfer"));
            return;
        }
        String filepath = null;
        String requested = null;
        String rule = null;
        String information = null;
        String finalInformation = null;
        boolean isMD5 = false;
        int blocksize = Configuration.configuration.getBLOCKSIZE();
        Timestamp timestart = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-to")) {
                i++;
                requested = args[i];
                if (Configuration.configuration.getAliases().containsKey(requested)) {
                    requested = Configuration.configuration.getAliases().get(requested);
                }
            } else if (args[i].equalsIgnoreCase("-file")) {
                i++;
                filepath = args[i];
            } else if (args[i].equalsIgnoreCase("-rule")) {
                i++;
                rule = args[i];
            } else if (args[i].equalsIgnoreCase("-copyinfo")) {
                finalInformation = argTransfer;
            } else if (args[i].equalsIgnoreCase("-info")) {
                i++;
                information = args[i];
                i++;
                while (i < args.length) {
                    information += " " + args[i];
                    i++;
                }
            } else if (args[i].equalsIgnoreCase("-md5")) {
                isMD5 = true;
            } else if (args[i].equalsIgnoreCase("-block")) {
                i++;
                blocksize = Integer.parseInt(args[i]);
                if (blocksize < 100) {
                    logger.warn("Block size is too small: " + blocksize);
                    blocksize = Configuration.configuration.getBLOCKSIZE();
                }
            } else if (args[i].equalsIgnoreCase("-start")) {
                i++;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                Date date;
                try {
                    date = dateFormat.parse(args[i]);
                    timestart = new Timestamp(date.getTime());
                } catch (ParseException e) {
                }
            } else if (args[i].equalsIgnoreCase("-delay")) {
                i++;
                try {
                    if (args[i].charAt(0) == '+') {
                        timestart = new Timestamp(System.currentTimeMillis() +
                                Long.parseLong(args[i].substring(1)));
                    } else {
                        timestart = new Timestamp(Long.parseLong(args[i]));
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (information != null) {
            if (finalInformation == null) {
                finalInformation = information;
            } else {
                finalInformation += " " + information;
            }
        } else if (finalInformation == null) {
            finalInformation = "noinfo";
        }
        R66Future future = new R66Future(true);
        SubmitTransfer transaction = new SubmitTransfer(future,
                requested, filepath, rule, finalInformation, isMD5, blocksize, DbConstant.ILLEGALVALUE,
                timestart);
        transaction.run();
        future.awaitUninterruptibly();
        futureCompletion.setResult(future.getResult());
        DbTaskRunner runner = future.getResult().getRunner();
        if (future.isSuccess()) {
            logger.info("Prepare transfer in     SUCCESS     " + runner.toShortString() +
                    "     <REMOTE>" + requested + "</REMOTE>");
            futureCompletion.setSuccess();
        } else {
            if (runner != null) {
                if (future.getCause() == null) {
                    futureCompletion.cancel();
                } else {
                    futureCompletion.setFailure(future.getCause());
                }
                logger.error("Prepare transfer in     FAILURE      " + runner.toShortString() +
                        "     <REMOTE>" + requested + "</REMOTE>", future.getCause());
            } else {
                if (future.getCause() == null) {
                    futureCompletion.cancel();
                } else {
                    futureCompletion.setFailure(future.getCause());
                }
                logger.error("Prepare transfer in     FAILURE without any runner back",
                        future.getCause());
            }
        }
    }

}
