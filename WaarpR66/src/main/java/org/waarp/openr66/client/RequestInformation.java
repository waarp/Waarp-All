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
package org.waarp.openr66.client;

import java.net.SocketAddress;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Class to request information on remote files
 * 
 * @author Frederic Bregier
 * 
 */
public class RequestInformation implements Runnable {
    /**
     * Internal Logger
     */
    static volatile WaarpLogger logger;

    protected static String _INFO_ARGS =
            Messages.getString("RequestInformation.0") + Messages.getString("Message.OutputFormat"); //$NON-NLS-1$

    protected final NetworkTransaction networkTransaction;
    final R66Future future;
    String requested = null;
    String filename = null;
    String rulename = null;
    byte code;
    long id = DbConstant.ILLEGALVALUE;
    boolean isTo = true;
    boolean normalInfoAsWarn = true;

    static String srequested = null;
    static String sfilename = null;
    static String srulename = null;
    static byte scode = -1;
    static long sid = DbConstant.ILLEGALVALUE;
    static boolean sisTo = true;
    static protected boolean snormalInfoAsWarn = true;

    /**
     * Parse the parameter and set current values
     * 
     * @param args
     * @return True if all parameters were found and correct
     */
    protected static boolean getParams(String[] args) {
        _INFO_ARGS = Messages.getString("RequestInformation.0") + Messages.getString("Message.OutputFormat"); //$NON-NLS-1$
        if (args.length < 5) {
            logger
                    .error(_INFO_ARGS);
            return false;
        }
        if (!FileBasedConfiguration
                .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
            logger
                    .error(Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
            return false;
        }
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-to")) {
                i++;
                srequested = args[i];
                if (Configuration.configuration.getAliases().containsKey(srequested)) {
                    srequested = Configuration.configuration.getAliases().get(srequested);
                }
            } else if (args[i].equalsIgnoreCase("-file")) {
                i++;
                sfilename = args[i];
                sfilename = sfilename.replace('§', '*');
            } else if (args[i].equalsIgnoreCase("-rule")) {
                i++;
                srulename = args[i];
            } else if (args[i].equalsIgnoreCase("-logWarn")) {
                snormalInfoAsWarn = true;
            } else if (args[i].equalsIgnoreCase("-notlogWarn")) {
                snormalInfoAsWarn = false;
            } else if (args[i].equalsIgnoreCase("-exist")) {
                scode = (byte) InformationPacket.ASKENUM.ASKEXIST.ordinal();
            } else if (args[i].equalsIgnoreCase("-detail")) {
                scode = (byte) InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal();
            } else if (args[i].equalsIgnoreCase("-list")) {
                scode = (byte) InformationPacket.ASKENUM.ASKLIST.ordinal();
            } else if (args[i].equalsIgnoreCase("-mlsx")) {
                scode = (byte) InformationPacket.ASKENUM.ASKMLSLIST.ordinal();
            } else if (args[i].equalsIgnoreCase("-id")) {
                i++;
                sid = Long.parseLong(args[i]);
            } else if (args[i].equalsIgnoreCase("-reqfrom")) {
                sisTo = true;
            } else if (args[i].equalsIgnoreCase("-reqto")) {
                sisTo = false;
            }
        }
        OutputFormat.getParams(args);
        if (sfilename != null && scode == -1) {
            scode = (byte) InformationPacket.ASKENUM.ASKEXIST.ordinal();
        }
        if ((srulename == null && scode != -1) || srequested == null) {
            logger.error(Messages.getString("RequestInformation.12") + _INFO_ARGS); //$NON-NLS-1$
            return false;
        }
        if (scode != -1 && sid != DbConstant.ILLEGALVALUE) {
            logger.error(Messages.getString("RequestInformation.13") + _INFO_ARGS); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    /**
     * @param future
     * @param requested
     * @param rulename
     * @param filename
     * @param request
     * @param id
     *            Id of the request
     * @param isTo
     *            request is To remote Host (true), or From remote host (false)
     * @param networkTransaction
     */
    public RequestInformation(R66Future future, String requested, String rulename,
            String filename, byte request, long id, boolean isTo,
            NetworkTransaction networkTransaction) {
        this.future = future;
        this.rulename = rulename;
        this.requested = requested;
        this.filename = filename;
        this.code = request;
        this.id = id;
        this.isTo = isTo;
        this.networkTransaction = networkTransaction;
    }

    public void run() {
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(RequestInformation.class);
        }
        InformationPacket request = null;
        if (code != -1) {
            request = new InformationPacket(rulename, code, filename);
        } else {
            request = new InformationPacket("" + id, code, (isTo ? "1" : "0"));
        }

        // Connection
        DbHostAuth host = R66Auth.getServerAuth(DbConstant.admin.getSession(),
                requested);
        if (host == null) {
            logger.error(Messages.getString("Message.HostNotFound") + requested); //$NON-NLS-1$
            R66Result result = new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
            this.future.setResult(result);
            this.future.cancel();
            return;
        }
        if (host.isClient()) {
            logger.error(Messages.getString("Message.HostIsClient") + requested); //$NON-NLS-1$
            R66Result result = new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
            this.future.setResult(result);
            this.future.cancel();
            return;
        }
        SocketAddress socketAddress = host.getSocketAddress();
        boolean isSSL = host.isSsl();

        LocalChannelReference localChannelReference = networkTransaction
                .createConnectionWithRetry(socketAddress, isSSL, future);
        socketAddress = null;
        if (localChannelReference == null) {
            logger.error(Messages.getString("AdminR66OperationsGui.188") + requested); //$NON-NLS-1$
            R66Result result = new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
            this.future.setResult(result);
            this.future.cancel();
            return;
        }
        localChannelReference.sessionNewState(R66FiniteDualStates.INFORMATION);
        try {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference, request, false);
        } catch (OpenR66ProtocolPacketException e) {
            logger.error(Messages.getString("RequestInformation.20")); //$NON-NLS-1$
            R66Result result = new R66Result(null, true, ErrorCode.TransferError, null);
            this.future.setResult(result);
            this.future.cancel();
            return;
        }
        localChannelReference.getFutureRequest().awaitUninterruptibly();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(RequestInformation.class);
        }
        if (!getParams(args)) {
            logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
            if (!OutputFormat.isQuiet()) {
                System.out.println(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
            }
            if (DbConstant.admin != null && DbConstant.admin.isActive()) {
                DbConstant.admin.close();
            }
            ChannelUtils.stopLogger();
            System.exit(1);
        }
        NetworkTransaction networkTransaction = null;
        int value = 3;
        try {
            Configuration.configuration.pipelineInit();
            networkTransaction = new NetworkTransaction();
            R66Future result = new R66Future(true);
            RequestInformation requestInformation =
                    new RequestInformation(result, srequested, srulename,
                            sfilename, scode, sid, sisTo,
                            networkTransaction);
            requestInformation.normalInfoAsWarn = snormalInfoAsWarn;
            requestInformation.run();
            result.awaitUninterruptibly();
            // if transfer information request (code = -1) => middle empty and header = Runner as XML
            // if listing request => middle = nb of files, header = list of files in native/list/mlsx/exist (true/false) format, 1 file per line
            OutputFormat outputFormat = new OutputFormat(RequestInformation.class.getSimpleName(), args);
            if (result.isSuccess()) {
                value = 0;
                R66Result r66result = result.getResult();
                ValidPacket info = (ValidPacket) r66result.getOther();
                outputFormat.setValue(FIELDS.status.name(), 0);
                outputFormat.setValue(FIELDS.statusTxt.name(), Messages.getString("RequestInformation.Success")); //$NON-NLS-1$
                outputFormat.setValue(FIELDS.remote.name(), srequested);
                if (requestInformation.code != -1) {
                    outputFormat.setValue("nb", Integer.parseInt(info.getSmiddle()));
                    String[] files = info.getSheader().split("\n");
                    int i = 0;
                    for (String file : files) {
                        i++;
                        outputFormat.setValue("file" + i, file);
                    }
                } else {
                    try {
                        DbTaskRunner runner = DbTaskRunner.fromStringXml(info.getSheader(), false);
                        outputFormat.setValueString(runner.getJson());
                    } catch (OpenR66ProtocolBusinessException e) {
                        outputFormat.setValue("Id", requestInformation.id);
                        outputFormat.setValue(FIELDS.transfer.name(), info.getSheader());
                    }
                }
                if (requestInformation.normalInfoAsWarn) {
                    logger.warn(outputFormat.loggerOut());
                } else {
                    logger.info(outputFormat.loggerOut());
                }
                if (!OutputFormat.isQuiet()) {
                    outputFormat.sysout();
                }
            } else {
                value = 2;
                outputFormat.setValue(FIELDS.status.name(), 2);
                outputFormat.setValue(FIELDS.statusTxt.name(), Messages.getString("RequestInformation.Failure")); //$NON-NLS-1$
                outputFormat.setValue(FIELDS.remote.name(), srequested);
                outputFormat.setValue(FIELDS.error.name(), result.getResult().toString());
                logger.error(outputFormat.loggerOut());
                if (!OutputFormat.isQuiet()) {
                    outputFormat.sysout();
                }
            }
        } catch (Throwable e) {
            logger.error("Exception", e);
        } finally {
            if (networkTransaction != null) {
                networkTransaction.closeAll();
            }
            if (DbConstant.admin != null) {
                DbConstant.admin.close();
            }
            System.exit(value);
        }
    }

}
