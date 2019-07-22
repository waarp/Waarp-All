/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.localhandler;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.netty.channel.Channel;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.configuration.AuthenticationFileBasedConfiguration;
import org.waarp.openr66.configuration.RuleFileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.ExecJavaTask;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessRemoteFileNotFoundException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.BlockRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ShutdownPacket;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.NbAndSpecialId;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;
import org.waarp.openr66.protocol.utils.TransferUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class to implement actions related to extra server actions: shutdown, bandwidth control,
 * configuration import/export, log purge, request restart/stop/cancel, business request, block new request control,
 * information request and transfer request.
 * 
 * Can be used in both standard mode (original packet), or in JSON mode.
 * 
 * @author "Frederic Bregier"
 *
 */
public class ServerActions extends ConnectionActions {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ServerActions.class);

    public ServerActions() {
    }

    /**
     * Test reception
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolPacketException
     */
    public void test(Channel channel, TestPacket packet)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolPacketException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Test received");
        }
        // simply write back after+1
        packet.update();
        if (packet.getType() == LocalPacketFactory.VALIDPACKET) {
            ValidPacket validPacket = new ValidPacket(packet.toString(), null,
                    LocalPacketFactory.TESTPACKET);
            R66Result result = new R66Result(session, true,
                    ErrorCode.CompleteOk, null);
            result.setOther(validPacket);
            session.newState(VALIDOTHER);
            localChannelReference.validateRequest(result);
            ChannelUtils.writeAbstractLocalPacket(localChannelReference, validPacket, true);
            logger.warn("Valid TEST MESSAGE from " +
                    session.getAuth().getUser() +
                    " [" + localChannelReference.getNetworkChannel().remoteAddress() +
                    "] Msg=" + packet.toString());
            ChannelCloseTimer.closeFutureChannel(channel);
            packet.clear();
        } else {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, false);
        }
    }

    /**
     * Receive a request of information
     * 
     * @param channel
     * @param packet
     * @throws CommandAbstractException
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolNoDataException
     * @throws OpenR66ProtocolPacketException
     */
    public void information(Channel channel, InformationPacket packet)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolNoDataException, OpenR66ProtocolPacketException {
        byte request = packet.getRequest();
        String rulename = packet.getRulename();
        String filename = packet.getFilename();
        packet.clear();
        long id = DbConstant.ILLEGALVALUE;
        if (request == -1) {
            try {
                id = Long.parseLong(rulename);
            } catch (NumberFormatException e) {
                logger.error("Incorrect Transfer ID", e);
                throw new OpenR66ProtocolNoDataException("Incorrect Transfer ID", e);
            }
        }
        boolean isTo = filename.equals("1");
        ValidPacket validPacket = null;
        if (request == -1) {
            validPacket = informationRequest(id, isTo, rulename, false);
        } else {
            validPacket = informationFile(request, rulename, filename, false);
        }
        if (validPacket != null) {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                    validPacket, true);
            channel.close();
        } else {
            session.newState(ERROR);
            ErrorPacket error = new ErrorPacket("Error while Request " + request,
                    ErrorCode.Internal.getCode(), ErrorPacket.FORWARDCLOSECODE);
            ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
            ChannelCloseTimer.closeFutureChannel(channel);
        }
    }

    /**
     * Receive a validation or a special request
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     * @throws OpenR66ProtocolBusinessException
     */
    public void valid(Channel channel, ValidPacket packet)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66RunnerErrorException, OpenR66ProtocolSystemException,
            OpenR66ProtocolBusinessException {
        // SHUTDOWNPACKET does not need authentication
        if (packet.getTypeValid() != LocalPacketFactory.SHUTDOWNPACKET &&
                (!session.isAuthenticated())) {
            logger.warn("Valid packet received while not authenticated: {} {}", packet, session);
            session.newState(ERROR);
            packet.clear();
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Valid received");
        }
        switch (packet.getTypeValid()) {
            case LocalPacketFactory.SHUTDOWNPACKET: {
                int rank = -1;
                if (session.getRunner() != null &&
                        session.getRunner().isInTransfer()) {
                    String srank = packet.getSmiddle();
                    if (srank != null && !srank.isEmpty()) {
                        // Save last rank from remote point of view
                        try {
                            rank = Integer.parseInt(srank);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                R66Result result = new R66Result(
                        new OpenR66ProtocolShutdownException(), session, true,
                        ErrorCode.Shutdown, session.getRunner());
                result.setOther(packet);
                rank = shutdownRequest(result, rank);
                if (rank >= 0) {
                    packet.setSmiddle(Integer.toString(rank));
                    try {
                        ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet,
                                true);
                    } catch (OpenR66ProtocolPacketException e) {
                    }
                }
                shutdownLocalChannel(channel);
                break;
            }
            case LocalPacketFactory.STOPPACKET:
            case LocalPacketFactory.CANCELPACKET: {
                String[] keys = packet.getSmiddle().split(" ");
                long id = Long.parseLong(keys[2]);
                session.newState(VALIDOTHER);
                R66Result resulttest = stopOrCancel(packet.getTypeValid(), keys[0], keys[1], id);
                // inform back the requester
                ValidPacket valid = new ValidPacket(packet.getSmiddle(), resulttest.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                resulttest.setOther(packet);
                localChannelReference.validateRequest(resulttest);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                session.setStatus(27);
                channel.close();
                break;
            }
            case LocalPacketFactory.VALIDPACKET: {
                // header = ?; middle = requested+blank+requester+blank+specialId
                // note: might contains one more argument = time to reschedule in yyyyMMddHHmmss format
                String[] keys = packet.getSmiddle().split(" ");
                ValidPacket valid = null;
                if (keys.length < 3) {
                    // not enough args
                    valid = new ValidPacket(packet.getSmiddle(),
                            ErrorCode.IncorrectCommand.getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                    R66Result resulttest = new R66Result(
                            new OpenR66ProtocolBusinessRemoteFileNotFoundException("Not enough arguments"),
                            session, true,
                            ErrorCode.IncorrectCommand, null);
                    resulttest.setOther(packet);
                    localChannelReference.invalidateRequest(resulttest);
                } else {
                    long id = Long.parseLong(keys[2]);
                    Date date = null;
                    if (keys.length > 3) {
                        // time to reschedule in yyyyMMddHHmmss format
                        logger.debug("Debug: restart with " + keys[3]);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        try {
                            date = dateFormat.parse(keys[3]);
                        } catch (ParseException e) {
                        }
                    }
                    session.newState(VALIDOTHER);
                    R66Result result = requestRestart(keys[0], keys[1], id, date);
                    valid = new ValidPacket(packet.getSmiddle(),
                            result.getCode().getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                    result.setOther(packet);
                    if (isCodeValid(result.getCode())) {
                        localChannelReference.validateRequest(result);
                    } else {
                        localChannelReference.invalidateRequest(result);
                    }
                }
                // inform back the requester
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.REQUESTUSERPACKET: {
                session.newState(VALIDOTHER);
                // Validate user request
                R66Result resulttest = new R66Result(session, true,
                        ErrorCode.getFromCode(packet.getSmiddle()), null);
                resulttest.setOther(packet);
                switch (resulttest.getCode()) {
                    case CompleteOk:
                    case InitOk:
                    case PostProcessingOk:
                    case PreProcessingOk:
                    case QueryAlreadyFinished:
                    case QueryStillRunning:
                    case Running:
                    case TransferOk:
                        break;
                    default:
                        localChannelReference.invalidateRequest(resulttest);
                        session.setStatus(102);
                        channel.close();
                        return;
                }
                localChannelReference.validateRequest(resulttest);
                session.setStatus(28);
                channel.close();
                break;
            }
            case LocalPacketFactory.LOGPACKET:
            case LocalPacketFactory.LOGPURGEPACKET: {
                session.newState(VALIDOTHER);
                // should be from the local server or from an authorized hosts: LOGCONTROL
                try {
                    if (!session.getAuth().getUser().equals(
                            Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                            !session.getAuth().isValidRole(ROLE.LOGCONTROL)) {
                        throw new OpenR66ProtocolNotAuthenticatedException(
                                "Not correctly authenticated");
                    }
                } catch (OpenR66ProtocolNoSslException e1) {
                    throw new OpenR66ProtocolNotAuthenticatedException(
                            "Not correctly authenticated since SSL is not supported", e1);
                }
                String sstart = packet.getSheader();
                String sstop = packet.getSmiddle();
                boolean isPurge = (packet.getTypeValid() == LocalPacketFactory.LOGPURGEPACKET);
                Timestamp start = (sstart == null || sstart.isEmpty()) ? null :
                        Timestamp.valueOf(sstart);
                Timestamp stop = (sstop == null || sstop.isEmpty()) ? null :
                        Timestamp.valueOf(sstop);
                packet.clear();
                // create export of log and optionally purge them from database
                String filename = Configuration.configuration.getBaseDirectory() +
                        Configuration.configuration.getArchivePath() + R66Dir.SEPARATOR +
                        Configuration.configuration.getHOST_ID() + "_" + System.currentTimeMillis() +
                        "_runners.xml";
                DbPreparedStatement statement = null;
                try {
                    statement =
                            DbTaskRunner.getLogPrepareStatement(
                                    localChannelReference.getDbSession(),
                                    start, stop);
                    DbTaskRunner.writeXMLWriter(statement, filename);
                } catch (WaarpDatabaseNoConnectionException e) {
                    throw new OpenR66ProtocolBusinessException(e);
                } catch (WaarpDatabaseSqlException e) {
                    throw new OpenR66ProtocolBusinessException(e);
                } finally {
                    if (statement != null) {
                        statement.realClose();
                    }
                }
                // in case of purge
                int nb = 0;
                if (isPurge) {
                    // purge in same interval all runners with globallaststep
                    // as ALLDONETASK or ERRORTASK
                    if (Configuration.configuration.getR66Mib() != null) {
                        Configuration.configuration.getR66Mib().notifyWarning(
                                "Purge Log Order received", session.getAuth().getUser());
                    }
                    try {
                        nb = DbTaskRunner.purgeLogPrepareStatement(
                                localChannelReference.getDbSession(),
                                start, stop);
                    } catch (WaarpDatabaseNoConnectionException e) {
                        throw new OpenR66ProtocolBusinessException(e);
                    } catch (WaarpDatabaseSqlException e) {
                        throw new OpenR66ProtocolBusinessException(e);
                    }
                }
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                // Now answer
                ValidPacket valid = new ValidPacket(filename + " " + nb, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.CONFEXPORTPACKET: {
                String shost = packet.getSheader();
                String srule = packet.getSmiddle();
                boolean bhost = Boolean.parseBoolean(shost);
                boolean brule = Boolean.parseBoolean(srule);
                packet.clear();
                session.newState(VALIDOTHER);
                String[] sresult = configExport(bhost, brule, false, false, false);
                R66Result result = null;
                if (sresult[0] != null || sresult[1] != null) {
                    result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                } else {
                    result = new R66Result(session, true, ErrorCode.TransferError, null);
                }
                // Now answer
                ValidPacket valid = new ValidPacket(shost + " " + srule, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.CONFIMPORTPACKET: {
                session.newState(VALIDOTHER);
                // Authentication must be the local server or CONFIGADMIN authorization
                try {
                    if (!session.getAuth().getUser().equals(
                            Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                            !session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
                        throw new OpenR66ProtocolNotAuthenticatedException(
                                "Not correctly authenticated");
                    }
                } catch (OpenR66ProtocolNoSslException e1) {
                    throw new OpenR66ProtocolNotAuthenticatedException(
                            "Not correctly authenticated since SSL is not supported", e1);
                }
                if (Configuration.configuration.getR66Mib() != null) {
                    Configuration.configuration.getR66Mib().notifyWarning(
                            "Import Configuration Order received", session.getAuth().getUser());
                }
                String shost = packet.getSheader();
                String srule = packet.getSmiddle();
                boolean bhostPurge = shost.startsWith("1 ");
                shost = shost.substring(2);
                boolean brulePurge = srule.startsWith("1 ");
                srule = srule.substring(2);
                boolean bhost = !shost.isEmpty();
                boolean brule = !srule.isEmpty();
                packet.clear();
                if (bhost) {
                    DbHostAuth[] oldHosts = null;
                    if (bhostPurge) {
                        // Need to first delete all entries
                        try {
                            oldHosts = DbHostAuth.deleteAll();
                        } catch (WaarpDatabaseException e) {
                            // ignore
                        }
                    }
                    String filename = shost;
                    if (AuthenticationFileBasedConfiguration.loadAuthentication(
                            Configuration.configuration,
                            filename)) {
                        shost = "Host:OK";
                    } else {
                        logger.error("Error in Load Hosts");
                        shost = "Host:KO";
                        bhost = false;
                    }
                    if (!bhost) {
                        if (oldHosts != null) {
                            for (DbHostAuth dbHost : oldHosts) {
                                try {
                                    if (!dbHost.exist()) {
                                        dbHost.insert();
                                    }
                                } catch (WaarpDatabaseException e1) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                if (brule) {
                    DbRule[] oldRules = null;
                    if (brulePurge) {
                        // Need to first delete all entries
                        try {
                            oldRules = DbRule.deleteAll();
                        } catch (WaarpDatabaseException e) {
                            // ignore
                        }
                    }
                    File file = new File(srule);
                    try {
                        RuleFileBasedConfiguration.getMultipleFromFile(file);
                        srule = "Rule:OK";
                        brule = true;
                    } catch (WaarpDatabaseNoConnectionException e) {
                        logger.error("Error", e);
                        srule = "Rule:KO";
                        brule = false;
                    } catch (WaarpDatabaseSqlException e) {
                        logger.error("Error", e);
                        srule = "Rule:KO";
                        brule = false;
                    } catch (WaarpDatabaseNoDataException e) {
                        logger.error("Error", e);
                        srule = "Rule:KO";
                        brule = false;
                    } catch (WaarpDatabaseException e) {
                        logger.error("Error", e);
                        srule = "Rule:KO";
                        brule = false;
                    }
                    if (!brule) {
                        if (oldRules != null) {
                            for (DbRule dbRule : oldRules) {
                                try {
                                    if (!dbRule.exist()) {
                                        dbRule.insert();
                                    }
                                } catch (WaarpDatabaseException e1) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                R66Result result = null;
                if (brule || bhost) {
                    result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                } else {
                    result = new R66Result(session, true, ErrorCode.TransferError, null);
                }
                // Now answer
                ValidPacket valid = new ValidPacket(shost + " " + srule, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.INFORMATIONPACKET: {
                session.newState(VALIDOTHER);
                // Validate user request
                R66Result resulttest = new R66Result(session, true,
                        ErrorCode.CompleteOk, null);
                resulttest.setOther(packet);
                localChannelReference.validateRequest(resulttest);
                channel.close();
                break;
            }
            case LocalPacketFactory.BANDWIDTHPACKET: {
                String[] splitglobal = packet.getSheader().split(" ");
                String[] splitsession = packet.getSmiddle().split(" ");
                packet.clear();
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                ValidPacket valid = null;
                if (splitglobal.length < 2 || splitsession.length < 2) {
                    // request of current values
                    session.newState(VALIDOTHER);

                    long[] lresult = bandwidth(false, 0, 0, 0, 0);
                    // Now answer
                    valid = new ValidPacket(lresult[0] + " " + lresult[1] +
                            " " + lresult[2] + " " + lresult[3], result.getCode().getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                } else {
                    session.newState(VALIDOTHER);
                    bandwidth(true, Long.parseLong(splitglobal[0]), Long.parseLong(splitglobal[1]),
                            Long.parseLong(splitsession[0]), Long.parseLong(splitsession[1]));
                    // Now answer
                    valid = new ValidPacket("Bandwidth changed", result.getCode().getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                }
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.TESTPACKET: {
                session.newState(VALIDOTHER);
                logger.info("Valid TEST MESSAGE: " + packet.toString());
                R66Result resulttest = new R66Result(session, true,
                        ErrorCode.CompleteOk, null);
                resulttest.setOther(packet);
                localChannelReference.validateRequest(resulttest);
                channel.close();
                break;
            }
            default:
                logger.info("Validation is ignored: " + packet.getTypeValid());
                packet.clear();
        }
    }

    /**
     * Receive a json request
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     * @throws OpenR66ProtocolBusinessException
     * @throws OpenR66ProtocolShutdownException
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolNoDataException
     */
    public void jsonCommand(Channel channel, JsonCommandPacket packet)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66RunnerErrorException, OpenR66ProtocolSystemException,
            OpenR66ProtocolBusinessException, OpenR66ProtocolShutdownException, OpenR66ProtocolPacketException,
            OpenR66ProtocolNoDataException {
        // SHUTDOWNPACKET does not need authentication
        if (packet.getTypeValid() != LocalPacketFactory.SHUTDOWNPACKET &&
                !session.isAuthenticated()) {
            logger.warn("JsonCommand packet received while not authenticated: {} {}", packet, session);
            session.newState(ERROR);
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Valid received");
        }
        JsonPacket json = packet.getJsonRequest();
        if (json == null) {
            ErrorCode code = ErrorCode.CommandNotFound;
            R66Result resulttest = new R66Result(session, true,
                    code, session.getRunner());
            json = new JsonPacket();
            json.setComment("Invalid command");
            json.setRequestUserPacket(packet.getTypeValid());
            JsonCommandPacket valid = new JsonCommandPacket(json, resulttest.getCode().getCode(),
                    LocalPacketFactory.REQUESTUSERPACKET);
            resulttest.setOther(packet);
            localChannelReference.validateRequest(resulttest);
            try {
                ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                        valid, true);
            } catch (OpenR66ProtocolPacketException e) {
            }
            session.setStatus(99);
            channel.close();
            return;
        }
        json.setRequestUserPacket(packet.getTypeValid());
        switch (packet.getTypeValid()) {
            case LocalPacketFactory.SHUTDOWNPACKET: {
                ShutdownRequestJsonPacket node = (ShutdownRequestJsonPacket) json;
                int rank = -1;
                if (session.getRunner() != null &&
                        session.getRunner().isInTransfer()) {
                    rank = node.getRank();
                }
                R66Result result = new R66Result(
                        new OpenR66ProtocolShutdownException(), session, true,
                        ErrorCode.Shutdown, session.getRunner());
                result.setOther(packet);
                rank = shutdownRequest(result, rank);
                if (rank >= 0) {
                    node.setRank(rank);
                    JsonCommandPacket valid = new JsonCommandPacket(node, result.getCode().getCode(),
                            LocalPacketFactory.SHUTDOWNPACKET);
                    try {
                        ChannelUtils.writeAbstractLocalPacket(localChannelReference, valid,
                                true);
                    } catch (OpenR66ProtocolPacketException e) {
                    }
                }
                shutdownLocalChannel(channel);
                break;
            }
            case LocalPacketFactory.BLOCKREQUESTPACKET: {
                ShutdownOrBlockJsonPacket node = (ShutdownOrBlockJsonPacket) json;
                byte[] key = node.getKey();
                if (node.isShutdownOrBlock()) {
                    // Shutdown
                    session.newState(SHUTDOWN);
                    shutdown(key, node.isRestartOrBlock());
                } else {
                    // Block
                    R66Result result = blockRequest(key, node.isRestartOrBlock());
                    node.setComment((node.isRestartOrBlock() ? "Block" : "Unblock") + " new request");
                    JsonCommandPacket valid = new JsonCommandPacket(json, result.getCode().getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                    try {
                        ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                                valid, true);
                    } catch (OpenR66ProtocolPacketException e) {
                    }
                    channel.close();
                }
                break;
            }
            case LocalPacketFactory.BUSINESSREQUESTPACKET: {
                BusinessRequestJsonPacket node = (BusinessRequestJsonPacket) json;
                if (node.isToApplied()) {
                    session.newState(BUSINESSD);
                }
                R66Future future = businessRequest(node.isToApplied(), node.getClassName(), node.getArguments(),
                        node.getExtraArguments(), node.getDelay());
                if (future != null && !future.isSuccess()) {
                    R66Result result = future.getResult();
                    if (result == null) {
                        result = new R66Result(session, false, ErrorCode.ExternalOp, session.getRunner());
                    }
                    logger.info("Task in Error:" + node.getClassName() + " " + result);
                    if (!result.isAnswered()) {
                        node.setValidated(false);
                        session.newState(ERROR);
                        ErrorPacket error = new ErrorPacket(
                                "BusinessRequest in error: for " + node.toString() + " since " +
                                        result.getMessage(),
                                result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
                        ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
                        session.setStatus(203);
                    }
                    session.setStatus(204);
                }
                break;
            }
            case LocalPacketFactory.INFORMATIONPACKET: {
                InformationJsonPacket node = (InformationJsonPacket) json;
                ValidPacket validPacket = null;
                if (node.isIdRequest()) {
                    validPacket = informationRequest(node.getId(), node.isTo(), node.getRulename(), false);
                } else {
                    validPacket = informationFile(node.getRequest(), node.getRulename(), node.getFilename(), false);
                }
                if (validPacket != null) {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            validPacket, true);
                    channel.close();
                } else {
                    session.newState(ERROR);
                    ErrorPacket error = new ErrorPacket("Error while Request " + node,
                            ErrorCode.Internal.getCode(), ErrorPacket.FORWARDCLOSECODE);
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
                    ChannelCloseTimer.closeFutureChannel(channel);
                }
                break;
            }
            case LocalPacketFactory.REQUESTPACKET: {
                TransferRequestJsonPacket node = (TransferRequestJsonPacket) json;
                R66Result result = transferRequest(node);
                if (isCodeValid(result.getCode())) {
                    JsonCommandPacket valid = new JsonCommandPacket(json, result.getCode().getCode(),
                            LocalPacketFactory.REQUESTUSERPACKET);
                    result.setOther(packet);
                    localChannelReference.validateRequest(result);
                    try {
                        ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                                valid, true);
                    } catch (OpenR66ProtocolPacketException e) {
                    }
                    session.setStatus(27);
                    channel.close();
                } else {
                    result.setOther(packet);
                    localChannelReference.invalidateRequest(result);
                    ErrorPacket error = new ErrorPacket(
                            "TransferRequest in error: for " + node.toString() + " since " +
                                    result.getMessage(),
                            result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
                    ChannelCloseTimer.closeFutureChannel(channel);
                }
                break;
            }
            case LocalPacketFactory.STOPPACKET:
            case LocalPacketFactory.CANCELPACKET: {
                StopOrCancelJsonPacket node = (StopOrCancelJsonPacket) json;
                R66Result resulttest;
                if (node.getRequested() == null || node.getRequester() == null
                        || node.getSpecialid() == DbConstant.ILLEGALVALUE) {
                    ErrorCode code = ErrorCode.CommandNotFound;
                    resulttest = new R66Result(session, true,
                            code, session.getRunner());
                } else {
                    String reqd = node.getRequested();
                    String reqr = node.getRequester();
                    long id = node.getSpecialid();
                    session.newState(VALIDOTHER);
                    resulttest = stopOrCancel(packet.getTypeValid(), reqd, reqr, id);
                }
                // inform back the requester
                JsonCommandPacket valid = new JsonCommandPacket(json, resulttest.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                resulttest.setOther(packet);
                localChannelReference.validateRequest(resulttest);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                session.setStatus(27);
                channel.close();
                break;
            }
            case LocalPacketFactory.VALIDPACKET: {
                RestartTransferJsonPacket node = (RestartTransferJsonPacket) json;
                session.newState(VALIDOTHER);
                R66Result result = requestRestart(node.getRequested(), node.getRequester(), node.getSpecialid(),
                        node.getRestarttime());
                result.setOther(packet);
                JsonCommandPacket valid = new JsonCommandPacket(node,
                        result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                if (isCodeValid(result.getCode())) {
                    localChannelReference.validateRequest(result);
                } else {
                    localChannelReference.invalidateRequest(result);
                }
                // inform back the requester
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.REQUESTUSERPACKET: {
                session.newState(VALIDOTHER);
                // Validate user request
                R66Result resulttest = new R66Result(session, true,
                        ErrorCode.getFromCode(packet.getResult()), null);
                resulttest.setOther(packet);
                switch (resulttest.getCode()) {
                    case CompleteOk:
                    case InitOk:
                    case PostProcessingOk:
                    case PreProcessingOk:
                    case QueryAlreadyFinished:
                    case QueryStillRunning:
                    case Running:
                    case TransferOk:
                        break;
                    default:
                        localChannelReference.invalidateRequest(resulttest);
                        session.setStatus(102);
                        channel.close();
                        return;
                }
                localChannelReference.validateRequest(resulttest);
                session.setStatus(28);
                channel.close();
                break;
            }
            case LocalPacketFactory.LOGPACKET:
            case LocalPacketFactory.LOGPURGEPACKET: {
                LogJsonPacket node = (LogJsonPacket) json;
                boolean purge = node.isPurge();
                boolean clean = node.isClean();
                Timestamp start = (node.getStart() == null) ? null :
                        new Timestamp(node.getStart().getTime());
                Timestamp stop = (node.getStop() == null) ? null :
                        new Timestamp(node.getStop().getTime());
                String startid = node.getStartid();
                String stopid = node.getStopid();
                String rule = node.getRule();
                String request = node.getRequest();
                boolean pending = node.isStatuspending();
                boolean transfer = node.isStatustransfer();
                boolean done = node.isStatusdone();
                boolean error = node.isStatuserror();
                boolean isPurge = (packet.getTypeValid() == LocalPacketFactory.LOGPURGEPACKET || purge);
                session.newState(VALIDOTHER);
                String sresult[] = logPurge(purge, clean, start, stop, startid, stopid, rule, request, pending,
                        transfer, done, error, isPurge);
                LogResponseJsonPacket newjson = new LogResponseJsonPacket();
                newjson.fromJson(node);
                // Now answer
                newjson.setCommand(packet.getTypeValid());
                newjson.setFilename(sresult[0]);
                newjson.setExported(Long.parseLong(sresult[1]));
                newjson.setPurged(Long.parseLong(sresult[2]));
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                JsonCommandPacket valid = new JsonCommandPacket(newjson, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.CONFEXPORTPACKET: {
                // host, rule, business, alias, roles
                ConfigExportJsonPacket node = (ConfigExportJsonPacket) json;
                boolean bhost = node.isHost();
                boolean brule = node.isRule();
                boolean bbusiness = node.isBusiness();
                boolean balias = node.isAlias();
                boolean broles = node.isRoles();
                session.newState(VALIDOTHER);
                String sresult[] = configExport(bhost, brule, bbusiness, balias, broles);
                // Now answer
                ConfigExportResponseJsonPacket resp = new ConfigExportResponseJsonPacket();
                resp.fromJson(node);
                resp.setFilehost(sresult[0]);
                resp.setFilerule(sresult[1]);
                resp.setFilebusiness(sresult[2]);
                resp.setFilealias(sresult[3]);
                resp.setFileroles(sresult[4]);
                R66Result result = null;
                if (resp.getFilerule() != null || resp.getFilehost() != null ||
                        resp.getFilebusiness() != null || resp.getFilealias() != null ||
                        resp.getFileroles() != null) {
                    result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                } else {
                    result = new R66Result(session, true, ErrorCode.TransferError, null);
                }
                JsonCommandPacket valid = new JsonCommandPacket(resp, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.CONFIMPORTPACKET: {
                ConfigImportResponseJsonPacket resp = configImport((ConfigImportJsonPacket) json);
                R66Result result = null;
                if (resp.isImportedhost() || resp.isImportedrule() ||
                        resp.isImportedbusiness() || resp.isImportedalias() ||
                        resp.isImportedroles()) {
                    result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                } else {
                    result = new R66Result(session, true, ErrorCode.TransferError, null);
                }
                JsonCommandPacket valid = new JsonCommandPacket(resp, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                logger.debug(valid.getRequest());
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.BANDWIDTHPACKET: {
                // setter, writeglobal, readglobal, writesession, readsession
                BandwidthJsonPacket node = (BandwidthJsonPacket) json;
                boolean setter = node.isSetter();
                // request of current values or set new values
                session.newState(VALIDOTHER);
                long[] lresult = bandwidth(setter,
                        node.getWriteglobal(), node.getReadglobal(),
                        node.getWritesession(), node.getReadsession());
                // Now answer
                node.setWriteglobal(lresult[0]);
                node.setReadglobal(lresult[1]);
                node.setWritesession(lresult[2]);
                node.setReadsession(lresult[3]);
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                JsonCommandPacket valid = new JsonCommandPacket(node, result.getCode().getCode(),
                        LocalPacketFactory.REQUESTUSERPACKET);
                localChannelReference.validateRequest(result);
                try {
                    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                            valid, true);
                } catch (OpenR66ProtocolPacketException e) {
                }
                channel.close();
                break;
            }
            case LocalPacketFactory.TESTPACKET: {
                session.newState(VALIDOTHER);
                logger.info("Valid TEST MESSAGE: " + packet.toString());
                R66Result resulttest = new R66Result(session, true,
                        ErrorCode.CompleteOk, null);
                resulttest.setOther(packet);
                localChannelReference.validateRequest(resulttest);
                channel.close();
                break;
            }
            default:
                logger.info("Validation is ignored: " + packet.getTypeValid());
        }
    }

    /**
     * Shutdown Local Channel after the request is shutdown
     * 
     * @param channel
     */
    private final void shutdownLocalChannel(Channel channel) {
        session.setStatus(26);
        logger.warn("Will Close Local from Network Channel since Remote shutdown received");
        ChannelCloseTimer.closeFutureChannel(channel);
        try {
            Thread.sleep(Configuration.WAITFORNETOP * 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        NetworkChannelReference ncr = localChannelReference.getNetworkChannelObject();
        NetworkTransaction.shuttingDownNetworkChannel(ncr);
        NetworkTransaction.shuttingdownNetworkChannelsPerHostID(ncr.getHostId());
    }

    /**
     * Shutdown the current request with an optional rank to set for future restart
     * 
     * @param result
     *            the result to be associated in finalization
     * @param rank
     *            the future rank to set if restart (<0 if none)
     * @return the rank to set for future restart if any (< 0 if none)
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     */
    private final int shutdownRequest(R66Result result, int rank)
            throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
        session.newState(SHUTDOWN);
        logger.warn("Shutdown received so Will close channel" +
                localChannelReference.toString());
        if (session.getRunner() != null &&
                session.getRunner().isInTransfer()) {
            DbTaskRunner runner = session.getRunner();
            if (rank >= 0) {
                // Save last rank from remote point of view
                runner.setRankAtStartup(rank);
                session.setFinalizeTransfer(false, result);
            } else if (!runner.isSender()) {
                // is receiver so informs back for the rank to use next time
                int newrank = runner.getRank();
                try {
                    runner.saveStatus();
                } catch (OpenR66RunnerErrorException e) {
                }
                session.setFinalizeTransfer(false, result);
                return newrank;
            } else {
                session.setFinalizeTransfer(false, result);
            }
        } else {
            session.setFinalizeTransfer(false, result);
        }
        return -1;
    }

    /**
     * Get or Set the bandwidth configuration
     * 
     * @param setter
     * @param writeglobal
     * @param readglobal
     * @param writesession
     * @param readsession
     * @return the 4 current values for the bandwidth (in the same order)
     * @throws OpenR66ProtocolNotAuthenticatedException
     */
    public final long[] bandwidth(boolean setter,
            long writeglobal, long readglobal,
            long writesession, long readsession)
            throws OpenR66ProtocolNotAuthenticatedException {
        // Authentication must be the local server or LIMIT authorization
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl()))
                    && !session.getAuth().isValidRole(ROLE.LIMIT)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        if (!setter) {
            // request of current values
            // Now answer
            return new long[] { Configuration.configuration.getServerGlobalWriteLimit(),
                    Configuration.configuration.getServerGlobalReadLimit(),
                    Configuration.configuration.getServerChannelWriteLimit(),
                    Configuration.configuration.getServerChannelReadLimit() };
        } else {
            long wgl = (writeglobal / 10) * 10;
            long rgl = (readglobal / 10) * 10;
            long wsl = (writesession / 10) * 10;
            long rsl = (readsession / 10) * 10;
            if (wgl < 0) {
                wgl = Configuration.configuration.getServerGlobalWriteLimit();
            }
            if (rgl < 0) {
                rgl = Configuration.configuration.getServerGlobalReadLimit();
            }
            if (wsl < 0) {
                wsl = Configuration.configuration.getServerChannelWriteLimit();
            }
            if (rsl < 0) {
                rsl = Configuration.configuration.getServerChannelReadLimit();
            }
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyWarning(
                        "Change Bandwidth Limit Order received: Global " +
                                wgl + ":" + rgl + " (W:R) Local " + wsl + ":" + rsl + " (W:R)",
                        session.getAuth().getUser());
            }
            Configuration.configuration.changeNetworkLimit(wgl, rgl, wsl, rsl,
                    Configuration.configuration.getDelayLimit());
            // Now answer
            return new long[] { Configuration.configuration.getServerGlobalWriteLimit(),
                    Configuration.configuration.getServerGlobalReadLimit(),
                    Configuration.configuration.getServerChannelWriteLimit(),
                    Configuration.configuration.getServerChannelReadLimit() };
        }
    }

    /**
     * Import configuration from files as parameter
     * 
     * @param json
     * @return the packet to answer
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolSystemException
     */
    public final ConfigImportResponseJsonPacket configImport(ConfigImportJsonPacket json)
            throws OpenR66ProtocolNotAuthenticatedException, OpenR66ProtocolSystemException {
        session.newState(VALIDOTHER);
        // Authentication must be the local server or CONFIGADMIN authorization
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                    !session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        if (Configuration.configuration.getR66Mib() != null) {
            Configuration.configuration.getR66Mib().notifyWarning(
                    "Import Configuration Order received", session.getAuth().getUser());
        }
        //purgehost, purgerule, purgebusiness, purgealias, purgeroles, host, rule, business, alias, roles
        ConfigImportJsonPacket node = json;
        boolean bhostPurge = node.isPurgehost();
        boolean brulePurge = node.isPurgerule();
        boolean bbusinessPurge = node.isPurgebusiness();
        boolean baliasPurge = node.isPurgealias();
        boolean brolesPurge = node.isPurgeroles();
        boolean importedhost = false, importedrule = false, importedbusiness = false, importedalias = false, importedroles = false;
        String shost = node.getHost();
        String srule = node.getRule();
        String sbusiness = node.getBusiness();
        String salias = node.getAlias();
        String sroles = node.getRoles();
        long hostid = node.getHostid();
        long ruleid = node.getRuleid();
        long businessid = node.getBusinessid();
        long aliasid = node.getAliasid();
        long roleid = node.getRolesid();

        DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                : DbConstant.admin.getSession();

        String remote = session.getAuth().getUser();
        String local = null;
        try {
            local = Configuration.configuration.getHostId(session.getAuth().isSsl());
        } catch (OpenR66ProtocolNoSslException e1) {
            logger.warn("Local Ssl Host is unknown", e1);
        }
        if (shost != null || (hostid != DbConstant.ILLEGALVALUE && local != null)) {
            DbHostAuth[] oldHosts = null;
            DbTaskRunner runner = null;
            if (hostid != DbConstant.ILLEGALVALUE && local != null) {
                // need to find the local filename
                try {
                    runner = new DbTaskRunner(session, null,
                            hostid, remote, local);
                    shost = runner.getFullFilePath();
                } catch (WaarpDatabaseException e) {
                    logger.error("RunnerTask is not found: " + hostid, e);
                    shost = null;
                } catch (CommandAbstractException e) {
                    logger.error("File is not found: " + hostid, e);
                    shost = null;
                }
            }
            if (shost != null) {
                if (bhostPurge) {
                    // Need to first delete all entries
                    try {
                        oldHosts = DbHostAuth.deleteAll();
                    } catch (WaarpDatabaseException e) {
                        // ignore
                    }
                }
                if (AuthenticationFileBasedConfiguration.loadAuthentication(
                        Configuration.configuration, shost)) {
                    importedhost = true;
                    logger.debug("Host configuration imported from " + shost);
                } else {
                    logger.error("Error in Load Hosts");
                    importedhost = false;
                }
                if (!importedhost && bhostPurge) {
                    if (oldHosts != null) {
                        for (DbHostAuth dbHost : oldHosts) {
                            try {
                                if (!dbHost.exist()) {
                                    dbHost.insert();
                                }
                            } catch (WaarpDatabaseException e1) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        if (srule != null || (ruleid != DbConstant.ILLEGALVALUE && local != null)) {
            DbRule[] oldRules = null;
            DbTaskRunner runner = null;
            if (ruleid != DbConstant.ILLEGALVALUE && local != null) {
                // need to find the local filename
                try {
                    runner = new DbTaskRunner(session, null,
                            ruleid, remote, local);
                    srule = runner.getFullFilePath();
                } catch (WaarpDatabaseException e) {
                    logger.error("RunnerTask is not found: " + ruleid, e);
                    srule = null;
                } catch (CommandAbstractException e) {
                    logger.error("File is not found: " + hostid, e);
                    srule = null;
                }
            }
            if (srule != null) {
                if (brulePurge) {
                    // Need to first delete all entries
                    try {
                        oldRules = DbRule.deleteAll();
                    } catch (WaarpDatabaseException e) {
                        // ignore
                    }
                }
                File file = new File(srule);
                try {
                    RuleFileBasedConfiguration.getMultipleFromFile(file);
                    importedrule = true;
                    logger.debug("Rule configuration imported from " + srule);
                } catch (WaarpDatabaseNoConnectionException e) {
                    logger.error("Error", e);
                    importedrule = false;
                } catch (WaarpDatabaseSqlException e) {
                    logger.error("Error", e);
                    importedrule = false;
                } catch (WaarpDatabaseNoDataException e) {
                    logger.error("Error", e);
                    importedrule = false;
                } catch (WaarpDatabaseException e) {
                    logger.error("Error", e);
                    importedrule = false;
                }
                if (!importedrule && brulePurge) {
                    if (oldRules != null) {
                        for (DbRule dbRule : oldRules) {
                            try {
                                if (!dbRule.exist()) {
                                    dbRule.insert();
                                }
                            } catch (WaarpDatabaseException e1) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        // load from file ! not from filename ! Moreover: filename might be incorrect => Must get the remote filename (recv)
        if (sbusiness != null || salias != null || sroles != null || bbusinessPurge || baliasPurge || brolesPurge
                || ((businessid != DbConstant.ILLEGALVALUE || aliasid != DbConstant.ILLEGALVALUE ||
                roleid != DbConstant.ILLEGALVALUE) && local != null)) {
            DbHostConfiguration host = null;
            try {
                host = new DbHostConfiguration(Configuration.configuration.getHOST_ID());
                DbTaskRunner runner = null;
                if (businessid != DbConstant.ILLEGALVALUE && local != null) {
                    // need to find the local filename
                    try {
                        runner = new DbTaskRunner(session, null, businessid,
                                remote, local);
                        sbusiness = runner.getFullFilePath();
                    } catch (WaarpDatabaseException e) {
                        logger.error("RunnerTask is not found: " + businessid, e);
                        sbusiness = null;
                    } catch (CommandAbstractException e) {
                        logger.error("File is not found: " + hostid, e);
                        sbusiness = null;
                    }
                }
                if (sbusiness != null) {
                    try {
                        String content = WaarpStringUtils.readFileException(sbusiness);
                        importedbusiness = host.updateBusiness(Configuration.configuration, content, bbusinessPurge);
                        logger.debug("Business configuration imported from " + sbusiness + "(" + importedbusiness + ")");
                    } catch (InvalidArgumentException e) {
                        logger.error("Error", e);
                        importedbusiness = false;
                    } catch (FileTransferException e) {
                        logger.error("Error", e);
                        importedbusiness = false;
                    }
                }
                if (aliasid != DbConstant.ILLEGALVALUE && local != null) {
                    // need to find the local filename
                    try {
                        runner = new DbTaskRunner(session, null, aliasid,
                                remote, local);
                        salias = runner.getFullFilePath();
                    } catch (WaarpDatabaseException e) {
                        logger.error("RunnerTask is not found: " + aliasid, e);
                        salias = null;
                    } catch (CommandAbstractException e) {
                        logger.error("File is not found: " + hostid, e);
                        salias = null;
                    }
                }
                if (salias != null) {
                    try {
                        String content = WaarpStringUtils.readFileException(salias);
                        importedalias = host.updateAlias(Configuration.configuration, content, baliasPurge);
                        logger.debug("Alias configuration imported from " + salias + "(" + importedalias + ")");
                    } catch (InvalidArgumentException e) {
                        logger.error("Error", e);
                        importedalias = false;
                    } catch (FileTransferException e) {
                        logger.error("Error", e);
                        importedalias = false;
                    }
                }
                if (roleid != DbConstant.ILLEGALVALUE && local != null) {
                    // need to find the local filename
                    try {
                        runner = new DbTaskRunner(session, null, roleid,
                                remote, local);
                        sroles = runner.getFullFilePath();
                    } catch (WaarpDatabaseException e) {
                        logger.error("RunnerTask is not found: " + roleid, e);
                        sroles = null;
                    } catch (CommandAbstractException e) {
                        logger.error("File is not found: " + hostid, e);
                        sroles = null;
                    }
                }
                if (sroles != null) {
                    try {
                        String content = WaarpStringUtils.readFileException(sroles);
                        importedroles = host.updateRoles(Configuration.configuration, content, brolesPurge);
                        logger.debug("Roles configuration imported from " + sroles + "(" + importedroles + ")");
                    } catch (InvalidArgumentException e) {
                        logger.error("Error", e);
                        importedroles = false;
                    } catch (FileTransferException e) {
                        logger.error("Error", e);
                        importedroles = false;
                    }
                }
            } catch (WaarpDatabaseException e1) {
                logger.error("Error while trying to open: " + sbusiness, e1);
                importedbusiness = false;
                importedalias = false;
                importedroles = false;
            }
        }
        // Now answer
        ConfigImportResponseJsonPacket resp = new ConfigImportResponseJsonPacket();
        resp.fromJson(node);
        if (bhostPurge || shost != null) {
            resp.setPurgedhost(bhostPurge);
            resp.setImportedhost(importedhost);
        }
        if (brulePurge || srule != null) {
            resp.setPurgedrule(brulePurge);
            resp.setImportedrule(importedrule);
        }
        if (bbusinessPurge || sbusiness != null) {
            resp.setPurgedbusiness(bbusinessPurge);
            resp.setImportedbusiness(importedbusiness);
        }
        if (baliasPurge || salias != null) {
            resp.setPurgedalias(baliasPurge);
            resp.setImportedalias(importedalias);
        }
        if (brolesPurge || sroles != null) {
            resp.setPurgedroles(brolesPurge);
            resp.setImportedroles(importedroles);
        }
        return resp;
    }

    /**
     * Export configuration and return filenames in order
     * 
     * @param bhost
     * @param brule
     * @param bbusiness
     * @param balias
     * @param broles
     * @return filenames in order
     * @throws OpenR66ProtocolNotAuthenticatedException
     */
    public final String[] configExport(boolean bhost, boolean brule,
            boolean bbusiness, boolean balias, boolean broles)
            throws OpenR66ProtocolNotAuthenticatedException {
        // Authentication must be the local server or CONFIGADMIN authorization
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                    !session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        if (Configuration.configuration.getR66Mib() != null) {
            Configuration.configuration.getR66Mib().notifyWarning(
                    "Export Configuration Order received", session.getAuth().getUser());
        }
        String dir = Configuration.configuration.getBaseDirectory() +
                Configuration.configuration.getArchivePath();
        DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                : DbConstant.admin.getSession();
        return staticConfigExport(dbSession, dir, bhost, brule, bbusiness, balias, broles);
    }

    /**
     * Export configuration and return filenames in order
     * 
     * @param dbSession
     * @param dir
     * @param bhost
     * @param brule
     * @param bbusiness
     * @param balias
     * @param broles
     * @return filenames in order
     */
    public static String[] staticConfigExport(DbSession dbSession, String dir, boolean bhost, boolean brule,
            boolean bbusiness, boolean balias, boolean broles) {
        String shost = null, srule = null, sbusiness = null, salias = null, sroles = null;
        String hostname = Configuration.configuration.getHOST_ID();
        if (bhost) {
            String filename = dir + File.separator + hostname + "_Authentications.xml";
            try {
                AuthenticationFileBasedConfiguration.writeXML(Configuration.configuration,
                        filename);
                shost = filename;
            } catch (WaarpDatabaseNoConnectionException e) {
                logger.error("Error", e);
                shost = null;
                bhost = false;
            } catch (WaarpDatabaseSqlException e) {
                logger.error("Error", e);
                shost = null;
                bhost = false;
            } catch (OpenR66ProtocolSystemException e) {
                logger.error("Error", e);
                shost = null;
                bhost = false;
            }
        }
        if (brule) {
            try {
                srule = RuleFileBasedConfiguration.writeOneXml(dir, hostname);
            } catch (WaarpDatabaseNoConnectionException e1) {
                logger.error("Error", e1);
                srule = null;
                brule = false;
            } catch (WaarpDatabaseSqlException e1) {
                logger.error("Error", e1);
                srule = null;
                brule = false;
            } catch (OpenR66ProtocolSystemException e1) {
                logger.error("Error", e1);
                srule = null;
                brule = false;
            }
        }
        if (bbusiness || balias || broles) {
            try {
                DbHostConfiguration host = new DbHostConfiguration(
                        Configuration.configuration.getHOST_ID());
                if (bbusiness) {
                    sbusiness = host.getBusiness();
                    if (sbusiness != null) {
                        String filename = dir + File.separator + hostname + "_Business.xml";
                        FileOutputStream outputStream = new FileOutputStream(filename);
                        outputStream.write(sbusiness.getBytes());
                        outputStream.flush();
                        outputStream.close();
                        sbusiness = filename;
                    }
                    bbusiness = (sbusiness != null);
                }
                if (balias) {
                    salias = host.getAliases();
                    if (salias != null) {
                        String filename = dir + File.separator + hostname + "_Aliases.xml";
                        FileOutputStream outputStream = new FileOutputStream(filename);
                        outputStream.write(salias.getBytes());
                        outputStream.flush();
                        outputStream.close();
                        salias = filename;
                    }
                    balias = (salias != null);
                }
                if (broles) {
                    sroles = host.getRoles();
                    if (sroles != null) {
                        String filename = dir + File.separator + hostname + "_Roles.xml";
                        FileOutputStream outputStream = new FileOutputStream(filename);
                        outputStream.write(sroles.getBytes());
                        outputStream.flush();
                        outputStream.close();
                        sroles = filename;
                    }
                    broles = (sroles != null);
                }
            } catch (WaarpDatabaseNoConnectionException e1) {
                logger.error("Error", e1);
                bbusiness = (sbusiness != null);
                balias = (salias != null);
                broles = (sroles != null);
            } catch (WaarpDatabaseSqlException e1) {
                logger.error("Error", e1);
                bbusiness = (sbusiness != null);
                balias = (salias != null);
                broles = (sroles != null);
            } catch (WaarpDatabaseException e) {
                logger.error("Error", e);
                bbusiness = (sbusiness != null);
                balias = (salias != null);
                broles = (sroles != null);
            } catch (IOException e) {
                logger.error("Error", e);
                bbusiness = (sbusiness != null);
                balias = (salias != null);
                broles = (sroles != null);
            }
        }
        // Now answer
        return new String[] { shost, srule, sbusiness, salias, sroles };
    }

    /**
     * Request to restart a transfer
     * 
     * @param reqd
     *            requested
     * @param reqr
     *            requester
     * @param id
     *            id of the Transfer
     * @param date
     *            time start if any
     * @return the Result including the error code to use in return
     * @throws OpenR66ProtocolNotAuthenticatedException
     */
    public final R66Result requestRestart(String reqd, String reqr, long id, Date date)
            throws OpenR66ProtocolNotAuthenticatedException {
        ErrorCode returnCode = ErrorCode.Internal;
        R66Result resulttest = null;
        // should be from the local server or from an authorized hosts: TRANSFER
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                    !session.getAuth().isValidRole(ROLE.TRANSFER)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        // Try to validate a restarting transfer
        // validLimit on requested side
        if (Configuration.configuration.getConstraintLimitHandler().checkConstraints()) {
            logger.error("Limit exceeded {} while asking to relaunch a task"
                    + reqd + ":" + reqr + ":" + id, Configuration.configuration.getConstraintLimitHandler().lastAlert);
            session.setStatus(100);
            returnCode = ErrorCode.ServerOverloaded;
            resulttest = new R66Result(null, session, true,
                    returnCode, null);
        } else {
            // Try to validate a restarting transfer
            // header = ?; middle = requested+blank+requester+blank+specialId
            // note: might contains one more argument = time to reschedule in yyyyMMddHHmmss format
            if (reqd == null || reqr == null || id == DbConstant.ILLEGALVALUE) {
                // not enough args
                returnCode = ErrorCode.IncorrectCommand;
                resulttest = new R66Result(new OpenR66ProtocolBusinessRemoteFileNotFoundException(
                        "Not enough arguments"), session, true,
                        returnCode, null);
            } else {
                DbTaskRunner taskRunner = null;
                try {
                    DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                            : DbConstant.admin.getSession();
                    taskRunner = new DbTaskRunner(session,null, id, reqr, reqd);
                    Timestamp timestart = null;
                    if (date != null) {
                        // time to reschedule in yyyyMMddHHmmss format
                        logger.debug("Debug: restart with " + date);
                        timestart = new Timestamp(date.getTime());
                        taskRunner.setStart(timestart);
                    }
                    LocalChannelReference lcr =
                            Configuration.configuration.getLocalTransaction().
                                    getFromRequest(reqd + " " + reqr + " " + id);
                    // since it comes from a request transfer, cannot redo it
                    logger.info("Will try to restart: " + taskRunner.toShortString());
                    resulttest = TransferUtils.restartTransfer(taskRunner, lcr);
                    returnCode = resulttest.getCode();
                } catch (WaarpDatabaseException e1) {
                    returnCode = ErrorCode.Internal;
                    resulttest = new R66Result(new OpenR66DatabaseGlobalException(e1),
                            session, true,
                            returnCode, taskRunner);
                }
            }
        }
        return resulttest;
    }

    /**
     * 
     * @param code
     * @return True if the code is an OK code and not an error
     */
    public final boolean isCodeValid(ErrorCode code) {
        switch (code) {
            case BadAuthent:
            case CanceledTransfer:
            case CommandNotFound:
            case ConnectionImpossible:
            case Disconnection:
            case ExternalOp:
            case FileNotAllowed:
            case FileNotFound:
            case FinalOp:
            case IncorrectCommand:
            case Internal:
            case LoopSelfRequestedHost:
            case MD5Error:
            case NotKnownHost:
            case PassThroughMode:
            case QueryRemotelyUnknown:
            case RemoteError:
            case RemoteShutdown:
            case ServerOverloaded:
            case Shutdown:
            case SizeNotAllowed:
            case StoppedTransfer:
            case TransferError:
            case Unimplemented:
            case Unknown:
            case Warning:
                return false;
            case CompleteOk:
            case InitOk:
            case PostProcessingOk:
            case PreProcessingOk:
            case QueryAlreadyFinished:
            case QueryStillRunning:
            case Running:
            case TransferOk:
                return true;
            default:
                return false;
        }
    }

    /**
     * Purge the logs as required
     * 
     * @param purge
     * @param clean
     * @param start
     * @param stop
     * @param startid
     * @param stopid
     * @param rule
     * @param request
     * @param pending
     * @param transfer
     * @param done
     * @param error
     * @param isPurge
     * @return an array of Strings as: filename, nb of exported, nb of purged
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolBusinessException
     */
    public final String[] logPurge(boolean purge, boolean clean,
            Timestamp start, Timestamp stop, String startid, String stopid, String rule, String request,
            boolean pending, boolean transfer, boolean done, boolean error, boolean isPurge)
            throws OpenR66ProtocolNotAuthenticatedException, OpenR66ProtocolBusinessException {
        // should be from the local server or from an authorized hosts: LOGCONTROL
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                    !session.getAuth().isValidRole(ROLE.LOGCONTROL)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                : DbConstant.admin.getSession();
        // first clean if ask
        if (clean) {
            // Update all UpdatedInfo to DONE
            // where GlobalLastStep = ALLDONETASK and status = CompleteOk
            try {
                DbTaskRunner.changeFinishedToDone();
            } catch (WaarpDatabaseNoConnectionException e) {
                logger.warn("Clean cannot be done {}", e.getMessage());
            }
        }
        // create export of log and optionally purge them from database
        String filename = Configuration.configuration.getBaseDirectory() +
                Configuration.configuration.getArchivePath() + R66Dir.SEPARATOR +
                Configuration.configuration.getHOST_ID() + "_" + System.currentTimeMillis() +
                "_runners.xml";
        NbAndSpecialId nb = null;
        DbPreparedStatement getValid = null;
        try {
            getValid =
                    DbTaskRunner.getFilterPrepareStatement(dbSession, 0,// 0 means no limit
                            true, startid, stopid, start, stop, rule, request,
                            pending, transfer, error, done, false);
            nb = DbTaskRunner.writeXMLWriter(getValid, filename);
        } catch (WaarpDatabaseNoConnectionException e1) {
            throw new OpenR66ProtocolBusinessException(e1);
        } catch (WaarpDatabaseSqlException e1) {
            throw new OpenR66ProtocolBusinessException(e1);
        } finally {
            if (getValid != null) {
                getValid.realClose();
            }
        }

        // in case of purge
        int npurge = 0;
        if (nb != null && nb.nb > 0 && isPurge) {
            // purge in same interval all runners with globallaststep
            // as ALLDONETASK or ERRORTASK
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyWarning(
                        "Purge Log Order received", session.getAuth().getUser());
            }
            try {
                if (stopid != null) {
                    long newstopid = Long.parseLong(stopid);
                    if (nb.higherSpecialId < newstopid) {
                        stopid = Long.toString(nb.higherSpecialId);
                    }
                } else {
                    stopid = Long.toString(nb.higherSpecialId);
                }
                // not pending or in transfer
                npurge =
                        DbTaskRunner.purgeLogPrepareStatement(dbSession,
                                startid, stopid, start, stop, rule, request,
                                false, false, error, done, false);
            } catch (WaarpDatabaseNoConnectionException e) {
                throw new OpenR66ProtocolBusinessException(e);
            } catch (WaarpDatabaseSqlException e) {
                throw new OpenR66ProtocolBusinessException(e);
            }
        }
        return new String[] { filename, Long.toString(nb.nb), Long.toString(npurge) };
    }

    /**
     * Stop or Cancel a transfer
     * 
     * @param type
     * @param reqd
     * @param reqr
     * @param id
     * @return the Result to answer
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @deprecated use stopTransfer or cancel transfer instead
     */
    @Deprecated
    public final R66Result stopOrCancel(byte type, String reqd, String reqr, long id)
            throws OpenR66ProtocolNotAuthenticatedException {
        // should be from the local server or from an authorized hosts: SYSTEM
        try {
            if (!session.getAuth().getUser().equals(
                    Configuration.configuration.getHostId(session.getAuth().isSsl())) &&
                    !session.getAuth().isValidRole(ROLE.SYSTEM)) {
                throw new OpenR66ProtocolNotAuthenticatedException(
                        "Not correctly authenticated");
            }
        } catch (OpenR66ProtocolNoSslException e1) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not correctly authenticated since SSL is not supported", e1);
        }
        R66Result resulttest;
        String key = reqd + " " + reqr + " " + id;
        // header = ?; middle = requested+blank+requester+blank+specialId
        LocalChannelReference lcr =
                Configuration.configuration.getLocalTransaction().
                        getFromRequest(key);
        // stop the current transfer
        ErrorCode code = (type == LocalPacketFactory.STOPPACKET) ?
                ErrorCode.StoppedTransfer : ErrorCode.CanceledTransfer;
        if (lcr != null) {
            int rank = 0;
            if (code == ErrorCode.StoppedTransfer && lcr.getSession() != null) {
                DbTaskRunner taskRunner = lcr.getSession().getRunner();
                if (taskRunner != null) {
                    rank = taskRunner.getRank();
                }
            }
            session.newState(ERROR);
            ErrorPacket error = new ErrorPacket(code.name() + " " + rank,
                    code.getCode(), ErrorPacket.FORWARDCLOSECODE);
            try {
                // XXX ChannelUtils.writeAbstractLocalPacket(lcr, error);
                // inform local instead of remote
                ChannelUtils.writeAbstractLocalPacketToLocal(lcr, error);
            } catch (Exception e) {
                logger.warn("Write local packet error", e);
            }
            resulttest = new R66Result(session, true,
                    ErrorCode.CompleteOk, session.getRunner());
        } else {
            // Transfer is not running
            // but maybe need action on database
            if (stopOrCancelRunner(id, reqd, reqr, code)) {
                resulttest = new R66Result(session, true,
                        ErrorCode.CompleteOk, session.getRunner());
            } else {
                resulttest = new R66Result(session, true,
                        ErrorCode.TransferOk, session.getRunner());
            }
        }
        return resulttest;
    }

    private LocalChannelReference getLocalChannelReference(Transfer transfer) {
        String key = transfer.getRequested() + " " + transfer.getRequester()
                + " " + transfer.getId();
        return Configuration.configuration.getLocalTransaction().
                getFromRequest(key);
    }

    /**
     * @param transfer the transfer to stop
     * @return
     */
    public R66Result stopTransfer(Transfer transfer) {
	    ErrorCode code = ErrorCode.StoppedTransfer;
        LocalChannelReference lcr = getLocalChannelReference(transfer);
        if (lcr == null) {
            // Transfer is not running
            transfer.setUpdatedInfo(UpdatedInfo.INERROR);
            transfer.setTransferInfo(code.getCode());
            return new R66Result(session, true,
                    ErrorCode.CompleteOk, session.getRunner());
        }
        ErrorPacket error = new ErrorPacket(
                code.name() + " " + transfer.getRank(),
                code.getCode(), ErrorPacket.FORWARDCLOSECODE);
        try {
            ChannelUtils.writeAbstractLocalPacketToLocal(lcr, error);
        } catch (OpenR66ProtocolPacketException e) {
            logger.error("Cannot stop transfer (" + transfer + ")", e);
            return new R66Result(session, true,
                    ErrorCode.TransferOk, session.getRunner());
        }
        // Update session and transfer status
        session.setErrorState();
        transfer.setTransferInfo(code.getCode());
        return new R66Result(session, true,
                ErrorCode.CompleteOk, session.getRunner());
    }

    /**
     *
     * @param transfer the transfer to stop
     * @return
     */
    public R66Result cancelTransfer(Transfer transfer) {
        ErrorCode code = ErrorCode.CanceledTransfer;
        LocalChannelReference lcr = getLocalChannelReference(transfer);
        if (lcr == null) {
            // Transfer is not running
            transfer.setUpdatedInfo(UpdatedInfo.INERROR);
            transfer.setTransferInfo(code.getCode());
            return new R66Result(session, true,
                    ErrorCode.CompleteOk, session.getRunner());
        }
        ErrorPacket error = new ErrorPacket(
                code.name() + " " + transfer.getRank(),
                code.getCode(), ErrorPacket.FORWARDCLOSECODE);
        try {
            ChannelUtils.writeAbstractLocalPacketToLocal(lcr, error);
        } catch (OpenR66ProtocolPacketException e) {
            logger.error("Cannot cancel transfer (" + transfer + ")", e);
            return new R66Result(session, true,
                    ErrorCode.TransferOk, session.getRunner());
        }
        // Update session and transfer status
        session.setErrorState();
        transfer.setTransferInfo(code.getCode());
        return new R66Result(session, true,
                ErrorCode.CompleteOk, session.getRunner());
    }

    /**
     * Stop or Cancel a Runner
     * 
     * @param id
     * @param reqd
     * @param reqr
     * @param code
     * @return True if correctly stopped or canceled
     */
    private final boolean stopOrCancelRunner(long id, String reqd, String reqr, ErrorCode code) {
        try {
            DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                    : DbConstant.admin.getSession();
            DbTaskRunner taskRunner =
                    new DbTaskRunner(session, null, id, reqr, reqd);
            return taskRunner.stopOrCancelRunner(code);
        } catch (WaarpDatabaseException e) {
        }
        return false;
    }

    /**
     * Receive a Shutdown request
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolShutdownException
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolBusinessException
     */
    public void shutdown(Channel channel, ShutdownPacket packet)
            throws OpenR66ProtocolShutdownException,
            OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolBusinessException {
        session.newState(SHUTDOWN);
        shutdown(packet.getKey(), packet.isRestart());
        packet.clear();
    }

    /**
     * Receive a Shutdown request
     * 
     * @param key
     * @param isRestart
     * @throws OpenR66ProtocolShutdownException
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolBusinessException
     */
    public final void shutdown(byte[] key, boolean isRestart)
            throws OpenR66ProtocolShutdownException,
            OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolBusinessException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Shutdown received");
        }
        // SYSTEM authorization
        boolean isAdmin = session.getAuth().isValidRole(ROLE.SYSTEM);
        boolean isKeyValid = Configuration.configuration.isKeyValid(key);
        if (isAdmin && isKeyValid) {
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyStartStop(
                        "Shutdown Order received effective in " +
                                Configuration.configuration.getTIMEOUTCON() + " ms",
                        session.getAuth().getUser());
            }
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                logger.warn("R66 started as a service, Windows Services might not shown it as stopped");
            }
            if (isRestart) {
                R66ShutdownHook.setRestart(true);
                logger.warn("Server will shutdown and restart");
            }
            throw new OpenR66ProtocolShutdownException("Shutdown Type received");
        }
        logger.error("Invalid Shutdown command: from " + session.getAuth().getUser()
                + " AdmValid: " + isAdmin + " KeyValid: " + isKeyValid);
        throw new OpenR66ProtocolBusinessException("Invalid Shutdown comand");
    }

    /**
     * Business Request (channel should stay open)
     * 
     * Note: the thread called should manage all writeback informations, as well as status, channel
     * closing if needed or not.
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolPacketException
     */
    public void businessRequest(Channel channel, BusinessRequestPacket packet)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolPacketException {
        String argRule = packet.getSheader();
        if (packet.isToValidate()) {
            session.newState(BUSINESSD);
        }
        R66Future future = businessRequest(packet.isToValidate(), argRule, null, null, packet.getDelay());
        if (future != null && !future.isSuccess()) {
            R66Result result = future.getResult();
            if (result == null) {
                result = new R66Result(session, false, ErrorCode.ExternalOp, session.getRunner());
            }
            logger.info("Task in Error:" + argRule + " " + result);
            if (!result.isAnswered()) {
                packet.invalidate();
                session.newState(ERROR);
                ErrorPacket error = new ErrorPacket(
                        "BusinessRequest in error: for " + packet.toString() + " since " +
                                result.getMessage(),
                        result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
                ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
                session.setStatus(203);
            }
            session.setStatus(204);
        }
        packet.clear();
    }

    /**
     * Business Request (channel should stay open)
     * 
     * Note: the thread called should manage all writeback informations, as well as status, channel
     * closing if needed or not.
     * 
     * @param isToApplied
     *            True means this is an action request, False it is the feedback
     * @param className
     * @param arguments
     * @param extraArguments
     * @param delay
     * @return future of the execution
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolPacketException
     */
    public final R66Future businessRequest(boolean isToApplied, String className, String arguments,
            String extraArguments, int delay)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolPacketException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while BusinessRequest received");
        }
        boolean argTransfer = isToApplied;
        if (argTransfer && !Configuration.configuration.getBusinessWhiteSet().contains(session.getAuth().getUser())) {
            logger.warn("Not allow to execute a BusinessRequest: " + session.getAuth().getUser());
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not allow to execute a BusinessRequest");
        }
        session.setStatus(200);
        String argRule = className;
        if (arguments != null) {
            argRule += " " + arguments;
        }
        ExecJavaTask task = new ExecJavaTask(argRule + " " + argTransfer,
                delay, extraArguments, session);
        task.setBusinessRequest(true);
        task.run();
        session.setStatus(201);
        if (task.isSuccess()) {
            session.setStatus(202);
            logger.info("Task done: " + className.split(" ")[0]);
        }
        return task.getFutureCompletion();
    }

    /**
     * Block/Unblock Request
     * 
     * 
     * @param channel
     * @param packet
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolBusinessException
     */
    public void blockRequest(Channel channel, BlockRequestPacket packet)
            throws OpenR66ProtocolPacketException, OpenR66ProtocolBusinessException {
        R66Result result = blockRequest(packet.getKey(), packet.getBlock());
        ValidPacket valid = new ValidPacket((packet.getBlock() ? "Block" : "Unblock") + " new request",
                result.getCode().getCode(),
                LocalPacketFactory.REQUESTUSERPACKET);
        try {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                    valid, true);
        } catch (OpenR66ProtocolPacketException e) {
        }
        channel.close();
        packet.clear();
    }

    /**
     * Block/Unblock Request
     * 
     * 
     * @param key
     * @param isBlocking
     * @return The result
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolBusinessException
     */
    public final R66Result blockRequest(byte[] key, boolean isBlocking)
            throws OpenR66ProtocolBusinessException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while BlockRequest received");
        }
        // SYSTEM authorization
        boolean isAdmin = session.getAuth().isValidRole(ROLE.SYSTEM);
        boolean isKeyValid = Configuration.configuration.isKeyValid(key);
        if (isAdmin && isKeyValid) {
            boolean block = isBlocking;
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyWarning(
                        (block ? "Block" : "Unblock") + " Order received",
                        session.getAuth().getUser());
            }
            logger.debug((block ? "Block" : "Unblock") + " Order received");
            Configuration.configuration.setShutdown(block);
            // inform back the requester
            // request of current values
            R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
            if (localChannelReference != null) {
                localChannelReference.validateRequest(result);
            }
            return result;
        }
        logger.error("Invalid Block command: from " + session.getAuth().getUser()
                + " AdmValid: " + isAdmin + " KeyValid: " + isKeyValid);
        throw new OpenR66ProtocolBusinessException("Invalid Block comand");
    }

    /**
     * Receive a request of information (Transfer information or File listing)
     * 
     * @param request
     *            InformationPacket.ASKENUM ordinal
     * @param rulename
     *            rulename for file path
     * @param filename
     *            partial name (including wildcard)
     * @param jsonOutput
     *            ValidPacket will contain Json format ?
     * @return the ValidPacket to answer containing: File Listing as Header and Number of entries as Middle
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolNoDataException
     * @throws OpenR66ProtocolPacketException
     */
    public final ValidPacket informationFile(byte request, String rulename, String filename, boolean jsonOutput)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolNoDataException, OpenR66ProtocolPacketException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Information received");
        }
        DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                : DbConstant.admin.getSession();
        DbRule rule;
        try {
            rule = new DbRule(rulename);
        } catch (WaarpDatabaseException e) {
            logger.error("Rule is unknown: " + rulename, e);
            throw new OpenR66ProtocolNoDataException(e);
        }
        try {
            session.getDir().changeDirectory(rule.getSendPath());

            if (request == InformationPacket.ASKENUM.ASKLIST.ordinal() ||
                    request == InformationPacket.ASKENUM.ASKMLSLIST.ordinal()) {
                // ls or mls from current directory
                List<String> list;
                if (request == InformationPacket.ASKENUM.ASKLIST.ordinal()) {
                    list = session.getDir().list(filename);
                } else {
                    list = session.getDir().listFull(filename, false);
                }

                StringBuilder builder = new StringBuilder();
                if (jsonOutput) {
                    ObjectNode node = JsonHandler.createObjectNode();
                    String asked = request == InformationPacket.ASKENUM.ASKLIST.ordinal() ? "ls" : "mls";
                    ArrayNode array = node.putArray(asked);
                    for (String elt : list) {
                        array.add(elt);
                    }
                    builder.append(JsonHandler.writeAsString(node));
                } else {
                    for (String elt : list) {
                        builder.append(elt).append('\n');
                    }
                }
                if (!jsonOutput) {
                    session.newState(VALIDOTHER);
                }
                ValidPacket validPacket = new ValidPacket(builder.toString(), "" + list.size(),
                        LocalPacketFactory.INFORMATIONPACKET);
                R66Result result = new R66Result(session, true,
                        ErrorCode.CompleteOk, null);
                result.setOther(validPacket);
                if (localChannelReference != null) {
                    localChannelReference.validateEndTransfer(result);
                    localChannelReference.validateRequest(result);
                }
                return validPacket;
            } else {
                // exists or ls or mls from current directory and filename
                R66File file = (R66File) session.getDir().setFile(filename, false);
                String sresult = null;
                if (request == InformationPacket.ASKENUM.ASKEXIST.ordinal()) {
                    if (jsonOutput) {
                        ObjectNode node = JsonHandler.createObjectNode();
                        node.put("exist", file.exists());
                        sresult = JsonHandler.writeAsString(node);
                    } else {
                        sresult = "" + file.exists();
                    }
                } else if (request == InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal()) {
                    sresult = session.getDir().fileFull(filename, false);
                    String[] list = sresult.split("\n");
                    sresult = list[1];
                    if (jsonOutput) {
                        ObjectNode node = JsonHandler.createObjectNode();
                        node.put("mls", sresult);
                        sresult = JsonHandler.writeAsString(node);
                    }
                } else {
                    session.newState(ERROR);
                    logger.warn("Unknown Request " + request);
                    return null;
                }
                if (!jsonOutput) {
                    session.newState(VALIDOTHER);
                }
                ValidPacket validPacket = new ValidPacket(sresult, "1",
                        LocalPacketFactory.INFORMATIONPACKET);
                R66Result result = new R66Result(session, true,
                        ErrorCode.CompleteOk, null);
                result.setOther(validPacket);
                if (localChannelReference != null) {
                    localChannelReference.validateEndTransfer(result);
                    localChannelReference.validateRequest(result);
                }
                return validPacket;
            }
        } catch (CommandAbstractException e) {
            session.newState(ERROR);
            logger.warn("Error while Request " + request + " "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Receive a request of information (Transfer information or File listing)
     * 
     * @param id
     *            Id of request
     * @param isTo
     *            True for remote host is requester, False for requested (default)
     * @param remoteHost
     *            requester/requested for transfer if jsonOutput is True, else (jsonOutput False) remoteHost is from current
     *            Authenticated user
     * @param jsonOutput
     *            ValidPacket will contain Json format ?
     * @return the ValidPacket to answer containing: Transfer Information as Header
     * @throws OpenR66ProtocolNotAuthenticatedException
     * @throws OpenR66ProtocolNoDataException
     * @throws OpenR66ProtocolPacketException
     */
    public final ValidPacket informationRequest(long id, boolean isTo, String remoteHost, boolean jsonOutput)
            throws OpenR66ProtocolNotAuthenticatedException,
            OpenR66ProtocolNoDataException, OpenR66ProtocolPacketException {
        if (!session.isAuthenticated()) {
            throw new OpenR66ProtocolNotAuthenticatedException(
                    "Not authenticated while Information received");
        }
        DbSession dbSession = (localChannelReference != null) ? localChannelReference.getDbSession()
                : DbConstant.admin.getSession();
        String remote = session.getAuth().getUser();
        if (jsonOutput && remoteHost != null && !remoteHost.isEmpty()) {
            remote = remoteHost;
        }
        String local = null;
        try {
            local = Configuration.configuration.getHostId(dbSession, remote);
        } catch (WaarpDatabaseException e1) {
            logger.error("Remote Host is unknown", e1);
            throw new OpenR66ProtocolNoDataException("Remote Host is unknown", e1);
        }
        DbTaskRunner runner = null;
        if (isTo) {
            logger.info("{} {} {}", id, remote, local);
            try {
                runner = new DbTaskRunner(session, null, id, remote, local);
            } catch (WaarpDatabaseException e) {
                logger.error(Messages.getString("LocalServerHandler.21") + id); //$NON-NLS-1$
                logger.debug("RunnerTask is not found: " + id + ":" + remote + ":" + local, e);
                throw new OpenR66ProtocolNoDataException(Messages.getString("LocalServerHandler.22") + id, e); //$NON-NLS-1$
            }
        } else {
            try {
                runner = new DbTaskRunner(session, null, id, local, remote);
            } catch (WaarpDatabaseException e) {
                logger.debug("RunnerTask is not found: " + id + ":" + local + ":" + remote, e);
                logger.error(Messages.getString("LocalServerHandler.21") + id);
                throw new OpenR66ProtocolNoDataException("(Local) " + Messages.getString("LocalServerHandler.21") + id,
                        e);
            }
        }
        if (!jsonOutput) {
            session.newState(VALIDOTHER);
        }
        ValidPacket validPacket;
        try {
            validPacket = new ValidPacket(jsonOutput ? runner.asJson() : runner.asXML(), "",
                    LocalPacketFactory.INFORMATIONPACKET);
        } catch (OpenR66ProtocolBusinessException e) {
            logger.error("RunnerTask cannot be found: " + id, e);
            throw new OpenR66ProtocolNoDataException("RunnerTask cannot be found: " + id, e);
        }
        R66Result result = new R66Result(session, true,
                ErrorCode.CompleteOk, null);
        result.setOther(validPacket);
        if (localChannelReference != null) {
            localChannelReference.validateEndTransfer(result);
            localChannelReference.validateRequest(result);
        }
        return validPacket;
    }

    /**
     * Receive a TransferRequest in JSON mode: just setting it to be scheduled
     * 
     * @param request
     * @return the result associated with the new transfer request
     */
    public final R66Result transferRequest(TransferRequestJsonPacket request) {
        DbTaskRunner runner = initTransferRequest(request);
        if (runner != null) {
            runner.changeUpdatedInfo(AbstractDbData.UpdatedInfo.TOSUBMIT);
            boolean isSender = runner.isSender();
            if (!runner.forceSaveStatus()) {
                logger.warn("Cannot prepare task");
                return new R66Result(session, false, ErrorCode.CommandNotFound,
                        runner);
            }
            R66Result result = new R66Result(session, false, ErrorCode.InitOk,
                    runner);
            try {
                runner.select();
            } catch (WaarpDatabaseException e) {
            }
            runner.setSender(isSender);
            request.setFromDbTaskRunner(runner);
            request.validate();
            return result;
        } else {
            logger.warn("ERROR: Transfer NOT scheduled");
            R66Result result = new R66Result(session, false, ErrorCode.Internal,
                    runner);
            return result;
        }
    }

    /**
     * initialize a new Transfer Request
     * 
     * @param request
     * @return the associated DbTaskRunner
     */
    private final DbTaskRunner initTransferRequest(TransferRequestJsonPacket request) {
        Timestamp ttimestart = null;
        Date date = request.getStart();
        if (date != null) {
            ttimestart = new Timestamp(date.getTime());
        } else if (request.getDelay() > 0) {
            if (request.isAdditionalDelay()) {
                ttimestart = new Timestamp(System.currentTimeMillis() +
                        request.getDelay());
            } else {
                ttimestart = new Timestamp(request.getDelay());
            }
        }
        DbRule rule;
        try {
            rule = new DbRule(request.getRulename());
        } catch (WaarpDatabaseException e) {
            logger.warn("Cannot get Rule: " + request.getRulename(), e);
            return null;
        }
        int mode = rule.getMode();
        if (RequestPacket.isMD5Mode(request.getMode())) {
            mode = RequestPacket.getModeMD5(mode);
        }
        DbTaskRunner taskRunner = null;
        long tid = DbConstant.ILLEGALVALUE;
        if (request.getSpecialId() != 0 || request.getSpecialId() == DbConstant.ILLEGALVALUE) {
            tid = request.getSpecialId();
        }
        if (tid != DbConstant.ILLEGALVALUE) {
            try {
                taskRunner = new DbTaskRunner(tid, request.getRequested());
                // requested
                taskRunner.setSenderByRequestToValidate(true);
            } catch (WaarpDatabaseException e) {
                logger.warn("Cannot get task", e);
                return null;
            }
        } else {
            String sep = PartnerConfiguration.getSeparator(request.getRequested());
            RequestPacket requestPacket = new RequestPacket(request.getRulename(),
                    mode, request.getFilename(), request.getBlocksize(), 0,
                    tid, request.getFileInformation(), -1, sep);
            // Not isRecv since it is the requester, so send => isRetrieve is true
            boolean isRetrieve = !RequestPacket.isRecvMode(requestPacket.getMode());
            try {
                taskRunner =
                        new DbTaskRunner(rule, isRetrieve, requestPacket,
                                request.getRequested(), ttimestart);
            } catch (WaarpDatabaseException e) {
                logger.warn("Cannot get task", e);
                return null;
            }
        }
        return taskRunner;
    }
}
