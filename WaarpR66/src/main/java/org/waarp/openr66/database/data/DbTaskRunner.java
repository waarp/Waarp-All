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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.AbstractTask;
import org.waarp.openr66.context.task.TaskType;
import org.waarp.openr66.context.task.exception.OpenR66RunnerEndTasksException;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.database.StatementExecutor;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.dao.xml.XMLTransferDAO;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.LocalTransaction;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.NbAndSpecialId;
import org.waarp.openr66.protocol.utils.R66Future;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.waarp.common.database.DbConstant.*;

/**
 * Task Runner from pre operation to transfer to post operation, except in case
 * of error
 */
public class DbTaskRunner extends AbstractDbDataDao<Transfer> {
  private static final String TRANSFER_NOT_FOUND = "Transfer not found";

  public static final String JSON_ORIGINALSIZE = "ORIGINALSIZE";

  public static final String JSON_THROUGHMODE = "THROUGHMODE";

  public static final String JSON_RESCHEDULE = "RESCHEDULE";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbTaskRunner.class);

  /**
   * Create the LRU cache
   *
   * @param limit limit of number of entries in the cache
   * @param ttl time to leave used
   */
  public static void createLruCache(int limit, long ttl) {
    XMLTransferDAO.createLruCache(limit, ttl);
  }

  public static String hashStatus() {
    return XMLTransferDAO.hashStatus();
  }

  /**
   * To enable clear of oldest entries in the cache
   *
   * @return the number of elements removed
   */
  public static int clearCache() {
    return XMLTransferDAO.clearCache();
  }

  /**
   * To update the TTL for the cache (to 10xTIMEOUT)
   *
   * @param ttl
   */
  public static void updateLruCacheTimeout(long ttl) {
    XMLTransferDAO.updateLruCacheTimeout(ttl);
  }

  public enum Columns {
    GLOBALSTEP, GLOBALLASTSTEP, STEP, RANK, STEPSTATUS, RETRIEVEMODE, FILENAME,
    ISMOVED, IDRULE, BLOCKSZ, ORIGINALNAME, FILEINFO, TRANSFERINFO, MODETRANS,
    STARTTRANS, STOPTRANS, INFOSTATUS, UPDATEDINFO, OWNERREQ, REQUESTER,
    REQUESTED, SPECIALID
  }

  public static final int[] dbTypes = {
      Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.CHAR,
      Types.BIT, Types.VARCHAR, Types.BIT, Types.NVARCHAR, Types.INTEGER,
      Types.VARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR, Types.INTEGER,
      Types.TIMESTAMP, Types.TIMESTAMP, Types.CHAR, Types.INTEGER,
      Types.NVARCHAR, Types.NVARCHAR, Types.NVARCHAR, Types.BIGINT
  };

  public static final String table = " RUNNER ";

  public static final String fieldseq = "RUNSEQ";

  public static final Columns[] indexes = {
      Columns.STARTTRANS, Columns.OWNERREQ, Columns.STEPSTATUS,
      Columns.UPDATEDINFO, Columns.GLOBALSTEP, Columns.INFOSTATUS,
      Columns.SPECIALID
  };

  public static final String XMLRUNNERS = "taskrunners";
  public static final String XMLRUNNER = "runner";
  public static final String XMLEXTENSION = "_singlerunner.xml";

  /**
   * GlobalStep Bounds
   */
  public enum TASKSTEP {
    NOTASK, PRETASK, TRANSFERTASK, POSTTASK, ALLDONETASK, ERRORTASK
  }

  // Values
  private DbRule rule;

  private R66Session session;

  private volatile boolean continueTransfer = true;

  private boolean rescheduledTransfer;

  private LocalChannelReference localChannelReference;

  private boolean isRecvThrough;
  private boolean isSendThrough;
  private long originalSize = -1;

  /**
   * Special For DbTaskRunner
   */
  public static final int NBPRKEY = 4;
  // ALL TABLE SHOULD IMPLEMENT THIS

  protected static final String selectAllFields =
      Columns.GLOBALSTEP.name() + ',' + Columns.GLOBALLASTSTEP.name() + ',' +
      Columns.STEP.name() + ',' + Columns.RANK.name() + ',' +
      Columns.STEPSTATUS.name() + ',' + Columns.RETRIEVEMODE.name() + ',' +
      Columns.FILENAME.name() + ',' + Columns.ISMOVED.name() + ',' +
      Columns.IDRULE.name() + ',' + Columns.BLOCKSZ.name() + ',' +
      Columns.ORIGINALNAME.name() + ',' + Columns.FILEINFO.name() + ',' +
      Columns.TRANSFERINFO.name() + ',' + Columns.MODETRANS.name() + ',' +
      Columns.STARTTRANS.name() + ',' + Columns.STOPTRANS.name() + ',' +
      Columns.INFOSTATUS.name() + ',' + Columns.UPDATEDINFO.name() + ',' +
      Columns.OWNERREQ.name() + ',' + Columns.REQUESTER.name() + ',' +
      Columns.REQUESTED.name() + ',' + Columns.SPECIALID.name();

  protected static final String updateAllFields =
      Columns.GLOBALSTEP.name() + "=?," + Columns.GLOBALLASTSTEP.name() +
      "=?," + Columns.STEP.name() + "=?," + Columns.RANK.name() + "=?," +
      Columns.STEPSTATUS.name() + "=?," + Columns.RETRIEVEMODE.name() + "=?," +
      Columns.FILENAME.name() + "=?," + Columns.ISMOVED.name() + "=?," +
      Columns.IDRULE.name() + "=?," + Columns.BLOCKSZ.name() + "=?," +
      Columns.ORIGINALNAME.name() + "=?," + Columns.FILEINFO.name() + "=?," +
      Columns.TRANSFERINFO.name() + "=?," + Columns.MODETRANS.name() + "=?," +
      Columns.STARTTRANS.name() + "=?," + Columns.STOPTRANS.name() + "=?," +
      Columns.INFOSTATUS.name() + "=?," + Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues =
      " (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

  @Override
  protected void initObject() {
    // Nothing
  }

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected AbstractDAO<Transfer> getDao() throws DAOConnectionException {
    return DAOFactory.getInstance().getTransferDAO();
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @return never
   */
  @Override
  protected String getPrimaryKey() {
    throw new UnsupportedOperationException("Not correct for Transfer");
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @return never
   */
  @Override
  protected String getPrimaryField() {
    throw new UnsupportedOperationException("Not correct for Transfer");
  }

  /**
   * @param session
   * @param requestPacket
   *
   * @return The associated requested Host Id
   */
  public static String getRequested(R66Session session,
                                    RequestPacket requestPacket) {
    if (requestPacket.isToValidate()) {
      // the request is initiated and sent by the requester
      try {
        return Configuration.configuration.getHostId(session.getAuth().isSsl());
      } catch (final OpenR66ProtocolNoSslException e) {
        return Configuration.configuration.getHostId();
      }
    } else {
      // the request is sent after acknowledge by the requested
      return session.getAuth().getUser();
    }
  }

  /**
   * @param session
   * @param requestPacket
   *
   * @return The associated requester Host Id
   */
  public static String getRequester(R66Session session,
                                    RequestPacket requestPacket) {
    if (requestPacket.isToValidate()) {
      return session.getAuth().getUser();
    } else {
      try {
        return Configuration.configuration.getHostId(session.getAuth().isSsl());
      } catch (final OpenR66ProtocolNoSslException e) {
        return Configuration.configuration.getHostId();
      }
    }
  }

  public void checkThroughMode() {
    isRecvThrough = RequestPacket
        .isRecvThroughMode(pojo.getTransferMode(), isSelfRequested());
    isSendThrough = RequestPacket
        .isSendThroughMode(pojo.getTransferMode(), isSelfRequested());

    if (localChannelReference != null) {
      if (localChannelReference.isRecvThroughMode()) {
        isRecvThrough = true;
      }
      if (localChannelReference.isSendThroughMode()) {
        isSendThrough = true;
      }
      if (isRecvThrough && !localChannelReference.isRecvThroughMode()) {
        // Cannot be a RecvThrough
        isRecvThrough = false;
      }
      if (isSendThrough && !localChannelReference.isSendThroughMode()) {
        isSendThrough = false;
      }
    }
    logger.debug("DbTask " + pojo.getTransferMode() + " isRecvThrough: " +
                 isRecvThrough + " isSendThrough: " + isSendThrough);
  }

  private void setStopNow() {
    pojo.setStop(new Timestamp(System.currentTimeMillis()));
  }

  public DbTaskRunner(Transfer transfer) {
    if (transfer == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.pojo = transfer;
  }

  /**
   * Constructor for submission (no transfer session), from database. It is
   * created, so with a new specialId if
   * necessary
   *
   * @param rule
   * @param isSender
   * @param requestPacket
   * @param requested
   * @param startTime
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(DbRule rule, boolean isSender,
                      RequestPacket requestPacket, String requested,
                      Timestamp startTime) throws WaarpDatabaseException {
    session = null;
    this.rule = rule;

    if (startTime != null) {
      pojo = new Transfer(requested, rule.getIdRule(), requestPacket.getMode(),
                          isSender, requestPacket.getFilename(),
                          requestPacket.getFileInformation(),
                          requestPacket.getBlocksize(), startTime);
    } else {
      pojo = new Transfer(requested, rule.getIdRule(), requestPacket.getMode(),
                          isSender, requestPacket.getFilename(),
                          requestPacket.getFileInformation(),
                          requestPacket.getBlocksize());
    }

    // Usefull ?
    pojo.setRank(requestPacket.getRank());
    logger.trace("TRACE ID {}", requestPacket.getSpecialId());
    pojo.setId(requestPacket.getSpecialId());

    originalSize = requestPacket.getOriginalSize();
    setOriginalSizeTransferMap(originalSize);
    // itself but according to SSL
    pojo.setRequester(
        Configuration.configuration.getHostId(dbSession, requested));

    // Retrieve rule
    this.rule = new DbRule(getRuleId());
    if (requestPacket.getMode() != rule.getMode()) {
      if (RequestPacket.isMD5Mode(requestPacket.getMode())) {
        pojo.setTransferMode(RequestPacket.getModeMD5(rule.getMode()));
      } else {
        pojo.setTransferMode(rule.getMode());
      }
    }
    checkThroughMode();
    insert();
    requestPacket.setSpecialId(pojo.getId());
  }

  /**
   * Constructor from a request without a valid Special Id to be inserted into
   * databases
   *
   * @param session
   * @param rule
   * @param isSender
   * @param requestPacket
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(R66Session session, DbRule rule, boolean isSender,
                      RequestPacket requestPacket)
      throws WaarpDatabaseException {
    this.session = session;
    localChannelReference = session.getLocalChannelReference();
    this.rule = rule;

    pojo = new Transfer(getRequested(session, requestPacket), rule.getIdRule(),
                        requestPacket.getMode(), isSender,
                        requestPacket.getFilename(),
                        requestPacket.getFileInformation(),
                        requestPacket.getBlocksize());
    pojo.setRequester(getRequester(session, requestPacket));
    pojo.setRank(requestPacket.getRank());
    if (requestPacket.getSpecialId() != ILLEGALVALUE) {
      logger.trace("TRACE ID {}", requestPacket.getSpecialId());
      pojo.setId(requestPacket.getSpecialId());
    }
    originalSize = requestPacket.getOriginalSize();
    setOriginalSizeTransferMap(originalSize);

    checkThroughMode();
    insert();
    requestPacket.setSpecialId(pojo.getId());
  }

  /**
   * Constructor from a request with a valid Special Id so loaded from
   * database
   *
   * @param session
   * @param rule
   * @param id
   * @param requester
   * @param requested
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(R66Session session, DbRule rule, long id,
                      String requester, String requested)
      throws WaarpDatabaseException {
    this.session = session;
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      pojo = transferAccess.select(id, requester, requested,
                                   Configuration.configuration.getHostId());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(TRANSFER_NOT_FOUND, e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    this.rule = new DbRule(getRuleId());
    if (rule != null && !pojo.getRule().equals(rule.getIdRule())) {
      throw new WaarpDatabaseNoDataException("Rule does not correspond");
    }
  }

  /**
   * Minimal constructor from database
   *
   * @param id
   * @param requester
   * @param requested
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(long id, String requester, String requested)
      throws WaarpDatabaseException {
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      pojo = transferAccess.select(id, requester, requested,
                                   Configuration.configuration.getHostId());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(TRANSFER_NOT_FOUND, e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    rule = new DbRule(getRuleId());
  }

  /**
   * Minimal constructor from database
   *
   * @param id
   * @param requester
   * @param requested
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(long id, String requester, String requested, String owner)
      throws WaarpDatabaseException {
    this(id, requester, requested);
    if (owner == null || owner.isEmpty()) {
      pojo.setOwnerRequest(Configuration.configuration.getHostId());
    } else {
      pojo.setOwnerRequest(owner);
    }
  }

  /**
   * To create a new DbTaskRunner (specialId could be invalid) without making
   * any entry in the database
   *
   * @param source
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(ObjectNode source) throws WaarpDatabaseException {
    pojo = new Transfer();
    setFromJson(source, false);
  }

  @Override
  public void setFromJson(ObjectNode source, boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    if (pojo == null) {
      pojo = new Transfer();
    }
    for (final Columns column : Columns.values()) {
      if (column == Columns.UPDATEDINFO) {
        continue;
      }
      final JsonNode item = source.get(column.name());
      if (item != null && !item.isMissingNode() && !item.isNull()) {
        switch (column) {
          case BLOCKSZ:
            pojo.setBlockSize(item.asInt());
            break;
          case FILEINFO:
            pojo.setFileInfo(item.asText());
            break;
          case FILENAME:
            pojo.setFilename(item.asText());
            break;
          case GLOBALLASTSTEP:
            pojo.setLastGlobalStep(Transfer.TASKSTEP.valueOf(item.asInt()));
            break;
          case GLOBALSTEP:
            pojo.setGlobalStep(Transfer.TASKSTEP.valueOf(item.asInt()));
            break;
          case IDRULE:
            pojo.setRule(item.asText());
            break;
          case INFOSTATUS:
            pojo.setInfoStatus(ErrorCode.getFromCode(item.asText()));
            break;
          case ISMOVED:
            pojo.setIsMoved(item.asBoolean());
            break;
          case MODETRANS:
            pojo.setTransferMode(item.asInt());
            break;
          case ORIGINALNAME:
            pojo.setOriginalName(item.asText());
            break;
          case OWNERREQ:
            String owner = item.asText();
            if (owner == null || owner.isEmpty()) {
              owner = Configuration.configuration.getHostId();
            }
            pojo.setOwnerRequest(owner);
            break;
          case RANK:
            pojo.setRank(item.asInt());
            break;
          case REQUESTED:
            pojo.setRequested(item.asText());
            break;
          case REQUESTER:
            pojo.setRequester(item.asText());
            break;
          case RETRIEVEMODE:
            pojo.setRetrieveMode(item.asBoolean());
            break;
          case SPECIALID:
            pojo.setId(item.asLong());
            break;
          case STARTTRANS:
            long start = item.asLong();
            if (start == 0) {
              start = System.currentTimeMillis();
            }
            pojo.setStart(new Timestamp(start));
            break;
          case STEP:
            pojo.setStep(item.asInt());
            break;
          case STEPSTATUS:
            pojo.setStepStatus(ErrorCode.getFromCode(item.asText()));
            break;
          case STOPTRANS:
            long stop = item.asLong();
            if (stop == 0) {
              stop = System.currentTimeMillis();
            }
            pojo.setStop(new Timestamp(stop));
            break;
          case TRANSFERINFO:
            pojo.setTransferInfo(item.asText());
            break;
          case UPDATEDINFO:
            // ignore
            break;
          default:
            break;
        }
      }
    }
    JsonNode node = source.path(JSON_RESCHEDULE);
    if (!node.isMissingNode() || !node.isNull()) {
      rescheduledTransfer = node.asBoolean(false);
    }
    node = source.path(JSON_THROUGHMODE);
    if (!node.isMissingNode() || !node.isNull()) {
      if (RequestPacket.isRecvMode(pojo.getTransferMode())) {
        isRecvThrough = node.asBoolean();
      } else {
        isSendThrough = node.asBoolean();
      }
    }
    node = source.path(JSON_ORIGINALSIZE);
    if (!node.isMissingNode() || !node.isNull()) {
      originalSize = node.asLong(getOriginalSizeTransferMap());
    }
    isSaved = false;
    try {
      rule = new DbRule(getRuleId());
    } catch (final WaarpDatabaseException e) {
      // ignore
      rule = null;
    }
    if (pojo.getFilename() == null || pojo.getFilename().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Cannot create a transfer without filename");
    } else if (pojo.getRule() == null || pojo.getRule().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Cannot create a transfer without rule");
    } else if (pojo.getOwnerRequest() == null ||
               pojo.getOwnerRequest().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Cannot create a transfer without owner");
    } else if (pojo.getRequester() == null || pojo.getRequester().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Cannot create a transfer without requester");
    } else if (pojo.getRequested() == null || pojo.getRequested().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Cannot create a transfer without requested");
    }
    checkThroughMode();
  }

  /**
   * Constructor to initiate a request with a valid previous Special Id so
   * loaded from database.
   * <p>
   * This object cannot be used except to retrieve information.
   *
   * @param id
   * @param requested
   *
   * @throws WaarpDatabaseException
   */
  public DbTaskRunner(long id, String requested) throws WaarpDatabaseException {
    TransferDAO transferAccess = null;
    logger.trace("TRACE ID {}", id);
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      pojo = transferAccess
          .select(id, Configuration.configuration.getHostId(), requested,
                  Configuration.configuration.getHostId());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(TRANSFER_NOT_FOUND, e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
  }

  /**
   * @return the condition to limit access to the row concerned by the Host
   */
  private static String getLimitWhereCondition() {
    return " " + Columns.OWNERREQ + " = '" +
           Configuration.configuration.getHostId() + "' ";
  }

  /**
   * To allow to remove specifically one SpecialId from MemoryHashmap
   *
   * @param specialId
   */
  public static final void removeNoDbSpecialId(long specialId) {
    XMLTransferDAO.removeNoDbSpecialId(specialId);
  }

  /**
   * To update the usage TTL of the associated object
   *
   * @param specialId
   */
  public static final void updateUsed(long specialId) {
    XMLTransferDAO.updateUsed(specialId);
  }

  /**
   * As insert but with the ability to change the SpecialId
   *
   * @throws WaarpDatabaseException
   */
  public void create() throws WaarpDatabaseException {
    setStopNow();
    insert();
  }

  @Override
  public boolean exist() throws WaarpDatabaseException {
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      return transferAccess
          .exist(pojo.getId(), pojo.getRequester(), pojo.getRequested(),
                 Configuration.configuration.getHostId());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
  }

  /**
   * Shall be called to ensure that item is really available in database
   *
   * @return True iff the element exists in a database (and reloaded then from
   *     Database)
   *
   * @throws WaarpDatabaseException
   */
  public boolean checkFromDbForSubmit() throws WaarpDatabaseException {
    if (exist()) {
      select();
      rule = new DbRule(getRuleId());
      return true;
    }
    return false;
  }

  @Override
  public void select() throws WaarpDatabaseException {
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      pojo = transferAccess
          .select(pojo.getId(), pojo.getRequester(), pojo.getRequested(),
                  Configuration.configuration.getHostId());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(TRANSFER_NOT_FOUND, e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    rule = new DbRule(getRuleId());
    checkThroughMode();
  }

  @Override
  public void update() throws WaarpDatabaseException {
    // SNMP notification
    if (pojo.getUpdatedInfo().equals(UpdatedInfo.INERROR) ||
        pojo.getUpdatedInfo().equals(UpdatedInfo.INTERRUPTED)) {
      if (Configuration.configuration.getR66Mib() != null) {
        Configuration.configuration.getR66Mib().notifyInfoTask(
            "Task is " + pojo.getUpdatedInfo().name(), this);
      }
    } else {
      if (pojo.getGlobalStep() != Transfer.TASKSTEP.TRANSFERTASK ||
          pojo.getGlobalStep() == Transfer.TASKSTEP.TRANSFERTASK &&
          pojo.getRank() % 100 == 0) {
        if (Configuration.configuration.getR66Mib() != null) {
          Configuration.configuration.getR66Mib().notifyTask(
              "Task is currently " + pojo.getUpdatedInfo().name(), this);
        }
      }
    }
    // FIX SelfRequest
    if (isSelfRequest()) {
      if (RequestPacket.isCompatibleMode(pojo.getTransferMode(),
                                         pojo.getRetrieveMode()?
                                             RequestPacket.TRANSFERMODE.RECVMODE
                                                 .ordinal() :
                                             RequestPacket.TRANSFERMODE.SENDMODE
                                                 .ordinal())) {
        optimizedUpdate();
      }
    } else {
      optimizedUpdate();
    }
  }

  /**
   * Update Runner using special PreparedStatement
   *
   * @throws WaarpDatabaseException
   */
  protected void optimizedUpdate() throws WaarpDatabaseException {
    setStopNow();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.update(pojo);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(TRANSFER_NOT_FOUND, e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
  }

  public void clean() {
    // ignore
  }

  /**
   * Special method used to force insert in case of SelfSubmit
   *
   * @throws WaarpDatabaseException
   */
  public boolean specialSubmit() throws WaarpDatabaseException {
    setStopNow();
    insert();
    return false;
  }

  /**
   * Partial set from another runner (infostatus, rank, status, step, stop,
   * filename, globallastep, globalstep,
   * isFileMoved)
   *
   * @param runner
   */
  public void setFrom(DbTaskRunner runner) {
    if (runner != null) {
      logger.trace("TRACE ID {}", pojo.getId());
      logger.trace("TRACE ID {}", runner.getSpecialId());
      pojo.setInfoStatus(runner.getErrorInfo());
      pojo.setRank(runner.getRank());
      pojo.setStepStatus(runner.getStatus());
      pojo.setStep(runner.getStep());
      pojo.setStop(runner.getStop());
      pojo.setFilename(runner.getFilename());
      pojo.setGlobalStep(runner.pojo.getGlobalStep());
      pojo.setLastGlobalStep(runner.pojo.getLastGlobalStep());
      pojo.setIsMoved(runner.isFileMoved());
    }
  }

  public boolean isRecvThrough() {
    return isRecvThrough;
  }

  public boolean isSendThrough() {
    return isSendThrough;
  }

  /**
   * Private constructor for Commander only
   */
  private DbTaskRunner() {
    pojo = new Transfer();
    session = null;
    rule = null;
  }

  /**
   * Set a localChannelReference
   *
   * @param localChannelReference
   */
  public void setLocalChannelReference(
      LocalChannelReference localChannelReference) {
    this.localChannelReference = localChannelReference;
  }

  /**
   * @return the localChannelReference
   */
  public LocalChannelReference getLocalChannelReference() {
    return localChannelReference;
  }

  /**
   * For instance from Commander when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated DbTaskRunner
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbTaskRunner getFromStatement(
      DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbTaskRunner dbTaskRunner = new DbTaskRunner();
    AbstractDAO<Transfer> transferDAO = null;
    try {
      transferDAO = dbTaskRunner.getDao();
      dbTaskRunner.pojo = ((StatementExecutor<Transfer>) transferDAO)
          .getFromResultSet(preparedStatement.getResultSet());
      if (dbTaskRunner.rule == null && dbTaskRunner.pojo.getRule() != null) {
        try {
          dbTaskRunner.rule = new DbRule(dbTaskRunner.getRuleId());
        } catch (final WaarpDatabaseException e) {
          throw new WaarpDatabaseSqlException(
              "Rule cannot be found for DbTaskRunner: " + dbTaskRunner.asJson(),
              e);
        }
      }
      dbTaskRunner.checkThroughMode();
      return dbTaskRunner;
    } catch (SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
  }

  /**
   * For REST interface, to prevent DbRule issue
   *
   * @param preparedStatement
   *
   * @return the next updated DbTaskRunner
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbTaskRunner getFromStatementNoDbRule(
      DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbTaskRunner dbTaskRunner = new DbTaskRunner();
    AbstractDAO<Transfer> transferDAO = null;
    try {
      transferDAO = dbTaskRunner.getDao();
      dbTaskRunner.pojo = ((StatementExecutor<Transfer>) transferDAO)
          .getFromResultSet(preparedStatement.getResultSet());
      if (dbTaskRunner.rule == null) {
        try {
          dbTaskRunner.rule = new DbRule(dbTaskRunner.getRuleId());
        } catch (final WaarpDatabaseNoDataException e) {
          // ignore
        } catch (final WaarpDatabaseException e) {
          throw new WaarpDatabaseSqlException(e);
        }
      }
      dbTaskRunner.checkThroughMode();
      return dbTaskRunner;
    } catch (SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
  }

  /**
   * @param preparedStatement
   * @param srcrequest
   * @param limit
   * @param orderby
   * @param startid
   * @param stopid
   * @param start
   * @param stop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   * @param done
   * @param all
   *
   * @return The DbPreparedStatement already prepared according to select or
   *     delete command
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  private static DbPreparedStatement getFilterCondition(
      DbPreparedStatement preparedStatement, String srcrequest, int limit,
      String orderby, String startid, String stopid, Timestamp start,
      Timestamp stop, String rule, String req, boolean pending,
      boolean transfer, boolean error, boolean done, boolean all)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request = srcrequest;
    if (startid == null && stopid == null && start == null && stop == null &&
        rule == null && req == null && all) {
      // finish
      if (limit > 0) {
        request = preparedStatement.getDbSession().getAdmin().getDbModel()
                                   .limitRequest(selectAllFields,
                                                 request + orderby, limit);
      } else {
        request += orderby;
      }
      preparedStatement.createPrepareStatement(request);
      return preparedStatement;
    }
    request += " WHERE ";
    final StringBuilder scondition = new StringBuilder();
    boolean hasCondition = false;
    if (start != null && stop != null) {
      scondition.append(Columns.STARTTRANS.name()).append(" >= ? AND ")
                .append(Columns.STARTTRANS.name()).append(" <= ? ");
      hasCondition = true;
    } else if (start != null) {
      scondition.append(Columns.STARTTRANS.name()).append(" >= ? ");
      hasCondition = true;
    } else if (stop != null) {
      scondition.append(Columns.STARTTRANS.name()).append(" <= ? ");
      hasCondition = true;
    }
    if (startid != null) {
      if (hasCondition) {
        scondition.append(" AND ");
      }
      hasCondition = true;
      scondition.append(Columns.SPECIALID.name()).append(" >= ? ");
    }
    if (stopid != null) {
      if (hasCondition) {
        scondition.append(" AND ");
      }
      hasCondition = true;
      scondition.append(Columns.SPECIALID.name()).append(" <= ? ");
    }
    if (rule != null) {
      if (hasCondition) {
        scondition.append(" AND ");
      }
      hasCondition = true;
      scondition.append(Columns.IDRULE.name()).append(" LIKE '%").append(rule)
                .append("%' ");
    }
    if (req != null) {
      if (hasCondition) {
        scondition.append(" AND ");
      }
      hasCondition = true;
      scondition.append("( ").append(Columns.REQUESTED.name())
                .append(" LIKE '%").append(req).append("%' OR ")
                .append(Columns.REQUESTER.name()).append(" LIKE '%").append(req)
                .append("%' )");
    }
    if (!all) {
      if (hasCondition) {
        scondition.append(" AND ");
      }
      hasCondition = true;
      scondition.append("( ");
      boolean hasone = false;
      if (pending) {
        scondition.append(Columns.UPDATEDINFO.name()).append(" = ")
                  .append(UpdatedInfo.TOSUBMIT.ordinal());
        hasone = true;
      }
      if (transfer) {
        if (hasone) {
          scondition.append(" OR ");
        }
        scondition.append("( ").append(Columns.UPDATEDINFO.name()).append(" = ")
                  .append(UpdatedInfo.RUNNING.ordinal()).append(" )");
        hasone = true;
      }
      if (error) {
        if (hasone) {
          scondition.append(" OR ");
        }
        scondition.append(Columns.GLOBALSTEP.name()).append(" = ")
                  .append(TASKSTEP.ERRORTASK.ordinal()).append(" OR ")
                  .append(Columns.UPDATEDINFO.name()).append(" = ")
                  .append(UpdatedInfo.INERROR.ordinal()).append(" OR ")
                  .append(Columns.UPDATEDINFO.name()).append(" = ")
                  .append(UpdatedInfo.INTERRUPTED.ordinal());
        hasone = true;
      }
      if (done) {
        if (hasone) {
          scondition.append(" OR ");
        }
        scondition.append(Columns.GLOBALSTEP.name()).append(" = ")
                  .append(TASKSTEP.ALLDONETASK.ordinal()).append(" OR ")
                  .append(Columns.UPDATEDINFO.name()).append(" = ")
                  .append(UpdatedInfo.DONE.ordinal());
        hasone = true;
      }
      if (!hasone) {
        scondition.append(Columns.UPDATEDINFO.name()).append(" IS NOT NULL ");
      }
      scondition.append(" )");
    }
    if (limit > 0) {
      scondition.insert(0, request).append(orderby);
      request = scondition.toString();
      request = preparedStatement.getDbSession().getAdmin().getDbModel()
                                 .limitRequest(selectAllFields, request, limit);
    } else {
      scondition.insert(0, request).append(orderby);
      request = scondition.toString();
    }
    preparedStatement.createPrepareStatement(request);
    int rank = 1;
    try {
      if (start != null && stop != null) {
        preparedStatement.getPreparedStatement().setTimestamp(rank, start);
        rank++;
        preparedStatement.getPreparedStatement().setTimestamp(rank, stop);
        rank++;
      } else if (start != null) {
        preparedStatement.getPreparedStatement().setTimestamp(rank, start);
        rank++;
      } else if (stop != null) {
        preparedStatement.getPreparedStatement().setTimestamp(rank, stop);
        rank++;
      }
      if (startid != null) {
        long value = ILLEGALVALUE;
        try {
          value = Long.parseLong(startid);
        } catch (final NumberFormatException e) {
          // ignore then
        }
        preparedStatement.getPreparedStatement().setLong(rank, value);
        rank++;
      }
      if (stopid != null) {
        long value = Long.MAX_VALUE;
        try {
          value = Long.parseLong(stopid);
        } catch (final NumberFormatException e) {
          // ignore then
        }
        preparedStatement.getPreparedStatement().setLong(rank, value);
        rank++;
      }
    } catch (final SQLException e) {
      preparedStatement.realClose();
      throw new WaarpDatabaseSqlException(e);
    }
    return preparedStatement;
  }

  /**
   * @param session
   * @param limit
   * @param orderBySpecialId
   * @param startid
   * @param stopid
   * @param start
   * @param stop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   * @param done
   * @param all
   *
   * @return the DbPreparedStatement according to the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStatement(DbSession session,
                                                              int limit,
                                                              boolean orderBySpecialId,
                                                              String startid,
                                                              String stopid,
                                                              Timestamp start,
                                                              Timestamp stop,
                                                              String rule,
                                                              String req,
                                                              boolean pending,
                                                              boolean transfer,
                                                              boolean error,
                                                              boolean done,
                                                              boolean all)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    return getFilterPrepareStatement(session, limit, orderBySpecialId, startid,
                                     stopid, start, stop, rule, req, pending,
                                     transfer, error, done, all, null);
  }

  /**
   * @param session
   * @param limit
   * @param orderBySpecialId
   * @param startid
   * @param stopid
   * @param start
   * @param stop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   * @param done
   * @param all
   * @param owner
   *
   * @return the DbPreparedStatement according to the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStatement(DbSession session,
                                                              int limit,
                                                              boolean orderBySpecialId,
                                                              String startid,
                                                              String stopid,
                                                              Timestamp start,
                                                              Timestamp stop,
                                                              String rule,
                                                              String req,
                                                              boolean pending,
                                                              boolean transfer,
                                                              boolean error,
                                                              boolean done,
                                                              boolean all,
                                                              String owner)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String orderby = "";
    if (startid == null && stopid == null && start == null && stop == null &&
        rule == null && req == null && all) {
      if (owner == null || owner.isEmpty()) {
        orderby = " WHERE " + getLimitWhereCondition();
      } else if (!"*".equals(owner)) {
        orderby = " WHERE " + Columns.OWNERREQ + " = '" + owner + "' ";
      }
    } else {
      if (owner == null || owner.isEmpty()) {
        orderby = " AND " + getLimitWhereCondition();
      } else if (!"*".equals(owner)) {
        orderby = " AND " + Columns.OWNERREQ + " = '" + owner + "' ";
      }
    }
    if (orderBySpecialId) {
      orderby += " ORDER BY " + Columns.SPECIALID.name() + " DESC ";
    } else {
      orderby += " ORDER BY " + Columns.STARTTRANS.name() + " DESC ";
    }
    return getFilterCondition(preparedStatement, request, limit, orderby,
                              startid, stopid, start, stop, rule, req, pending,
                              transfer, error, done, all);
  }

  /**
   * @param info
   * @param orderByStart If true, sort on Start ; If false, does not
   *     set the
   *     limit on start
   * @param limit
   *
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbTaskRunner[] getSelectFromInfoPrepareStatement(
      UpdatedInfo info, boolean orderByStart, int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final List<Filter> filters = new ArrayList<Filter>(3);
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                           org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info)
                                                             .ordinal()));
    filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<=",
                           new Timestamp(System.currentTimeMillis())));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    TransferDAO transferAccess = null;
    List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      if (orderByStart) {
        transfers = transferAccess
            .find(filters, DBTransferDAO.TRANSFER_START_FIELD, true, limit);
      } else {
        transfers = transferAccess.find(filters, limit);
      }
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    final DbTaskRunner[] res = new DbTaskRunner[transfers.size()];
    int i = 0;
    for (final Transfer transfer : transfers) {
      res[i] = new DbTaskRunner(transfer);
      i++;
    }
    return res;
  }

  /**
   * @param session
   *
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInfoPrepareStatement(
      DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table +
        " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
        getLimitWhereCondition() + " AND " + Columns.UPDATEDINFO.name() +
        " = ? ";
    final DbPreparedStatement pstt = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(pstt);
    return pstt;
  }

  /**
   * @param pstt
   * @param info
   * @param time
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(DbPreparedStatement pstt,
                                                    UpdatedInfo info,
                                                    long time) {
    long result = 0;
    try {
      finishSelectOrCountPrepareStatement(pstt, time);
      pstt.getPreparedStatement().setInt(2, info.ordinal());
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // nothing
    } catch (final WaarpDatabaseSqlException ignored) {
      // nothing
    } catch (final SQLException ignored) {
      // nothing
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * @param session
   * @param globalstep
   *
   * @return the DbPreparedStatement for getting Runner according to
   *     globalstep
   *     ordered by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountStepPrepareStatement(
      DbSession session, TASKSTEP globalstep)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    if (globalstep != null) {
      request +=
          " WHERE " + Columns.GLOBALSTEP.name() + " = " + globalstep.ordinal() +
          " AND ";
      request +=
          Columns.STARTTRANS.name() + " >= ? AND " + getLimitWhereCondition();
    } else {
      request += " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
                 getLimitWhereCondition();
    }
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * @param session
   *
   * @return the DbPreparedStatement for getting Runner according to status
   *     ordered by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountStatusPrepareStatement(
      DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    request += " WHERE " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + Columns.INFOSTATUS.name() + " = ? AND " +
               getLimitWhereCondition();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * @param pstt
   * @param error
   * @param time
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(DbPreparedStatement pstt,
                                                    ErrorCode error,
                                                    long time) {
    long result = 0;
    try {
      finishSelectOrCountPrepareStatement(pstt, time);
      pstt.getPreparedStatement().setString(2, error.getCode());
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // ignore
    } catch (final WaarpDatabaseSqlException ignored) {
      // ignore
    } catch (final SQLException ignored) {
      // ignore
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * Only running transfers
   *
   * @param session
   * @param status
   *
   * @return the DbPreparedStatement for getting Runner according to status
   *     ordered by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountStatusRunningPrepareStatement(
      DbSession session, ErrorCode status)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    if (status != null) {
      request +=
          " WHERE " + Columns.STEPSTATUS.name() + " = '" + status.getCode() +
          "' AND " + getLimitWhereCondition();
    } else {
      request += " WHERE " + getLimitWhereCondition();
    }
    request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + Columns.UPDATEDINFO.name() + " = " +
               UpdatedInfo.RUNNING.ordinal();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * Running or not transfers are concerned
   *
   * @param session
   * @param in True for Incoming, False for Outgoing
   *
   * @return the DbPreparedStatement for getting Runner according to in or out
   *     going way and Error
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInOutErrorPrepareStatement(
      DbSession session, boolean in)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    String requesterd;
    final String from = Configuration.configuration.getHostId();
    final String sfrom = Configuration.configuration.getHostSslId();
    if (in) {
      requesterd = Columns.REQUESTED.name();
    } else {
      requesterd = Columns.REQUESTER.name();
    }
    if (from != null && sfrom != null) {
      request +=
          " WHERE ((" + requesterd + " = '" + from + "' OR " + requesterd +
          " = '" + sfrom + "') ";
    } else if (from != null) {
      request += " WHERE (" + requesterd + " = '" + from + "' ";
    } else {
      request += " WHERE (" + requesterd + " = '" + sfrom + "' ";
    }
    request += " AND " + getLimitWhereCondition() + ") ";
    request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + Columns.UPDATEDINFO.name() + " = " +
               UpdatedInfo.INERROR.ordinal();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * Running or not transfers are concerned
   *
   * @param session
   * @param in True for Incoming, False for Outgoing
   * @param running True for Running only, False for all
   *
   * @return the DbPreparedStatement for getting Runner according to in or out
   *     going way
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInOutRunningPrepareStatement(
      DbSession session, boolean in, boolean running)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    String requesterd;
    final String from = Configuration.configuration.getHostId();
    final String sfrom = Configuration.configuration.getHostSslId();
    if (in) {
      requesterd = Columns.REQUESTED.name();
    } else {
      requesterd = Columns.REQUESTER.name();
    }
    if (from != null && sfrom != null) {
      request +=
          " WHERE ((" + requesterd + " = '" + from + "' OR " + requesterd +
          " = '" + sfrom + "') ";
    } else if (from != null) {
      request += " WHERE (" + requesterd + " = '" + from + "' ";
    } else {
      request += " WHERE (" + requesterd + " = '" + sfrom + "' ";
    }
    request += " AND " + getLimitWhereCondition() + ") ";
    request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
    if (running) {
      request += " AND " + Columns.UPDATEDINFO.name() + " = " +
                 UpdatedInfo.RUNNING.ordinal();
    }
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * @param pstt
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(DbPreparedStatement pstt) {
    long result = 0;
    try {
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // ignore
    } catch (final WaarpDatabaseSqlException ignored) {
      // ignore
    } catch (final SQLException ignored) {
      // ignore
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * Set the current time in the given updatedPreparedStatement
   *
   * @param pstt
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static void finishSelectOrCountPrepareStatement(
      DbPreparedStatement pstt, long time)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final Timestamp startlimit = new Timestamp(time);
    try {
      pstt.getPreparedStatement().setTimestamp(1, startlimit);
    } catch (final SQLException e) {
      logger.error("Database SQL Error: Cannot set timestamp", e);
      throw new WaarpDatabaseSqlException("Cannot set timestamp", e);
    }
  }

  /**
   * @param session
   * @param start
   * @param stop
   *
   * @return the DbPreparedStatement for getting Selected Object, whatever
   *     their
   *     status
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getLogPrepareStatement(DbSession session,
                                                           Timestamp start,
                                                           Timestamp stop)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    String request = "SELECT " + selectAllFields + " FROM " + table;
    if (start != null && stop != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
                 Columns.STARTTRANS.name() + " <= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, start);
        preparedStatement.getPreparedStatement().setTimestamp(2, stop);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else if (start != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, start);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else if (stop != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " <= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, stop);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else {
      request += " WHERE " + getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
    }
    return preparedStatement;
  }

  /**
   * purge in same interval all runners with globallaststep as ALLDONETASK or
   * UpdatedInfo as Done
   *
   * @param session
   * @param start
   * @param stop
   *
   * @return the number of log purged
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static int purgeLogPrepareStatement(DbSession session, Timestamp start,
                                             Timestamp stop)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    String request =
        "DELETE FROM " + table + " WHERE (" + Columns.GLOBALLASTSTEP + " = " +
        TASKSTEP.ALLDONETASK.ordinal() + " OR " + Columns.UPDATEDINFO + " = " +
        UpdatedInfo.DONE.ordinal() + ") AND " + getLimitWhereCondition();
    try {
      if (start != null && stop != null) {
        request += " AND " + Columns.STARTTRANS.name() + " >= ? AND " +
                   Columns.STOPTRANS.name() + " <= ? ";
        preparedStatement.createPrepareStatement(request);
        try {
          preparedStatement.getPreparedStatement().setTimestamp(1, start);
          preparedStatement.getPreparedStatement().setTimestamp(2, stop);
        } catch (final SQLException e) {
          preparedStatement.realClose();
          throw new WaarpDatabaseSqlException(e);
        }
      } else if (start != null) {
        request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
        preparedStatement.createPrepareStatement(request);
        try {
          preparedStatement.getPreparedStatement().setTimestamp(1, start);
        } catch (final SQLException e) {
          preparedStatement.realClose();
          throw new WaarpDatabaseSqlException(e);
        }
      } else if (stop != null) {
        request += " AND " + Columns.STOPTRANS.name() + " <= ? ";
        preparedStatement.createPrepareStatement(request);
        try {
          preparedStatement.getPreparedStatement().setTimestamp(1, stop);
        } catch (final SQLException e) {
          preparedStatement.realClose();
          throw new WaarpDatabaseSqlException(e);
        }
      } else {
        preparedStatement.createPrepareStatement(request);
      }
      final int nb = preparedStatement.executeUpdate();
      logger.info("Purge " + nb + " from " + request);
      return nb;
    } finally {
      preparedStatement.realClose();
    }
  }

  /**
   * @param session
   * @param startid
   * @param stopid
   * @param start
   * @param stop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   * @param done
   * @param all
   *
   * @return the DbPreparedStatement according to the filter and ALLDONE,
   *     ERROR
   *     globallaststep
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static int purgeLogPrepareStatement(DbSession session, String startid,
                                             String stopid, Timestamp start,
                                             Timestamp stop, String rule,
                                             String req, boolean pending,
                                             boolean transfer, boolean error,
                                             boolean done, boolean all)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    DbPreparedStatement preparedStatement = new DbPreparedStatement(session);
    final String request = "DELETE FROM " + table;
    String orderby;
    if (startid == null && stopid == null && start == null && stop == null &&
        rule == null && req == null && all) {
      orderby = " WHERE (" + Columns.GLOBALLASTSTEP + " = " +
                TASKSTEP.ALLDONETASK.ordinal() + " OR " + Columns.UPDATEDINFO +
                " = " + UpdatedInfo.DONE.ordinal() + ") AND " +
                getLimitWhereCondition();
    } else {
      if (all) {
        orderby = " AND (" + Columns.GLOBALLASTSTEP + " = " +
                  TASKSTEP.ALLDONETASK.ordinal() + " OR " +
                  Columns.UPDATEDINFO + " = " + UpdatedInfo.DONE.ordinal() +
                  " OR " + Columns.UPDATEDINFO + " = " +
                  UpdatedInfo.INERROR.ordinal() + ") AND " +
                  getLimitWhereCondition();
      } else {
        orderby = " AND " + Columns.UPDATEDINFO + " <> " +
                  UpdatedInfo.RUNNING.ordinal() + " AND " +
                  getLimitWhereCondition();// limit by field
      }
    }
    int nb;
    try {
      preparedStatement =
          getFilterCondition(preparedStatement, request, 0, orderby, startid,
                             stopid, start, stop, rule, req, pending, transfer,
                             error, done, all);
      nb = preparedStatement.executeUpdate();
      logger.info("Purge " + nb + " from " + request);
    } finally {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
    }
    return nb;
  }

  /**
   * Change RUNNING, INTERRUPTED to TOSUBMIT TaskRunner from database. This
   * method is to be used when the
   * commander is starting the very first time, in order to be ready to rerun
   * tasks that are pending.
   *
   * @param session
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public static void resetToSubmit(DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Change RUNNING and INTERRUPTED to TOSUBMIT since they should be ready
    final String request =
        "UPDATE " + table + " SET " + Columns.UPDATEDINFO.name() + '=' +
        AbstractDbData.UpdatedInfo.TOSUBMIT.ordinal() + " WHERE (" +
        Columns.UPDATEDINFO.name() + " = " +
        AbstractDbData.UpdatedInfo.RUNNING.ordinal() + " OR " +
        Columns.UPDATEDINFO.name() + " = " +
        AbstractDbData.UpdatedInfo.INTERRUPTED.ordinal() + ") AND " +
        getLimitWhereCondition();
    final DbPreparedStatement initial = new DbPreparedStatement(session);
    try {
      initial.createPrepareStatement(request);
      initial.executeUpdate();
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.error("Database No Connection Error: Cannot execute Commander", e);
    } catch (final WaarpDatabaseSqlException e) {
      logger.error("Database SQL Error: Cannot execute Commander", e);
    } finally {
      initial.close();
    }
  }

  /**
   * Change CompleteOk+ALLDONETASK to Updated = DONE TaskRunner from database.
   * This method is a clean function
   * to be used for instance before log export or at the very beginning of the
   * commander.
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public static void changeFinishedToDone()
      throws WaarpDatabaseNoConnectionException {
    // Update all UpdatedInfo to DONE where GlobalLastStep = ALLDONETASK and
    // status = CompleteOk
    final List<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "<>",
                           UpdatedInfo.DONE.ordinal()));
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, ">",
                           UpdatedInfo.UNKNOWN.ordinal()));
    filters.add(new Filter(DBTransferDAO.GLOBAL_LAST_STEP_FIELD, "=",
                           Transfer.TASKSTEP.ALLDONETASK.ordinal()));
    filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, ">",
                           ErrorCode.CompleteOk.getCode()));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, ">",
                           Configuration.configuration.getHostId()));

    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      final List<Transfer> transfers = transferAccess.find(filters);
      for (final Transfer transfer : transfers) {
        transfer.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.DONE);
        transferAccess.update(transfer);
      }
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoConnectionException(TRANSFER_NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
  }

  /**
   * Reset the runner (ready to be run again)
   *
   * @return True if OK, False if already finished
   */
  public boolean reset() {
    // Reset the status if already stopped and not finished
    if (getStatus() != ErrorCode.CompleteOk) {
      // restart
      switch (TASKSTEP.values()[getGloballaststep()]) {
        case PRETASK:
          // restart
          setPreTask();
          setExecutionStatus(ErrorCode.InitOk);
          break;
        case TRANSFERTASK:
          // continue
          final int newrank = getRank();
          setTransferTask(newrank);
          setExecutionStatus(ErrorCode.PreProcessingOk);
          break;
        case POSTTASK:
          // restart
          setPostTask();
          setExecutionStatus(ErrorCode.TransferOk);
          break;
        case NOTASK:
          setInitialTask();
          setExecutionStatus(ErrorCode.Unknown);
          break;
        default:
          break;
      }
      changeUpdatedInfo(UpdatedInfo.UNKNOWN);
      setErrorExecutionStatus(pojo.getStepStatus());
      return true;
    } else {
      // Already finished
      return false;
    }
  }

  /**
   * Decrease if necessary the rank
   */
  public void restartRank() {
    if (!pojo.getRetrieveMode()) {
      int newrank = getRank();
      if (newrank > 0) {
        logger.debug(
            "Decrease Rank Restart of -" + Configuration.getRankRestart() +
            " from " + newrank);
        newrank -= Configuration.getRankRestart();
        if (newrank <= 0) {
          newrank = 1;
        }
        if (getRank() != newrank) {
          logger.warn("Decreased Rank Restart at rank: " + newrank + " for {}",
                      this);
        }
      }
      setTransferTask(newrank);
    }
  }

  /**
   * Make this Runner ready for restart
   *
   * @param submit True to resubmit this task, else False to keep it
   *     as
   *     running (only reset)
   *
   * @return True if OK or False if Already finished or if submitted and the
   *     request is a selfRequested and is
   *     not ready to restart locally
   */
  public boolean restart(boolean submit) {
    // Restart if not Requested
    if (submit) {
      if (isSelfRequested() &&
          (pojo.getLastGlobalStep() != Transfer.TASKSTEP.ALLDONETASK ||
           pojo.getLastGlobalStep() != Transfer.TASKSTEP.ERRORTASK)) {
        // nothing
      }
      {
        return false;
      }
    }
    // Restart if already stopped and not finished
    if (reset()) {
      // if not submit and transfertask and receiver AND not requester
      // If requester and receiver => rank is already decreased when request is sent
      if (!submit && pojo.getGlobalStep() == Transfer.TASKSTEP.TRANSFERTASK &&
          !pojo.getRetrieveMode() && isSelfRequested()) {
        logger.debug("Will try to restart transfer {}", this);
        restartRank();
        logger.debug("New restart for transfer is {}", this);
      }
      if (submit) {
        changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
      } else {
        changeUpdatedInfo(UpdatedInfo.RUNNING);
      }
      return true;
    } else {
      // Already finished so DONE
      setAllDone();
      setErrorExecutionStatus(ErrorCode.QueryAlreadyFinished);
      forceSaveStatus();
      return false;
    }
  }

  /**
   * Stop or Cancel a Runner from database point of view
   *
   * @param code
   *
   * @return True if correctly stopped or canceled
   */
  public boolean stopOrCancelRunner(ErrorCode code) {
    if (!isFinished()) {
      reset();
      switch (code) {
        case CanceledTransfer:
        case StoppedTransfer:
        case RemoteShutdown:
          changeUpdatedInfo(UpdatedInfo.INERROR);
          break;
        default:
          changeUpdatedInfo(UpdatedInfo.INTERRUPTED);
      }
      try {
        update();
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot save transfer status", e);
      }
      logger.warn("StopOrCancel: {}     {}", code.getMesg(), toShortString());
      return true;
    } else {
      logger.info("Transfer already finished {}", toShortString());
    }
    return false;
  }

  @Override
  public void changeUpdatedInfo(UpdatedInfo info) {
    setStopNow();
    pojo.setUpdatedInfo(
        org.waarp.openr66.pojo.UpdatedInfo.valueOf(info.ordinal()));
  }

  /**
   * Set the ErrorCode for the InfoStatus
   *
   * @param code
   */
  public void setErrorExecutionStatus(ErrorCode code) {
    setStopNow();
    pojo.setInfoStatus(code);
  }

  /**
   * @return The current UpdatedInfo value
   */
  public UpdatedInfo getUpdatedInfo() {
    return pojo.getUpdatedInfo().getLegacy();
  }

  /**
   * @return the error code associated with the Updated Info
   */
  public ErrorCode getErrorInfo() {
    return pojo.getInfoStatus();
  }

  /**
   * @return the step
   */
  public int getStep() {
    return pojo.getStep();
  }

  /**
   * @return the rescheduledTransfer
   */
  public boolean isRescheduledTransfer() {
    return rescheduledTransfer;
  }

  /**
   * Set this DbTaskRunner as rescheduled (valid only while still in memory)
   */
  public void setRescheduledTransfer() {
    rescheduledTransfer = true;
  }

  /**
   * To set the rank at startup of the request if the request specify a
   * specific
   * rank
   *
   * @param rank the rank to set
   */
  public void setRankAtStartup(int rank) {
    if (pojo.getRank() > rank) {
      pojo.setRank(rank);
    }
  }

  /**
   * @param filename the filename to set
   */
  public void setFilename(String filename) {
    pojo.setFilename(filename);
  }

  /**
   * @param newFilename the new Filename to set
   * @param isFileMoved the isFileMoved to set
   */
  public void setFileMoved(String newFilename, boolean isFileMoved) {
    pojo.setIsMoved(isFileMoved);
    pojo.setFilename(newFilename);
  }

  /**
   * @param originalFilename the originalFilename to set
   */
  public void setOriginalFilename(String originalFilename) {
    pojo.setOriginalName(originalFilename);
  }

  /**
   * @return the rank
   */
  public int getRank() {
    return pojo.getRank();
  }

  /**
   * Change the status from Task Execution
   *
   * @param status
   */
  public void setExecutionStatus(ErrorCode status) {
    pojo.setStepStatus(status);
  }

  /**
   * @return the status
   */
  public ErrorCode getStatus() {
    return pojo.getStepStatus();
  }

  /**
   * @return the isSender
   */
  public boolean isSender() {
    return pojo.getRetrieveMode();
  }

  /**
   * @return the isFileMoved
   */
  public boolean isFileMoved() {
    return pojo.getIsMoved();
  }

  /**
   * @return the blocksize
   */
  public int getBlocksize() {
    return pojo.getBlockSize();
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return pojo.getFilename();
  }

  /**
   * @return the originalFilename
   */
  public String getOriginalFilename() {
    return pojo.getOriginalName();
  }

  /**
   * @return the fileInformation
   */
  public String getFileInformation() {
    return pojo.getFileInfo();
  }

  /**
   * @return the Map<String, Object> for the content of the
   *     transferInformation
   */
  public Map<String, Object> getTransferMap() {
    return JsonHandler.getMapFromString(pojo.getTransferInfo());
  }

  /**
   * @param map the Map to set as XML string to transferInformation
   */
  public void setTransferMap(Map<String, Object> map) {
    setTransferInfo(JsonHandler.writeAsString(map));
  }

  /**
   * Helper to set a new (key, value) in the map Transfer
   *
   * @param key
   * @param value
   */
  public void addToTransferMap(String key, Object value) {
    Map<String, Object> map = getTransferMap();
    map.put(key, value);
    setTransferMap(map);
  }

  /**
   *
   * @param key
   * @return the associated value or null if it does not exist
   */
  public Object getFromTransferMap(String key) {
    return getTransferMap().get(key);
  }

  /**
   * @param size the new size value to set in TransferMap
   */
  private void setOriginalSizeTransferMap(long size) {
    addToTransferMap(JSON_ORIGINALSIZE, size);
  }

  /**
   * @return the size set in TransferMap
   */
  private long getOriginalSizeTransferMap() {
    final Object size = getTransferMap().get(JSON_ORIGINALSIZE);
    if (size == null) {
      return -1;
    }
    if (size instanceof Long) {
      return (Long) size;
    } else {
      return (Integer) size;
    }
  }

  /**
   * Set a new File information for this transfer
   *
   * @param newFileInformation
   */
  public void setFileInformation(String newFileInformation) {
    pojo.setFileInfo(newFileInformation);
  }

  public String getTransferInfo() {
    return pojo.getTransferInfo();
  }

  /**
   * @param transferInformation the transferInformation to set
   */
  private void setTransferInfo(String transferInformation) {
    if (transferInformation == null) {
      transferInformation = "{}";
    }
    pojo.setTransferInfo(transferInformation);
  }

  /**
   * @return the specialId
   */
  public long getSpecialId() {
    return pojo.getId();
  }

  /**
   * @return the rule
   */
  public DbRule getRule() {
    if (rule == null && getRuleId() != null) {
      try {
        rule = new DbRule(getRuleId());
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
    }
    return rule;
  }

  /**
   * @return the ruleId
   */
  public String getRuleId() {
    return pojo.getRule();
  }

  /**
   * @return the mode
   */
  public int getMode() {
    return pojo.getTransferMode();
  }

  /**
   * @return the globalstep
   */
  public TASKSTEP getGlobalStep() {
    return pojo.getGlobalStep().toLegacy();
  }

  /**
   * @return the globalstep
   */
  public TASKSTEP getLastGlobalStep() {
    return pojo.getLastGlobalStep().toLegacy();
  }

  /**
   * @return the globallaststep
   */
  public int getGloballaststep() {
    return pojo.getLastGlobalStep().ordinal();
  }

  /**
   * @return True if this runner is ready for transfer or post operation
   */
  public boolean ready() {
    return pojo.getGlobalStep() != Transfer.TASKSTEP.NOTASK &&
           pojo.getGlobalStep() != Transfer.TASKSTEP.PRETASK;
  }

  /**
   * @return True if the runner is currently in transfer
   */
  public boolean isInTransfer() {
    return pojo.getGlobalStep() == Transfer.TASKSTEP.TRANSFERTASK;
  }

  /**
   * @return True if this runner is finished, either in success or in error
   */
  public boolean isFinished() {
    return isAllDone() || isInError();
  }

  /**
   * @return True if this runner is in error and no more running
   */
  public boolean isInError() {
    return pojo.getGlobalStep() == Transfer.TASKSTEP.ERRORTASK &&
           pojo.getStepStatus() != ErrorCode.Running;
  }

  /**
   * @return True if the runner is finished in success
   */
  public boolean isAllDone() {
    return pojo.getGlobalStep() == Transfer.TASKSTEP.ALLDONETASK;
  }

  /**
   * To be called before executing Pre execution
   *
   * @return True if the task is going to run PRE task from the first action
   */
  public boolean isPreTaskStarting() {
    if (pojo.getLastGlobalStep() == Transfer.TASKSTEP.PRETASK ||
        pojo.getLastGlobalStep() == Transfer.TASKSTEP.NOTASK) {
      return pojo.getStep() - 1 <= 0;
    }
    return false;
  }

  /**
   * Set the Initial Task step (before Pre task)
   */
  public void setInitialTask() {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.NOTASK);
    pojo.setLastGlobalStep(Transfer.TASKSTEP.NOTASK);
    pojo.setStep(-1);
    pojo.setStepStatus(ErrorCode.Running);
    pojo.setInfoStatus(ErrorCode.Unknown);
    pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo
                            .valueOf(UpdatedInfo.RUNNING.ordinal()));
  }

  /**
   * Set Pre Task step
   */
  public void setPreTask() {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.PRETASK);
    pojo.setLastGlobalStep(Transfer.TASKSTEP.PRETASK);
    final int step = pojo.getStep();
    if (step <= 0) {
      pojo.setStep(0);
    } else {
      pojo.setStep(step - 1);
    }
    pojo.setStepStatus(ErrorCode.Running);
    pojo.setInfoStatus(ErrorCode.InitOk);
    pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo
                            .valueOf(UpdatedInfo.RUNNING.ordinal()));
  }

  /**
   * Set Transfer rank
   *
   * @param rank
   */
  public void setTransferTask(int rank) {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.TRANSFERTASK);
    pojo.setLastGlobalStep(Transfer.TASKSTEP.TRANSFERTASK);
    final int lastRank = pojo.getRank();
    if (lastRank > rank) {
      pojo.setRank(rank);
    }
    pojo.setStepStatus(ErrorCode.Running);
    pojo.setInfoStatus(ErrorCode.PreProcessingOk);
  }

  /**
   * Set the Post Task step
   */
  public void setPostTask() {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.POSTTASK);
    pojo.setLastGlobalStep(Transfer.TASKSTEP.POSTTASK);
    final int step = pojo.getStep();
    if (step <= 0) {
      pojo.setStep(0);
    } else {
      pojo.setStep(step - 1);
    }
    pojo.setStepStatus(ErrorCode.Running);
    pojo.setInfoStatus(ErrorCode.TransferOk);
  }

  /**
   * Set the Error Task step
   */
  public void setErrorTask() {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.ERRORTASK);
    pojo.setStep(0);
    pojo.setStepStatus(ErrorCode.Running);
  }

  /**
   * Set the global step as finished (after post task in success)
   */
  public void setAllDone() {
    setStopNow();
    pojo.setGlobalStep(Transfer.TASKSTEP.ALLDONETASK);
    pojo.setLastGlobalStep(Transfer.TASKSTEP.ALLDONETASK);
    pojo.setStep(0);
    pojo.setStepStatus(ErrorCode.CompleteOk);
    pojo.setInfoStatus(ErrorCode.CompleteOk);
    pojo.setUpdatedInfo(
        org.waarp.openr66.pojo.UpdatedInfo.valueOf(UpdatedInfo.DONE.ordinal()));
  }

  /**
   * Set the status of the transfer
   *
   * @param code TransferOk if success
   *
   * @return the current rank of transfer
   */
  public int finishTransferTask(ErrorCode code) {
    setStopNow();
    if (code == ErrorCode.TransferOk) {
      pojo.setStepStatus(code);
      pojo.setInfoStatus(code);
    } else {
      continueTransfer = false;
      final ErrorCode infostatus = pojo.getInfoStatus();
      if (infostatus == ErrorCode.InitOk ||
          infostatus == ErrorCode.PostProcessingOk ||
          infostatus == ErrorCode.PreProcessingOk ||
          infostatus == ErrorCode.Running ||
          infostatus == ErrorCode.TransferOk) {
        pojo.setInfoStatus(code);
      }
      if (!pojo.getUpdatedInfo().equals(UpdatedInfo.INTERRUPTED)) {
        pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo
                                .valueOf(UpdatedInfo.INERROR.ordinal()));
      }
    }
    return pojo.getRank();
  }

  /**
   * @return True if the transfer is valid to continue
   */
  public boolean continueTransfer() {
    return continueTransfer;
  }

  /**
   * Run the task from the given task information (from rule)
   *
   * @param tasks
   *
   * @return The future of the operation (in success or not)
   *
   * @throws OpenR66RunnerEndTasksException
   * @throws OpenR66RunnerErrorException
   */
  private R66Future runNextTask(String[][] tasks)
      throws OpenR66RunnerEndTasksException, OpenR66RunnerErrorException {
    logger.debug((session == null) + ":" +
                 (session == null? "norunner" : session.getRunner() == null) +
                 ':' + toLogRunStep() + ':' + getStep() + ':' +
                 (tasks == null? "null" : tasks.length) + " Sender: " +
                 isSender() + ' ' +
                 rule.printTasks(isSender(), getGlobalStep()));
    if (tasks == null) {
      throw new OpenR66RunnerEndTasksException("No tasks!");
    }
    R66Session tempSession = session;
    if (tempSession == null) {
      tempSession = new R66Session();
      if (tempSession.getRunner() == null) {
        tempSession.setNoSessionRunner(this, localChannelReference);
      }
    } else {
      if (tempSession.getRunner() == null) {
        tempSession
            .setNoSessionRunner(this, tempSession.getLocalChannelReference());
      }
    }
    session = tempSession;
    if (session.getLocalChannelReference().getCurrentCode() ==
        ErrorCode.Unknown) {
      session.getLocalChannelReference()
             .setErrorMessage(getErrorInfo().getMesg(), getErrorInfo());
    }
    if (tasks.length <= getStep()) {
      throw new OpenR66RunnerEndTasksException();
    }
    final AbstractTask task = getTask(tasks[getStep()], tempSession);
    logger.debug(toLogRunStep() + " Task: " + task.getClass().getName());
    task.run();
    task.getFutureCompletion().awaitOrInterruptible();
    if (task.getType() == TaskType.RESCHEDULE) {
      // Special case : must test if exec is OK since it must be the last
      if (isRescheduledTransfer()) {
        throw new OpenR66RunnerEndTasksException();
      }
    }
    setStopNow();
    return task.getFutureCompletion();
  }

  /**
   * @param task
   * @param tempSession
   *
   * @return the corresponding AbstractTask
   *
   * @throws OpenR66RunnerErrorException
   */
  public final AbstractTask getTask(String[] task, R66Session tempSession)
      throws OpenR66RunnerErrorException {
    final String name = task[0];
    final String arg = task[1];
    int delay;
    try {
      delay = Integer.parseInt(task[2]);
    } catch (final NumberFormatException e) {
      logger.warn("Malformed task so stop the execution: " + toShortString());
      throw new OpenR66RunnerErrorException(
          "Malformed task so stop the execution");
    }
    return TaskType.getTaskFromId(name, arg, delay, tempSession);
  }

  /**
   * @return the future of the task run
   *
   * @throws OpenR66RunnerEndTasksException
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66RunnerEndTasksException
   */
  private R66Future runNext()
      throws OpenR66RunnerErrorException, OpenR66RunnerEndTasksException {
    if (rule == null) {
      if (getRuleId() != null) {
        try {
          rule = new DbRule(getRuleId());
        } catch (final WaarpDatabaseException e) {
          rule = null;
        }
      }
      if (rule == null) {
        throw new OpenR66RunnerErrorException("Rule Object not initialized");
      }
    }
    logger.debug(toLogRunStep() + " Sender: " + isSender() + ' ' +
                 rule.printTasks(isSender(), getGlobalStep()));
    switch (getGlobalStep()) {
      case PRETASK:
        try {
          if (isSender()) {
            return runNextTask(rule.getSpreTasksArray());
          } else {
            return runNextTask(rule.getRpreTasksArray());
          }
        } catch (final OpenR66RunnerEndTasksException e) {
          if (getStatus() == ErrorCode.Running) {
            setExecutionStatus(ErrorCode.PreProcessingOk);
            setErrorExecutionStatus(ErrorCode.PreProcessingOk);
          }
          throw e;
        }
      case POSTTASK:
        try {
          if (isSender()) {
            return runNextTask(rule.getSpostTasksArray());
          } else {
            return runNextTask(rule.getRpostTasksArray());
          }
        } catch (final OpenR66RunnerEndTasksException e) {
          if (getStatus() == ErrorCode.Running) {
            setExecutionStatus(ErrorCode.PostProcessingOk);
            setErrorExecutionStatus(ErrorCode.PostProcessingOk);
          }
          throw e;
        }
      case ERRORTASK:
        try {
          if (isSender()) {
            return runNextTask(rule.getSerrorTasksArray());
          } else {
            return runNextTask(rule.getRerrorTasksArray());
          }
        } catch (final OpenR66RunnerEndTasksException e) {
          if (getStatus() == ErrorCode.Running) {
            setExecutionStatus(getErrorInfo());
          }
          throw e;
        }
      default:
        throw new OpenR66RunnerErrorException("Global Step unknown");
    }
  }

  /**
   * Run all task from current status (globalstep and step)
   *
   * @throws OpenR66RunnerErrorException
   */
  public void run() throws OpenR66RunnerErrorException {
    R66Future future;
    try {
      logger.debug(toLogRunStep() + " Status: " + getStatus() + " Sender: " +
                   isSender() + ' ' +
                   rule.printTasks(isSender(), getGlobalStep()));
    } catch (NullPointerException ignored) {
      // Ignored
    }
    if (getStatus() != ErrorCode.Running) {
      throw new OpenR66RunnerErrorException(
          "Current global STEP not ready to run: " + this);
    }
    while (true) {
      logger.debug(toLogRunStep());
      try {
        future = runNext();
      } catch (final OpenR66RunnerEndTasksException e) {
        pojo.setStep(0);
        saveStatus();
        return;
      } catch (final OpenR66RunnerErrorException e) {
        setErrorExecutionStatus(ErrorCode.ExternalOp);
        saveStatus();
        throw new OpenR66RunnerErrorException(
            "Runner is in error: " + e.getMessage(), e);
      }
      if (!future.isDone() || future.isFailed()) {
        final R66Result result = future.getResult();
        if (result != null) {
          setErrorExecutionStatus(future.getResult().getCode());
        } else {
          setErrorExecutionStatus(ErrorCode.ExternalOp);
        }
        saveStatus();
        logger.info("Future is failed: " + getErrorInfo().getMesg());
        if (future.getCause() != null) {
          throw new OpenR66RunnerErrorException(
              "Runner is failed: " + future.getCause().getMessage(),
              future.getCause());
        } else {
          throw new OpenR66RunnerErrorException(
              "Runner is failed: " + getErrorInfo().getMesg());
        }
      }
      pojo.setStep(getStep() + 1);
    }
  }

  /**
   * Once the transfer is over, finalize the Runner by running the error or
   * post
   * operation according to the
   * status.
   *
   * @param localChannelReference
   * @param file
   * @param finalValue
   * @param status
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolSystemException
   */
  public void finalizeTransfer(LocalChannelReference localChannelReference,
                               R66File file, R66Result finalValue,
                               boolean status)
      throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
    logger.debug("status: " + status + ':' + finalValue);

    if (session == null) {
      if (localChannelReference == null) {
        return;
      }
      session = localChannelReference.getSession();
    }
    if (status) {
      // First move the file
      if (isSender()) {
        // Nothing to do since it is the original file
        setPostTask();
        if (!shallIgnoreSave()) {
          saveStatus();
        }
      } else {
        final int poststep = getStep();
        setPostTask();
        saveStatus();
        // in case of error
        final R66Result error =
            new R66Result(session, finalValue.isAnswered(), ErrorCode.FinalOp,
                          this);
        if (!isRecvThrough()) {
          if (getGlobalStep() == TASKSTEP.TRANSFERTASK ||
              getGlobalStep() == TASKSTEP.POSTTASK && poststep == 0) {
            // Result file moves
            final String finalpath = R66Dir.getFinalUniqueFilename(file);
            logger.debug("Will move file {}", finalpath);
            try {
              if (!file.renameTo(getRule().setRecvPath(finalpath))) {
                final OpenR66ProtocolSystemException e =
                    new OpenR66ProtocolSystemException(
                        "Cannot move file to final position");
                final R66Result result =
                    new R66Result(e, session, false, ErrorCode.FinalOp, this);
                result.setFile(file);
                result.setRunner(this);
                if (localChannelReference != null) {
                  localChannelReference.invalidateRequest(result);
                }
                errorTransfer(error, file, localChannelReference);
                throw e;
              }
            } catch (final OpenR66ProtocolSystemException e) {
              final R66Result result =
                  new R66Result(e, session, false, ErrorCode.FinalOp, this);
              result.setFile(file);
              result.setRunner(this);
              if (localChannelReference != null) {
                localChannelReference.invalidateRequest(result);
              }
              errorTransfer(error, file, localChannelReference);
              throw e;
            } catch (final CommandAbstractException e) {
              final R66Result result =
                  new R66Result(new OpenR66RunnerErrorException(e), session,
                                false, ErrorCode.FinalOp, this);
              result.setFile(file);
              result.setRunner(this);
              if (localChannelReference != null) {
                localChannelReference.invalidateRequest(result);
              }
              errorTransfer(error, file, localChannelReference);
              throw (OpenR66RunnerErrorException) result.getException();
            }
            logger.debug("File finally moved: {}", file);
            try {
              setFilename(file.getFile());
            } catch (final CommandAbstractException ignored) {
              // nothing
            }
            // check if possible once more the hash
            String hash = localChannelReference.getHashComputeDuringTransfer();
            if (localChannelReference.isPartialHash()) {
              hash = null; // ignore
            }
            if (hash != null) {
              // we can compute it once more
              try {
                if (!FilesystemBasedDigest.getHex(FilesystemBasedDigest.getHash(
                    file.getTrueFile(), true,
                    Configuration.configuration.getDigest())).equals(hash)) {
                  // KO
                  final R66Result result = new R66Result(
                      new OpenR66RunnerErrorException(
                          "Bad final digest on receive operation"), session,
                      false, ErrorCode.FinalOp, this);
                  result.setFile(file);
                  result.setRunner(this);
                  if (localChannelReference != null) {
                    localChannelReference.invalidateRequest(result);
                  }
                  errorTransfer(error, file, localChannelReference);
                  throw (OpenR66RunnerErrorException) result.getException();
                }
              } catch (final IOException e) {
                final R66Result result = new R66Result(
                    new OpenR66RunnerErrorException(
                        "Bad final digest on receive operation", e), session,
                    false, ErrorCode.FinalOp, this);
                result.setFile(file);
                result.setRunner(this);
                if (localChannelReference != null) {
                  localChannelReference.invalidateRequest(result);
                }
                errorTransfer(error, file, localChannelReference);
                throw (OpenR66RunnerErrorException) result.getException();
              }
            }
          }
        }
      }
      saveStatus();
      if (isRecvThrough() || isSendThrough()) {
        // File could not exist
      } else if (getStep() == 0) {
        // File must exist
        try {
          if (!file.exists()) {
            // error
            final R66Result error =
                new R66Result(session, finalValue.isAnswered(),
                              ErrorCode.FileNotFound, this);
            setErrorExecutionStatus(ErrorCode.FileNotFound);
            errorTransfer(error, file, localChannelReference);
            return;
          }
        } catch (final CommandAbstractException e) {
          // error
          final R66Result error =
              new R66Result(session, finalValue.isAnswered(),
                            ErrorCode.FileNotFound, this);
          setErrorExecutionStatus(ErrorCode.FileNotFound);
          errorTransfer(error, file, localChannelReference);
          return;
        }
      }
      try {
        run();
      } catch (final OpenR66RunnerErrorException e1) {
        final R66Result result =
            new R66Result(e1, session, false, ErrorCode.ExternalOp, this);
        result.setFile(file);
        result.setRunner(this);
        changeUpdatedInfo(UpdatedInfo.INERROR);
        saveStatus();
        errorTransfer(result, file, localChannelReference);
        if (localChannelReference != null) {
          localChannelReference.invalidateRequest(result);
        }
        throw e1;
      }
      saveStatus();
      /*
       * Done later on after EndRequest this.setAllDone() this.saveStatus()
       */
      logger.info("Transfer done on {} at RANK {}",
                  file != null? file : "no file", getRank());
      if (localChannelReference != null) {
        localChannelReference.validateEndTransfer(finalValue);
      }
    } else {
      logger.debug(
          "ContinueTransfer: " + continueTransfer + " status:" + status + ':' +
          finalValue);
      errorTransfer(finalValue, file, localChannelReference);
    }
  }

  /**
   * Finalize a transfer in error
   *
   * @param finalValue
   * @param file
   * @param localChannelReference
   *
   * @throws OpenR66RunnerErrorException
   */
  private void errorTransfer(R66Result finalValue, R66File file,
                             LocalChannelReference localChannelReference)
      throws OpenR66RunnerErrorException {
    // error or not ?
    final ErrorCode runnerStatus = getErrorInfo();
    if (finalValue.getException() != null) {
      logger.error("Transfer KO on " + file + " due to " +
                   finalValue.getException().getMessage());
    } else {
      logger.error("Transfer KO on " + file + " due to " + finalValue);
    }
    if (runnerStatus == ErrorCode.CanceledTransfer) {
      // delete file, reset runner
      setRankAtStartup(0);
      deleteTempFile();
      changeUpdatedInfo(UpdatedInfo.INERROR);
      saveStatus();
      finalValue.setAnswered(true);
    } else if (runnerStatus == ErrorCode.StoppedTransfer &&
               runnerStatus == ErrorCode.Shutdown) {
      // just save runner and stop
      changeUpdatedInfo(UpdatedInfo.INERROR);
      saveStatus();
      finalValue.setAnswered(true);
    }
    logger.debug("status: " + getStatus() + " wasNotError:" +
                 (getGlobalStep() != TASKSTEP.ERRORTASK) + ':' + finalValue);
    if (getGlobalStep() != TASKSTEP.ERRORTASK) {
      // errorstep was not already executed
      // real error
      if (localChannelReference != null && finalValue != null) {
        localChannelReference
            .setErrorMessage(finalValue.getMessage(), finalValue.getCode());
      }
      // First send error mesg
      if (!finalValue.isAnswered()) {
        localChannelReference.sessionNewState(R66FiniteDualStates.ERROR);
        final ErrorPacket errorPacket = new ErrorPacket(finalValue.getMessage(),
                                                        finalValue.getCode()
                                                                  .getCode(),
                                                        ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, errorPacket,
                                        true);
          finalValue.setAnswered(true);
        } catch (final OpenR66ProtocolPacketException e1) {
          // should not be
        }
      }
      // now run error task
      setErrorTask();
      saveStatus();
      try {
        run();
      } catch (final OpenR66RunnerErrorException e1) {
        changeUpdatedInfo(UpdatedInfo.INERROR);
        setErrorExecutionStatus(runnerStatus);
        saveStatus();
        if (localChannelReference != null) {
          localChannelReference.invalidateRequest(finalValue);
        }
        throw e1;
      }
    }
    if (!isRescheduledTransfer()) {
      changeUpdatedInfo(UpdatedInfo.INERROR);
    }
    RequestPacket.isThroughMode(getMode());
    // re set the original status
    setErrorExecutionStatus(runnerStatus);
    saveStatus();
    if (localChannelReference != null) {
      localChannelReference.invalidateRequest(finalValue);
    }
  }

  /**
   * Increment the rank of the transfer
   */
  public void incrementRank() {
    pojo.setRank(getRank() + 1);
    int modulo = 10;
    if (!admin.isCompatibleWithThreadSharedConnexion()) {
      modulo =
          100; // Bug in JDBC MariaDB/MySQL which tends to consume more memory
    }
    if (getRank() % modulo == 0) {
      // Save each 10 blocks
      try {
        update();
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot update Runner: {}", e.getMessage());
      }
    }
  }

  /**
   * This method is to be called each time an operation is happening on Runner
   *
   * @throws OpenR66RunnerErrorException
   */
  public void saveStatus() throws OpenR66RunnerErrorException {
    try {
      update();
    } catch (final WaarpDatabaseException e) {
      throw new OpenR66RunnerErrorException(e);
    }
  }

  /**
   * This method is to be called each time an operation is happening on Runner
   * and it is forced (for SelfRequest
   * handling)
   *
   * @return True if saved
   *
   * @throws OpenR66RunnerErrorException
   */
  public boolean forceSaveStatus() {
    final boolean isSender = isSender();
    setSenderForUpdate();
    boolean status = true;
    try {
      saveStatus();
    } catch (final OpenR66RunnerErrorException e) {
      status = false;
    }
    setSender(isSender);
    return status;
  }

  /**
   * Clear the runner
   */
  public void clear() {
    // ignore
  }

  /**
   * Delete the temporary empty file (retrieved file at rank 0)
   */
  public void deleteTempFile() {
    if (!isSender() && getRank() == 0) {
      try {
        if (session != null) {
          final R66File file = session.getFile();
          if (file != null) {
            file.delete();
          }
        }
      } catch (final CommandAbstractException e1) {
        logger.warn("Cannot delete temporary empty file", e1);
      }
    }
  }

  @Override
  public String toString() {
    return "Run: '" + (rule != null? rule.toString() : getRuleId()) +
           "' Filename: '" + getFilename() + "', STEP: '" + getGlobalStep() +
           '(' + getLastGlobalStep() + "):" + getStep() + ':' +
           getStatus().getMesg() + "', TransferRank: " + getRank() +
           ", Blocksize: " + getBlocksize() + ", SpecialId: " + getSpecialId() +
           ", isSender: " + isSender() + ", isMoved: " + isFileMoved() +
           ", Mode: '" + getMode() + "', Requester: '" + getRequester() +
           "', Requested: '" + getRequested() + "', Start: '" + getStart() +
           "', Stop: '" + getStop() + "', Internal: '" +
           getUpdatedInfo().name() + ':' + getErrorInfo().getMesg() +
           "', OriginalSize: " + originalSize + ", Fileinfo: '" +
           getFileInformation() + "', Transferinfo: '" + getTransferInfo() +
           '\'';
  }

  public String toLogRunStep() {
    return "Run: " + getRuleId() + " on " + getFilename() + " STEP: " +
           getGlobalStep() + '(' + getLastGlobalStep() + "):" + getStep() +
           ':' + getStatus().getMesg();
  }

  public String toShortNoHtmlString(String newline) {
    return "{Run: '" + getRuleId() + "', Filename: '" + getFilename() + "'," +
           newline + " STEP: '" + getGlobalStep() + '(' + getLastGlobalStep() +
           "):" + getStep() + ':' + getStatus().getMesg() + "'," + newline +
           " TransferRank: " + getRank() + ", Blocksize: " + getBlocksize() +
           ", SpecialId: " + getSpecialId() + ", isSender: '" + isSender() +
           "', isMoved: '" + isFileMoved() + "', Mode: '" +
           TRANSFERMODE.values()[getMode()] + newline + "', Requester: '" +
           getRequester() + "', Requested: '" + getRequested() + "', Start: '" +
           getStart() + "', Stop: '" + getStop() + "'," + newline +
           " Internal: '" + getUpdatedInfo().name() + ':' +
           getErrorInfo().getMesg() + "', OriginalSize: " + originalSize + ',' +
           newline + " Fileinfo: '" + getFileInformation() +
           "', Transferinfo: '" + getTransferInfo() + "'}";
  }

  public String toShortString() {
    return "<RULE>" + getRuleId() + "</RULE><ID>" + getSpecialId() +
           "</ID><FILE>" + getFilename() + "</FILE>     <STEP>" +
           getGlobalStep() + '(' + getLastGlobalStep() + "):" + getStep() +
           ':' + getStatus().getMesg() + "</STEP><RANK>" + getRank() +
           "</RANK><BLOCKSIZE>" + getBlocksize() + "</BLOCKSIZE>     <SENDER>" +
           isSender() + "</SENDER><MOVED>" + isFileMoved() + "</MOVED><MODE>" +
           TRANSFERMODE.values()[getMode()] + "</MODE>     <REQR>" +
           getRequester() + "</REQR><REQD>" + getRequested() +
           "</REQD>     <START>" + getStart() + "</START><STOP>" + getStop() +
           "</STOP>     <INTERNAL>" + getUpdatedInfo().name() + " : " +
           getErrorInfo().getMesg() + "</INTERNAL><ORIGINALSIZE>" +
           originalSize + "</ORIGINALSIZE>     <FILEINFO>" +
           getFileInformation() + "</FILEINFO> <TRANSFERINFO>" +
           getTransferInfo() + "</TRANSFERINFO>";
  }

  /**
   * @return the header for a table of runners in Html format
   */
  public static String headerHtml() {
    return "<td>SpecialId</td><td>Rule</td><td>Filename</td><td>Info" +
           "</td><td>Step (LastStep)</td><td>Action</td><td>Status" +
           "</td><td>Internal</t><td>Transfer Rank</td><td>BlockSize</td><td>isMoved" +
           "</td><td>Requester</td><td>Requested" +
           "</td><td>Start</td><td>Stop</td><td>Bandwidth (Mbits)</td><td>Free Space(MB)</td>";
  }

  /**
   * @param session
   *
   * @return The associated freespace of the current directory (in MB)
   */
  public long freespaceMB(R66Session session) {
    if (getLastGlobalStep() == TASKSTEP.ALLDONETASK ||
        getLastGlobalStep() == TASKSTEP.POSTTASK) {
      // All finished or Post task
      return freespace(session, false) / 0x100000L;
    } else {
      // are we in sending or receive
      return freespace(session, true) / 0x100000L;
    }
  }

  /**
   * @param session
   * @param isWorkingPath
   *
   * @return The associated freespace of the directory (Working if True, Recv
   *     if
   *     False) (in B, not MB)
   */
  public long freespace(R66Session session, boolean isWorkingPath) {
    long freespace = -1;
    DbRule dbRule = null;
    try {
      dbRule = this.rule != null? this.rule : new DbRule(getRuleId());
    } catch (final WaarpDatabaseException ignored) {
      // ignore
    }
    if (this.rule == null) {
      this.rule = dbRule;
    }
    if (dbRule != null && !isSender()) {
      try {
        String sdir;
        if (isWorkingPath) {
          sdir = dbRule.getWorkPath();
        } else {
          sdir = dbRule.getRecvPath();
        }
        R66Dir dir;
        if (session.getDirsFromSession().containsKey(sdir)) {
          dir = session.getDirsFromSession().get(sdir);
        } else {
          dir = new R66Dir(session);
          dir.changeDirectory(sdir);
          session.getDirsFromSession().put(sdir, dir);
        }
        freespace = dir.getFreeSpace();
      } catch (final CommandAbstractException e) {
        logger.warn("Error while freespace compute {}", e.getMessage(), e);
      }
    }
    return freespace;
  }

  private String bandwidthMB() {
    final double drank = getRank() <= 0? 1 : getRank();
    final double dblocksize = getBlocksize();
    final double size = drank * dblocksize;
    final double time = getStop().getTime() + 1 - getStart().getTime();
    final double result = size / time / 0x100000L * 1000;
    return String.format("%,.2f", result);
  }

  private String getHtmlColor() {
    String color;
    switch (getGlobalStep()) {
      case NOTASK:
        color = "Orange";
        break;
      case PRETASK:
        color = "Yellow";
        break;
      case TRANSFERTASK:
        color = "LightGreen";
        break;
      case POSTTASK:
        color = "Turquoise";
        break;
      case ERRORTASK:
        color = "Red";
        break;
      case ALLDONETASK:
        color = "Cyan";
        break;
      default:
        color = "";
    }
    return color;
  }

  private String getInfoHtmlColor() {
    String color;
    switch (getUpdatedInfo()) {
      case DONE:
        color = "Cyan";
        break;
      case INERROR:
        color = "Red";
        break;
      case INTERRUPTED:
        color = "Orange";
        break;
      case NOTUPDATED:
        color = "Yellow";
        break;
      case RUNNING:
        color = "LightGreen";
        break;
      case TOSUBMIT:
      case UNKNOWN:
        color = "Turquoise";
        break;
      default:
        color = "";
    }
    return color;
  }

  /**
   * @param session
   * @param running special info
   *
   * @return the runner in Html format compatible with the header from
   *     headerHtml method
   */
  public String toHtml(R66Session session, String running) {
    final long freespace = freespaceMB(session);
    final String color = getHtmlColor();
    final String updcolor = getInfoHtmlColor();
    return "<td>" + getSpecialId() + "</td><td>" +
           (rule != null? rule.toShortString() : getRuleId()) + "</td><td>" +
           getFilename() + "</td><td>" + getFileInformation() + '[' +
           getTransferInfo() + ']' + "</td><td bgcolor=\"" + color + "\">" +
           getGlobalStep() + " (" + getGloballaststep() + ")</td><td>" +
           getStep() + "</td><td>" + getStatus().getMesg() + " <b>" + running +
           "</b></td><td bgcolor=\"" + updcolor + "\">" +
           getUpdatedInfo().name() + " : " + getErrorInfo().getMesg() +
           "</td><td>" + getRank() + "</td><td>" + getBlocksize() +
           "</td><td>" + isFileMoved() + "</td><td>" + getRequester() +
           "</td><td>" + getRequested() + "</td><td>" + getStart() +
           "</td><td>" + getStop() + "</td><td>" + bandwidthMB() + "</td>" +
           "<td>" + freespace + "</td>";
  }

  /**
   * @param session
   * @param body
   * @param running special info
   *
   * @return the runner in Html format specified by body by replacing all
   *     instance of fields
   */
  public String toSpecializedHtml(R66Session session, String body,
                                  String running) {
    final long freespace = freespaceMB(session);
    final StringBuilder builder = new StringBuilder(body);
    WaarpStringUtils
        .replaceAll(builder, "XXXSpecIdXXX", Long.toString(getSpecialId()));
    WaarpStringUtils.replace(builder, "XXXRulXXX",
                             rule != null? rule.toShortString() : getRuleId());
    WaarpStringUtils.replace(builder, "XXXFileXXX", getFilename());
    WaarpStringUtils.replace(builder, "XXXInfoXXX", getFileInformation());
    WaarpStringUtils.replace(builder, "XXXTransXXX", pojo.getFileInfo());
    WaarpStringUtils.replace(builder, "XXXStepXXX",
                             getGlobalStep() + " (" + getGloballaststep() +
                             ')');
    WaarpStringUtils.replace(builder, "XXXCOLXXX", getHtmlColor());
    WaarpStringUtils.replace(builder, "XXXActXXX", Integer.toString(getStep()));
    WaarpStringUtils
        .replace(builder, "XXXStatXXX", pojo.getStepStatus().getMesg());
    WaarpStringUtils.replace(builder, "XXXRunningXXX", running);
    WaarpStringUtils.replace(builder, "XXXInternXXX",
                             getUpdatedInfo().name() + " : " +
                             getErrorInfo().getMesg());
    WaarpStringUtils.replace(builder, "XXXUPDCOLXXX", getInfoHtmlColor());
    WaarpStringUtils.replace(builder, "XXXBloXXX", Integer.toString(getRank()));
    WaarpStringUtils
        .replace(builder, "XXXisSendXXX", Boolean.toString(isSender()));
    WaarpStringUtils
        .replace(builder, "XXXisMovXXX", Boolean.toString(isFileMoved()));
    WaarpStringUtils.replace(builder, "XXXModXXX",
                             TRANSFERMODE.values()[getMode()].toString());
    WaarpStringUtils.replaceAll(builder, "XXXReqrXXX", getRequester());
    WaarpStringUtils.replaceAll(builder, "XXXReqdXXX", getRequested());
    WaarpStringUtils.replace(builder, "XXXStarXXX", getStart().toString());
    WaarpStringUtils.replace(builder, "XXXStopXXX", getStop().toString());
    WaarpStringUtils.replace(builder, "XXXBandXXX", bandwidthMB());
    WaarpStringUtils.replace(builder, "XXXFreeXXX", Long.toString(freespace));
    return builder.toString();
  }

  /**
   * @return True if the current host is the requested host (to prevent
   *     request
   *     to itself)
   */
  public boolean isSelfRequested() {
    if (pojo.getRequested().equals(Configuration.configuration.getHostId()) ||
        pojo.getRequested()
            .equals(Configuration.configuration.getHostSslId())) {
      // check if not calling itself
      return !pojo.getRequester()
                  .equals(Configuration.configuration.getHostId()) &&
             !pojo.getRequester()
                  .equals(Configuration.configuration.getHostSslId());
    }
    return false;
  }

  /**
   * @return True if this is a self request and current action is on Requested
   */
  public boolean shallIgnoreSave() {
    return isSelfRequest() && (isSender() && getRule().isSendMode() ||
                               !isSender() && getRule().isRecvMode());
  }

  /**
   * @return True if the request is a self request (same host on both side)
   */
  public boolean isSelfRequest() {
    return
        (pojo.getRequested().equals(Configuration.configuration.getHostId()) ||
         pojo.getRequested()
             .equals(Configuration.configuration.getHostSslId())) &&
        (pojo.getRequester().equals(Configuration.configuration.getHostId()) ||
         pojo.getRequester()
             .equals(Configuration.configuration.getHostSslId()));
  }

  /**
   * @return the requested HostId
   */
  public String getRequested() {
    return pojo.getRequested();
  }

  /**
   * @return the requester HostId
   */
  public String getRequester() {
    return pojo.getRequester();
  }

  /**
   * @return the start
   */
  public Timestamp getStart() {
    return pojo.getStart();
  }

  /**
   * @param start new Start time to apply when reschedule
   */
  public void setStart(Timestamp start) {
    pojo.setStart(start);
  }

  /**
   * @return the stop
   */
  public Timestamp getStop() {
    return pojo.getStop();
  }

  public void setStop(Timestamp stop) {
    pojo.setStop(stop);
  }

  /**
   * @return the associated request
   */
  public RequestPacket getRequest() {
    String sep;
    if (pojo.getRequested().equals(Configuration.configuration.getHostId()) ||
        pojo.getRequested()
            .equals(Configuration.configuration.getHostSslId())) {
      sep = PartnerConfiguration.getSeparator(pojo.getRequester());
    } else {
      sep = PartnerConfiguration.getSeparator(pojo.getRequested());
    }
    return new RequestPacket(pojo.getRule(), pojo.getTransferMode(),
                             pojo.getOriginalName(), pojo.getBlockSize(),
                             pojo.getRank(), pojo.getId(), pojo.getFileInfo(),
                             originalSize, sep);
  }

  /**
   * Used internally
   *
   * @return a Key representing the primary key as a unique string
   */
  public String getKey() {
    return pojo.getRequested() + ' ' + pojo.getRequester() + ' ' + pojo.getId();
  }

  /**
   * Construct a new Element with value
   *
   * @param name
   * @param value
   *
   * @return the new Element
   */
  private static Element newElement(String name, String value) {
    final Element node = new DefaultElement(name);
    if (value != null) {
      node.addText(value);
    }
    return node;
  }

  private static Object getValue(DbTaskRunner runner, String field) {
    Columns column = Columns.valueOf(field.toUpperCase());
    switch (column) {
      case GLOBALSTEP:
        return runner.pojo.getGlobalStep().ordinal();
      case GLOBALLASTSTEP:
        return runner.pojo.getLastGlobalStep().ordinal();
      case STEP:
        return runner.pojo.getStep();
      case RANK:
        return runner.pojo.getRank();
      case STEPSTATUS:
        return runner.pojo.getStepStatus().getCode();
      case RETRIEVEMODE:
        return runner.pojo.getRetrieveMode();
      case FILENAME:
        return runner.pojo.getFilename();
      case ISMOVED:
        return runner.pojo.getIsMoved();
      case IDRULE:
        return runner.pojo.getRule();
      case BLOCKSZ:
        return runner.pojo.getBlockSize();
      case ORIGINALNAME:
        return runner.pojo.getOriginalName();
      case FILEINFO:
        return runner.pojo.getFileInfo();
      case TRANSFERINFO:
        return runner.pojo.getTransferInfo();
      case MODETRANS:
        return runner.pojo.getTransferMode();
      case STARTTRANS:
        return runner.pojo.getStart();
      case STOPTRANS:
        return runner.pojo.getStop();
      case INFOSTATUS:
        return runner.pojo.getInfoStatus().getCode();
      case UPDATEDINFO:
        return runner.pojo.getUpdatedInfo().ordinal();
      case OWNERREQ:
        return runner.pojo.getOwnerRequest();
      case REQUESTER:
        return runner.pojo.getRequester();
      case REQUESTED:
        return runner.pojo.getRequested();
      case SPECIALID:
        return runner.pojo.getId();
      default:
        throw new IllegalArgumentException("Field unknown: " + column.name());
    }
  }

  /**
   * Need to call 'setToArray' before
   *
   * @param runner
   *
   * @return The Element representing the given Runner
   *
   * @throws WaarpDatabaseSqlException
   */
  private static Element getElementFromRunner(DbTaskRunner runner)
      throws WaarpDatabaseSqlException {
    final Element root = new DefaultElement(XMLRUNNER);
    for (Columns column : Columns.values()) {
      if (column.name().equals(Columns.UPDATEDINFO.name()) ||
          column.name().equals(Columns.TRANSFERINFO.name())) {
        continue;
      }
      root.add(newElement(column.name().toLowerCase(),
                          getValue(runner, column.name()).toString()));
    }
    return root;
  }

  /**
   * Set the given runner from the root element of the runner itself
   * (XMLRUNNER
   * but not XMLRUNNERS). Need to
   * call 'setFromArray' after.
   *
   * @param runner
   * @param root
   *
   * @throws WaarpDatabaseSqlException
   */
  private static void setRunnerFromElement(DbTaskRunner runner, Element root)
      throws WaarpDatabaseSqlException {
    setRunnerFromElementNoException(runner, root);
  }

  /**
   * Write the selected TaskRunners from PrepareStatement to a XMLWriter
   *
   * @param preparedStatement ready to be executed
   * @param xmlWriter
   *
   * @return the NbAndSpecialId for the number of transfer and higher rank
   *     found
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   * @throws OpenR66ProtocolBusinessException
   */
  public static NbAndSpecialId writeXML(DbPreparedStatement preparedStatement,
                                        XMLWriter xmlWriter)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    final Element root = new DefaultElement(XMLRUNNERS);
    final NbAndSpecialId nbAndSpecialId = new NbAndSpecialId();
    try {
      xmlWriter.writeOpen(root);
      Element node;
      while (preparedStatement.getNext()) {
        final DbTaskRunner runner = getFromStatement(preparedStatement);
        if (nbAndSpecialId.higherSpecialId < runner.getSpecialId()) {
          nbAndSpecialId.higherSpecialId = runner.getSpecialId();
        }
        node = getElementFromRunner(runner);
        xmlWriter.write(node);
        xmlWriter.flush();
        nbAndSpecialId.nb++;
      }
      xmlWriter.writeClose(root);
    } catch (final IOException e) {
      logger.error("Cannot write XML file", e);
      throw new OpenR66ProtocolBusinessException(
          "Cannot write file: " + e.getMessage());
    }
    return nbAndSpecialId;
  }

  /**
   * Write selected TaskRunners to a Json String
   *
   * @param preparedStatement
   *
   * @return the associated Json String
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   * @throws OpenR66ProtocolBusinessException
   */
  public static String getJson(DbPreparedStatement preparedStatement, int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    final ArrayNode arrayNode = JsonHandler.createArrayNode();
    try {
      preparedStatement.executeQuery();
      final LocalTransaction localTransaction =
          Configuration.configuration.getLocalTransaction();
      int nb = 0;
      while (preparedStatement.getNext()) {
        final DbTaskRunner runner = getFromStatement(preparedStatement);
        final ObjectNode node = runner.getJson();
        node.put(Columns.SPECIALID.name(),
                 Long.toString(runner.getSpecialId()));
        if (localTransaction == null) {
          node.put("Running", false);
        } else {
          node.put("Running", localTransaction.contained(runner.getKey()));
        }
        arrayNode.add(node);
        nb++;
        if (nb >= limit) {
          break;
        }
      }
    } finally {
      preparedStatement.realClose();
    }
    return WaarpStringUtils.cleanJsonForHtml(
        arrayNode.toString().replaceAll("(\\\"\\{)([^}]+)(\\}\\\")", "{$2}")
                 .replaceAll("([^\\\\])(\\\\\")([a-zA-Z_0-9]+)(\\\\\")",
                             "$1\"$3\""));
  }

  /**
   * Write selected TaskRunners to an XML file using an XMLWriter
   *
   * @param preparedStatement
   * @param filename
   *
   * @return the NbAndSpecialId for the number of transfer and higher rank
   *     found
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   * @throws OpenR66ProtocolBusinessException
   */
  public static NbAndSpecialId writeXMLWriter(
      DbPreparedStatement preparedStatement, String filename)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    NbAndSpecialId nbAndSpecialId;
    OutputStream outputStream = null;
    XMLWriter xmlWriter = null;
    boolean isOk = false;
    try {
      outputStream = new FileOutputStream(filename);
      final OutputFormat format = OutputFormat.createPrettyPrint();
      format.setEncoding(WaarpStringUtils.UTF_8);
      xmlWriter = new XMLWriter(outputStream, format);
      preparedStatement.executeQuery();
      nbAndSpecialId = writeXML(preparedStatement, xmlWriter);
      isOk = true;
    } catch (final FileNotFoundException e) {
      logger.error("Cannot write XML file", e);
      throw new OpenR66ProtocolBusinessException("File not found");
    } catch (final UnsupportedEncodingException e) {
      logger.error("Cannot write XML file", e);
      throw new OpenR66ProtocolBusinessException("Unsupported Encoding");
    } finally {
      if (xmlWriter != null) {
        try {
          xmlWriter.endDocument();
          xmlWriter.flush();
          xmlWriter.close();
        } catch (final SAXException e) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
          logger.error("Cannot write XML file", e);
          throw new OpenR66ProtocolBusinessException("Unsupported Encoding");
        } catch (final IOException e) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
          logger.error("Cannot write XML file", e);
          throw new OpenR66ProtocolBusinessException("Unsupported Encoding");
        }
        if (!isOk && outputStream != null) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
        }
      } else if (outputStream != null) {
        FileUtils.close(outputStream);
        final File file = new File(filename);
        file.delete();
      }
    }
    return nbAndSpecialId;
  }

  /**
   * Write all TaskRunners to an XML file using an XMLWriter
   *
   * @param filename
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   * @throws OpenR66ProtocolBusinessException
   */
  public static void writeXMLWriter(String filename)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    final String request =
        "SELECT " + selectAllFields + " FROM " + table + " WHERE " +
        getLimitWhereCondition();
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = new DbPreparedStatement(admin.getSession());
      preparedStatement.createPrepareStatement(request);
      writeXMLWriter(preparedStatement, filename);
    } finally {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
    }
  }

  /**
   * @return the backend XML filename for the current TaskRunner in NoDb
   *     Client mode
   */
  public String backendXmlFilename() {
    return Configuration.configuration.getBaseDirectory() +
           Configuration.configuration.getArchivePath() +
           DirInterface.SEPARATOR + pojo.getRequester() + '_' +
           pojo.getRequested() + '_' + pojo.getId() + XMLEXTENSION;
  }

  /**
   * @return the runner as XML
   *
   * @throws OpenR66ProtocolBusinessException
   */
  public String asXML() throws OpenR66ProtocolBusinessException {
    Element node;
    try {
      node = getElementFromRunner(this);
    } catch (final WaarpDatabaseSqlException e) {
      logger.error("Cannot read Data", e);
      throw new OpenR66ProtocolBusinessException(
          "Cannot read Data: " + e.getMessage());
    }
    return node.asXML();
  }

  @Override
  public ObjectNode getJson() {
    final ObjectNode node = super.getJson();
    if (rescheduledTransfer) {
      node.put(JSON_RESCHEDULE, true);
    }
    if (isRecvThrough || isSendThrough) {
      node.put(JSON_THROUGHMODE, true);
    }
    node.put(JSON_ORIGINALSIZE, originalSize);
    return node;
  }

  @Override
  protected void setFromJson(final String field, final JsonNode value) {
    if (value == null) {
      return;
    }
    for (Columns column : Columns.values()) {
      if (column.name().equalsIgnoreCase(field)) {
        switch (column) {
          case BLOCKSZ:
            pojo.setBlockSize(value.asInt());
            break;
          case FILEINFO:
            pojo.setFileInfo(value.asText());
            break;
          case FILENAME:
            pojo.setFilename(value.asText());
            break;
          case GLOBALLASTSTEP:
            pojo.setLastGlobalStep(Transfer.TASKSTEP.valueOf(value.asInt()));
            break;
          case GLOBALSTEP:
            pojo.setGlobalStep(Transfer.TASKSTEP.valueOf(value.asInt()));
            break;
          case IDRULE:
            pojo.setRule(value.asText());
            break;
          case INFOSTATUS:
            pojo.setInfoStatus(ErrorCode.getFromCode(value.asText()));
            break;
          case ISMOVED:
            pojo.setIsMoved(value.asBoolean());
            break;
          case MODETRANS:
            pojo.setTransferMode(value.asInt());
            break;
          case ORIGINALNAME:
            pojo.setOriginalName(value.asText());
            break;
          case OWNERREQ:
            String owner = value.asText();
            if (owner == null || owner.isEmpty()) {
              owner = Configuration.configuration.getHostId();
            }
            pojo.setOwnerRequest(owner);
            break;
          case RANK:
            pojo.setRank(value.asInt());
            break;
          case REQUESTED:
            pojo.setRequested(value.asText());
            break;
          case REQUESTER:
            pojo.setRequester(value.asText());
            break;
          case RETRIEVEMODE:
            pojo.setRetrieveMode(value.asBoolean());
            break;
          case SPECIALID:
            pojo.setId(value.asLong());
            break;
          case STARTTRANS:
            long start = value.asLong();
            if (start == 0) {
              start = System.currentTimeMillis();
            }
            pojo.setStart(new Timestamp(start));
            break;
          case STEP:
            pojo.setStep(value.asInt());
            break;
          case STEPSTATUS:
            pojo.setStepStatus(ErrorCode.getFromCode(value.asText()));
            break;
          case STOPTRANS:
            long stop = value.asLong();
            if (stop == 0) {
              stop = System.currentTimeMillis();
            }
            pojo.setStop(new Timestamp(stop));
            break;
          case TRANSFERINFO:
            pojo.setTransferInfo(value.asText());
            break;
          case UPDATEDINFO:
            pojo.setUpdatedInfo(
                org.waarp.openr66.pojo.UpdatedInfo.valueOf(value.asInt()));
            break;
        }
      }
    }
  }

  /**
   * @return the Json string for this
   */
  public String getJsonAsString() {
    final ObjectNode node = getJson();
    node.put(Columns.SPECIALID.name(), Long.toString(pojo.getId()));
    final LocalTransaction localTransaction =
        Configuration.configuration.getLocalTransaction();
    if (localTransaction == null) {
      node.put("Running", false);
    } else {
      node.put("Running", localTransaction.contained(getKey()));
    }
    return WaarpStringUtils.cleanJsonForHtml(JsonHandler.writeAsString(node));
  }

  /**
   * Set the given runner from the root element of the runner itself
   * (XMLRUNNER
   * but not XMLRUNNERS). Need to
   * call 'setFromArray' after.
   *
   * @param runner
   * @param root
   */
  private static void setRunnerFromElementNoException(DbTaskRunner runner,
                                                      Element root) {
    ObjectNode node = JsonHandler.createObjectNode();
    for (Columns column : Columns.values()) {
      if (column.name().equals(Columns.UPDATEDINFO.name()) ||
          column.name().equals(Columns.TRANSFERINFO.name())) {
        continue;
      }
      final Element elt =
          (Element) root.selectSingleNode(column.name().toLowerCase());
      if (elt != null) {
        final String newValue = elt.getText();
        node.put(column.name(), newValue);
        runner.setFromJson(column.name(), node.findValue(column.name()));
        node.removeAll();
      }
    }
    runner.pojo.setTransferInfo("{}");
  }

  /**
   * Reload a to submitted runner from a remote partner's log (so reversing
   * isSender should be true)
   *
   * @param xml
   * @param reverse
   *
   * @return the TaskRunner from the XML source element
   *
   * @throws OpenR66ProtocolBusinessException
   */
  public static DbTaskRunner fromStringXml(String xml, boolean reverse)
      throws OpenR66ProtocolBusinessException {
    Document document;
    try {
      document = DocumentHelper.parseText(xml);
    } catch (final DocumentException e1) {
      logger.warn("Cant parse XML", e1);
      throw new OpenR66ProtocolBusinessException("Cannot parse the XML input");
    }
    final DbTaskRunner runner = new DbTaskRunner();
    setRunnerFromElementNoException(runner, document.getRootElement());
    runner.pojo.setOwnerRequest(Configuration.configuration.getHostId());
    if (reverse) {
      runner.setSender(!runner.isSender());
      if (runner.isSender()) {
        runner.setFilename(runner.getOriginalFilename());
      }
    }
    return runner;
  }

  /**
   * Method to write the current DbTaskRunner for NoDb client instead of
   * updating DB. 'setToArray' must be
   * called priorly to be able to store the values.
   *
   * @throws OpenR66ProtocolBusinessException
   */
  public void writeXmlWorkNoDb() throws OpenR66ProtocolBusinessException {
    final String filename = backendXmlFilename();
    OutputStream outputStream = null;
    XMLWriter xmlWriter = null;
    boolean isOk = false;
    try {
      outputStream = new FileOutputStream(filename);
      final OutputFormat format = OutputFormat.createPrettyPrint();
      format.setEncoding(WaarpStringUtils.UTF_8);
      xmlWriter = new XMLWriter(outputStream, format);
      final Element root = new DefaultElement(XMLRUNNERS);
      try {
        xmlWriter.writeOpen(root);
        Element node;
        node = getElementFromRunner(this);
        xmlWriter.write(node);
        xmlWriter.flush();
        xmlWriter.writeClose(root);
        isOk = true;
      } catch (final IOException e) {
        logger.error("Cannot write XML file", e);
        throw new OpenR66ProtocolBusinessException(
            "Cannot write file: " + e.getMessage());
      } catch (final WaarpDatabaseSqlException e) {
        logger.error("Cannot write Data", e);
        throw new OpenR66ProtocolBusinessException(
            "Cannot write Data: " + e.getMessage());
      }
    } catch (final FileNotFoundException e) {
      logger.error("Cannot write XML file", e);
      throw new OpenR66ProtocolBusinessException("File not found");
    } catch (final UnsupportedEncodingException e) {
      logger.error("Cannot write XML file", e);
      throw new OpenR66ProtocolBusinessException("Unsupported Encoding");
    } finally {
      if (xmlWriter != null) {
        try {
          xmlWriter.endDocument();
          xmlWriter.flush();
          xmlWriter.close();
        } catch (final SAXException e) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
          logger.error("Cannot write XML file", e);
          throw new OpenR66ProtocolBusinessException("Cannot write XML file");
        } catch (final IOException e) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
          logger.error("Cannot write XML file", e);
          throw new OpenR66ProtocolBusinessException("IO error on XML file", e);
        }
        if (!isOk && outputStream != null) {
          FileUtils.close(outputStream);
          final File file = new File(filename);
          file.delete();
        }
      } else if (outputStream != null) {
        FileUtils.close(outputStream);
        final File file = new File(filename);
        file.delete();
      }
    }
  }

  /**
   * Method to load a previous existing DbTaskRunner for NoDb client from File
   * instead of from DB.
   * 'setFromArray' must be called after.
   *
   * @throws OpenR66ProtocolBusinessException
   */
  public void loadXmlWorkNoDb() throws OpenR66ProtocolBusinessException {
    final String filename = backendXmlFilename();
    final File file = new File(filename);
    if (!file.canRead()) {
      throw new OpenR66ProtocolBusinessException(
          "Backend XML file cannot be read");
    }
    final SAXReader reader = new SAXReader();
    Document document;
    try {
      document = reader.read(file);
    } catch (final DocumentException e) {
      throw new OpenR66ProtocolBusinessException(
          "Backend XML file cannot be read as an XML file", e);
    }
    final Element root =
        (Element) document.selectSingleNode('/' + XMLRUNNERS + '/' + XMLRUNNER);
    try {
      setRunnerFromElement(this, root);
    } catch (final WaarpDatabaseSqlException e) {
      throw new OpenR66ProtocolBusinessException(
          "Backend XML file is not conform to the model", e);
    }
  }

  /**
   * Special function for save or update for Log Import
   *
   * @throws WaarpDatabaseException
   */
  private void insertOrUpdateForLogsImport() throws WaarpDatabaseException {
    if (dbSession == null) {
      return;
    }
    if (super.exist()) {
      super.update();
    } else {
      super.insert();
    }
  }

  /**
   * Method to load several DbTaskRunner from File logs.
   *
   * @param logsFile File containing logs from export function
   *
   * @throws OpenR66ProtocolBusinessException
   */
  public static void loadXml(File logsFile)
      throws OpenR66ProtocolBusinessException {
    if (!logsFile.canRead()) {
      throw new OpenR66ProtocolBusinessException("XML file cannot be read");
    }
    final SAXReader reader = new SAXReader();
    Document document;
    try {
      document = reader.read(logsFile);
    } catch (final DocumentException e) {
      throw new OpenR66ProtocolBusinessException(
          "XML file cannot be read as an XML file", e);
    }
    @SuppressWarnings("unchecked")
    final List<Element> elts =
        document.selectNodes('/' + XMLRUNNERS + '/' + XMLRUNNER);
    boolean error = false;
    Exception one = null;
    for (final Element element : elts) {
      final DbTaskRunner runnerlog = new DbTaskRunner();
      try {
        setRunnerFromElement(runnerlog, element);
        runnerlog.insertOrUpdateForLogsImport();
      } catch (final WaarpDatabaseSqlException e) {
        error = true;
        one = e;
      } catch (final WaarpDatabaseException e) {
        error = true;
        one = e;
      }
    }
    if (error) {
      throw new OpenR66ProtocolBusinessException(
          "Backend XML file is not conform to the model", one);
    }
  }

  /**
   * @return True if the backend XML for NoDb client is available for this
   *     TaskRunner
   */
  public boolean existXmlWorkNoDb() {
    final String filename = backendXmlFilename();
    final File file = new File(filename);
    return file.canRead();
  }

  /**
   * Delete the backend XML file for the current TaskRunner for NoDb Client
   */
  public void deleteXmlWorkNoDb() {
    final File file = new File(backendXmlFilename());
    file.delete();
  }

  /**
   * Utility for "self request" mode only
   *
   * @param sender
   */
  public void setSender(boolean sender) {
    pojo.setRetrieveMode(sender);
  }

  /**
   * Helper
   *
   * @param request
   *
   * @return isSender according to request
   */
  public static boolean getSenderByRequestPacket(RequestPacket request) {
    if (request.isToValidate()) {
      return RequestPacket.isRecvMode(request.getMode());
    }
    return !RequestPacket.isRecvMode(request.getMode());
  }

  /**
   * Utility for "self request"
   *
   * @param requestToValidate
   */
  public void setSenderByRequestToValidate(boolean requestToValidate) {
    pojo.setRetrieveMode(RequestPacket.isRecvMode(pojo.getTransferMode()));
    if (!requestToValidate) {
      pojo.setRetrieveMode(!pojo.getRetrieveMode());
    }
  }

  /**
   * Utility to force "update"
   */
  private void setSenderForUpdate() {
    if (isSelfRequest()) {
      pojo.setRetrieveMode(RequestPacket.isRecvMode(pojo.getTransferMode()));
    }
  }

  /**
   * @return the originalSize
   */
  public long getOriginalSize() {
    return originalSize;
  }

  /**
   * @param originalSize the originalSize to set
   */
  public void setOriginalSize(long originalSize) {
    this.originalSize = originalSize;
    setOriginalSizeTransferMap(originalSize);
  }

  /**
   * @return the full path for the current file
   *
   * @throws CommandAbstractException
   */
  public String getFullFilePath() throws CommandAbstractException {
    if (isFileMoved()) {
      return getFilename();
    } else {
      final R66File file =
          new R66File(session, session.getDir(), getFilename(), false);
      return file.getTrueFile().getAbsolutePath();
    }
  }

}
