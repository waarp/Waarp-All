/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 3.0 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.gateway.kernel.exec;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;

/**
 * R66PreparedTransferExecutor class. If the command starts with "REFUSED", the command will be
 * refused for execution. If "REFUSED" is set, the command "RETR" or "STOR" like operations will be
 * stopped at starting of command.
 * 
 * 
 * 
 * Format is like r66send command in any order except "-info" which should be the last item:<br>
 * "-to Host -file FILE -rule RULE [-md5] [-nolog] [-start yyyyMMddHHmmss or -delay (delay or +delay)] [-info INFO]" <br>
 * <br>
 * INFO is the only one field that can contains blank character.<br>
 * <br>
 * The following replacement are done dynamically before the command is executed:<br>
 * - #BASEPATH# is replaced by the full path for the root of FTP Directory<br>
 * - #FILE# is replaced by the current file path relative to FTP Directory (so #BASEPATH##FILE# is
 * the full path of the file)<br>
 * - #USER# is replaced by the username<br>
 * - #ACCOUNT# is replaced by the account<br>
 * - #COMMAND# is replaced by the command issued for the file<br>
 * - #SPECIALID# is replaced by the FTP id of the transfer (whatever in or out)<br>
 * - #UUID# is replaced by a special UUID globally unique for the transfer, in general to be placed in -info part (for instance ##UUID## giving #uuid#)<br>
 * <br>
 * So for instance
 * "-to Host -file #BASEPATH##FILE# -rule RULE [-md5] [-nolog] [-delay +delay]  [-info ##UUID## #USER# #ACCOUNT# #COMMAND# INFO]" <br>
 * will be a standard use of this function.
 * 
 * @author Frederic Bregier
 * 
 */
public class R66PreparedTransferExecutor extends AbstractExecutor {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(R66PreparedTransferExecutor.class);

    protected final WaarpFuture future;

    protected String filename = null;

    protected String rulename = null;

    protected String fileinfo = null;

    protected boolean isMD5 = false;

    protected boolean nolog = false;

    protected Timestamp timestart = null;

    protected String remoteHost = null;

    protected int blocksize = Configuration.configuration.getBLOCKSIZE();;

    protected DbSession dbsession;

    /**
     * 
     * @param command
     * @param delay
     * @param futureCompletion
     */
    public R66PreparedTransferExecutor(String command, long delay,
            WaarpFuture futureCompletion) {
        String args[] = command.split(" ");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-to")) {
                i++;
                remoteHost = args[i];
                if (Configuration.configuration.getAliases().containsKey(remoteHost)) {
                    remoteHost = Configuration.configuration.getAliases().get(remoteHost);
                }
            } else if (args[i].equalsIgnoreCase("-file")) {
                i++;
                filename = args[i];
            } else if (args[i].equalsIgnoreCase("-rule")) {
                i++;
                rulename = args[i];
            } else if (args[i].equalsIgnoreCase("-info")) {
                i++;
                fileinfo = args[i];
                i++;
                while (i < args.length) {
                    fileinfo += " " + args[i];
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
            } else if (args[i].equalsIgnoreCase("-nolog")) {
                nolog = true;
                i++;
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
                if (args[i].charAt(0) == '+') {
                    timestart = new Timestamp(System.currentTimeMillis() +
                            Long.parseLong(args[i].substring(1)));
                } else {
                    timestart = new Timestamp(Long.parseLong(args[i]));
                }
            }
        }
        if (fileinfo == null) {
            fileinfo = "noinfo";
        }
        this.future = futureCompletion;
    }

    /**
     * @param dbsession
     *            the dbsession to set
     */
    public void setDbsession(DbSession dbsession) {
        this.dbsession = dbsession;
    }

    public void run() throws CommandAbstractException {
        String message = "R66Prepared with -to " + remoteHost + " -rule " +
                rulename + " -file " + filename + " -nolog: " + nolog +
                " -isMD5: " + isMD5 + " -info " + fileinfo;
        if (remoteHost == null || rulename == null || filename == null) {
            logger.error("Mandatory argument is missing: -to " + remoteHost +
                    " -rule " + rulename + " -file " + filename);
            throw new Reply421Exception("Mandatory argument is missing\n    " + message);
        }
        logger.debug(message);
        DbRule rule;
        try {
            rule = new DbRule(rulename);
        } catch (WaarpDatabaseException e) {
            logger.error("Cannot get Rule: " + rulename + " since {}\n    " +
                    message, e.getMessage());
            throw new Reply421Exception("Cannot get Rule: " +
                    rulename + "\n    " + message);
        }
        int mode = rule.getMode();
        if (isMD5) {
            mode = RequestPacket.getModeMD5(mode);
        }
        String sep = PartnerConfiguration.getSeparator(remoteHost);
        long originalSize = -1;
        if (RequestPacket.isSendMode(mode) && !RequestPacket.isThroughMode(mode)) {
            File file = new File(filename);
            if (file.canRead()) {
                originalSize = file.length();
            }
        }
        RequestPacket request = new RequestPacket(rulename, mode, filename,
                blocksize, 0, DbConstant.ILLEGALVALUE, fileinfo, originalSize, sep);
        // Not isRecv since it is the requester, so send => isRetrieve is true
        boolean isRetrieve = !RequestPacket.isRecvMode(request.getMode());
        logger.debug("Will prepare: {}", request);
        DbTaskRunner taskRunner;
        try {
            taskRunner = new DbTaskRunner(rule, isRetrieve, request,
                    remoteHost, timestart);
        } catch (WaarpDatabaseException e) {
            logger.error("Cannot get new task since {}\n    " + message, e
                    .getMessage());
            throw new Reply421Exception("Cannot get new task\n    " + message);
        }
        taskRunner.changeUpdatedInfo(AbstractDbData.UpdatedInfo.TOSUBMIT);
        if (!taskRunner.forceSaveStatus()) {
            try {
                if (!taskRunner.specialSubmit()) {
                    logger.error("Cannot prepare task: " + message);
                    throw new Reply421Exception("Cannot get new task\n    " + message);
                }
            } catch (WaarpDatabaseException e) {
                logger.error("Cannot prepare task since {}\n    " + message, e
                        .getMessage());
                throw new Reply421Exception("Cannot get new task\n    " + message);
            }
        }
        logger.debug("R66PreparedTransfer prepared: {}", request);
        future.setSuccess();
    }
}
