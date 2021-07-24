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

package org.waarp.openr66.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.waarp.common.database.data.AbstractDbData.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;

/**
 * Transfer data object
 */
@XmlType(name = DbTaskRunner.XMLRUNNER)
@XmlAccessorType(XmlAccessType.NONE)
public class Transfer {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(Transfer.class);

  public enum TASKSTEP {
    NOTASK(0), PRETASK(1), TRANSFERTASK(2), POSTTASK(3), ALLDONETASK(4),
    ERRORTASK(5);

    private final int taskNo;

    private static final Map<Integer, TASKSTEP> map =
        new HashMap<Integer, TASKSTEP>();

    static {
      for (final TASKSTEP task : TASKSTEP.values()) {
        map.put(task.taskNo, task);
      }
    }

    TASKSTEP(final int task) {
      taskNo = task;
    }

    public static TASKSTEP valueOf(final int taskStep) {
      return map.get(taskStep);
    }

    public final DbTaskRunner.TASKSTEP toLegacy() {
      return DbTaskRunner.TASKSTEP.valueOf(name());
    }

    @JsonValue
    public final int getTaskNo() {
      return taskNo;
    }
  }

  @XmlElement(name = ID_FIELD)
  @JsonProperty("SPECIALID")
  private long id = DbConstantR66.ILLEGALVALUE;

  /**
   * True if requester is the sender of the file (SEND MODE) False if
   * requested is the sender of the file
   * (RETRIEVE MODE)
   */
  @XmlElement(name = RETRIEVE_MODE_FIELD)
  @JsonProperty("RETRIEVEMODE")
  private boolean retrieveMode;

  @XmlElement(name = ID_RULE_FIELD)
  @JsonProperty("IDRULE")
  private String rule = "";

  @XmlElement(name = TRANSFER_MODE_FIELD)
  @JsonProperty("MODETRANS")
  private int transferMode = 1;

  @XmlElement(name = FILENAME_FIELD)
  @JsonProperty("FILENAME")
  private String filename = "";

  @XmlElement(name = ORIGINAL_NAME_FIELD)
  @JsonProperty("ORIGINALNAME")
  private String originalName = "";

  @XmlElement(name = FILE_INFO_FIELD)
  @JsonProperty("FILEINFO")
  private String fileInfo = "";

  @XmlElement(name = IS_MOVED_FIELD)
  @JsonProperty("ISMOVED")
  private boolean isMoved;

  @XmlElement(name = BLOCK_SIZE_FIELD)
  @JsonProperty("BLOCKSZ")
  private int blockSize;

  @XmlElement(name = OWNER_REQUEST_FIELD)
  @JsonProperty("OWNERREQ")
  private String ownerRequest = Configuration.configuration.getHostId();

  @XmlElement(name = REQUESTER_FIELD)
  @JsonProperty("REQUESTER")
  private String requester = "";

  @XmlElement(name = REQUESTED_FIELD)
  @JsonProperty("REQUESTED")
  private String requested = "";

  @XmlElement(name = TRANSFER_INFO_FIELD)
  @JsonProperty("TRANSFERINFO")
  private String transferInfo = "";

  @XmlElement(name = GLOBAL_STEP_FIELD)
  @JsonProperty("GLOBALSTEP")
  private TASKSTEP globalStep = TASKSTEP.NOTASK;

  @XmlElement(name = GLOBAL_LAST_STEP_FIELD)
  @JsonProperty("GLOBALLASTSTEP")
  private TASKSTEP lastGlobalStep = TASKSTEP.NOTASK;

  @XmlElement(name = STEP_FIELD)
  @JsonProperty("STEP")
  private int step = -1;

  @XmlElement(name = STEP_STATUS_FIELD)
  @JsonProperty("STEPSTATUS")
  private ErrorCode stepStatus = ErrorCode.Unknown;

  @XmlElement(name = INFO_STATUS_FIELD)
  @JsonProperty("INFOSTATUS")
  private ErrorCode infoStatus = ErrorCode.Unknown;

  @XmlElement(name = RANK_FIELD)
  @JsonProperty("RANK")
  private int rank;

  @XmlTransient
  @JsonProperty("STARTTRANS")
  private Timestamp start = new Timestamp(0);

  @XmlTransient
  @JsonProperty("STOPTRANS")
  private Timestamp stop = new Timestamp(0);

  @XmlTransient
  @JsonProperty("UPDATEDINFO")
  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  @XmlElement(name = TRANSFER_START_FIELD)
  @JsonIgnore
  public final long getXmlStart() {
    return start.getTime();
  }

  public final void setXmlStart(final long xml) {
    start = new Timestamp(xml);
  }

  @XmlElement(name = TRANSFER_STOP_FIELD)
  @JsonIgnore
  public final long getXmlStop() {
    return stop.getTime();
  }

  public final void setXmlStop(final long xml) {
    stop = new Timestamp(xml);
  }

  /**
   * Full Constructor to create Transfer from the database
   *
   * @param id
   * @param rule
   * @param mode
   * @param filename
   * @param originalName
   * @param fileInfo
   * @param isMoved
   * @param blockSize
   * @param retrieveMode
   * @param ownerReq
   * @param requester
   * @param requested
   * @param transferInfo
   * @param globalStep
   * @param lastGlobalStep
   * @param step
   * @param stepStatus
   * @param infoStatus
   * @param rank
   * @param start
   * @param stop
   * @param updatedInfo
   */
  public Transfer(final long id, final String rule, final int mode,
                  final String filename, final String originalName,
                  final String fileInfo, final boolean isMoved,
                  final int blockSize, final boolean retrieveMode,
                  final String ownerReq, final String requester,
                  final String requested, final String transferInfo,
                  final TASKSTEP globalStep, final TASKSTEP lastGlobalStep,
                  final int step, final ErrorCode stepStatus,
                  final ErrorCode infoStatus, final int rank,
                  final Timestamp start, final Timestamp stop,
                  final UpdatedInfo updatedInfo)
      throws WaarpDatabaseSqlException {
    this(id, rule, mode, filename, originalName, fileInfo, isMoved, blockSize,
         retrieveMode, ownerReq, requester, requested, transferInfo, globalStep,
         lastGlobalStep, step, stepStatus, infoStatus, rank, start, stop);
    this.updatedInfo = updatedInfo;
  }

  /**
   * Constructor to create Transfer from remote requests
   *
   * @param id
   * @param rule
   * @param mode
   * @param filename
   * @param originalName
   * @param fileInfo
   * @param isMoved
   * @param blockSize
   * @param retrieveMode
   * @param ownerReq
   * @param requester
   * @param requested
   * @param transferInfo
   * @param globalStep
   * @param lastGlobalStep
   * @param step
   * @param stepStatus
   * @param infoStatus
   * @param rank
   * @param start
   * @param stop
   */
  public Transfer(final long id, final String rule, final int mode,
                  final String filename, final String originalName,
                  final String fileInfo, final boolean isMoved,
                  final int blockSize, final boolean retrieveMode,
                  final String ownerReq, final String requester,
                  final String requested, final String transferInfo,
                  final TASKSTEP globalStep, final TASKSTEP lastGlobalStep,
                  final int step, final ErrorCode stepStatus,
                  final ErrorCode infoStatus, final int rank,
                  final Timestamp start, final Timestamp stop)
      throws WaarpDatabaseSqlException {
    this.id = id;
    this.rule = rule;
    transferMode = mode;
    this.retrieveMode = retrieveMode;
    this.filename = filename;
    this.originalName = originalName;
    this.fileInfo = fileInfo;
    this.isMoved = isMoved;
    this.blockSize = blockSize;
    ownerRequest = ownerReq;
    this.requester = requester;
    this.requested = requested;
    this.transferInfo = transferInfo;
    this.globalStep = globalStep;
    this.lastGlobalStep = lastGlobalStep;
    this.step = step;
    this.stepStatus = stepStatus;
    this.infoStatus = infoStatus;
    this.rank = rank;
    this.start = start;
    this.stop = stop;
    checkValues();
  }

  /**
   * Constructor to create transfer locally with delayed start time
   *
   * @param rule
   * @param retrieveMode
   * @param file
   * @param fileInfo
   * @param blockSize
   */
  public Transfer(final String remote, final String rule, final int ruleMode,
                  final boolean retrieveMode, final String file,
                  final String fileInfo, final int blockSize,
                  final Timestamp start) throws WaarpDatabaseSqlException {
    ownerRequest = Configuration.configuration.getHostId();
    requester = Configuration.configuration.getHostId();
    requested = remote;
    this.rule = rule;
    transferMode = ruleMode;
    this.retrieveMode = retrieveMode;
    filename = file;
    originalName = file;
    this.fileInfo = fileInfo;
    this.blockSize = blockSize;
    this.start = start;
    checkValues();
  }

  /**
   * Constructor to create transfer locally
   *
   * @param rule
   * @param retrieveMode
   * @param file
   * @param fileInfo
   * @param blockSize
   */
  public Transfer(final String remote, final String rule, final int ruleMode,
                  final boolean retrieveMode, final String file,
                  final String fileInfo, final int blockSize)
      throws WaarpDatabaseSqlException {
    this(remote, rule, ruleMode, retrieveMode, file, fileInfo, blockSize,
         new Timestamp(new Date().getTime()));
  }

  /**
   * Empty constructor
   */
  public Transfer() {
    // Nothing
  }

  @JsonIgnore
  public final void checkValues() throws WaarpDatabaseSqlException {
    validateLength(Types.NVARCHAR, rule, ownerRequest, requested, requester);
    validateLength(Types.VARCHAR, filename, originalName, fileInfo,
                   transferInfo);
  }

  public final long getId() {
    return id;
  }

  public final void setId(final long id) {
    this.id = id;
  }

  public final boolean getRetrieveMode() {
    return retrieveMode;
  }

  public final void setRetrieveMode(final boolean retrieveMode) {
    this.retrieveMode = retrieveMode;
  }

  public final String getRule() {
    return rule;
  }

  public final void setRule(final String rule) {
    this.rule = rule;
  }

  public final int getTransferMode() {
    return transferMode;
  }

  public final void setTransferMode(final int mode) {
    transferMode = mode;
  }

  public final String getFilename() {
    return filename;
  }

  public final void setFilename(final String filename) {
    this.filename = filename;
  }

  public final String getOriginalName() {
    return originalName;
  }

  public final void setOriginalName(final String originalName) {
    this.originalName = originalName;
  }

  public final String getFileInfo() {
    return fileInfo;
  }

  public final void setFileInfo(final String fileInfo) {
    this.fileInfo = fileInfo;
  }

  public final boolean getIsMoved() {
    return isMoved;
  }

  public final void setIsMoved(final boolean isMoved) {
    this.isMoved = isMoved;
  }

  public final int getBlockSize() {
    return blockSize;
  }

  public final void setBlockSize(final int blockSize) {
    this.blockSize = blockSize;
  }

  public final String getOwnerRequest() {
    return ownerRequest;
  }

  public final void setOwnerRequest(final String ownerRequest) {
    this.ownerRequest = ownerRequest;
  }

  public final String getRequester() {
    return requester;
  }

  public final void setRequester(final String requester) {
    this.requester = requester;
  }

  public final String getRequested() {
    return requested;
  }

  public final void setRequested(final String requested) {
    this.requested = requested;
  }

  public final String getTransferInfo() {
    return transferInfo;
  }

  public final void setTransferInfo(final String transferInfo) {
    this.transferInfo = transferInfo;
  }

  public final TASKSTEP getGlobalStep() {
    return globalStep;
  }

  public final void setGlobalStep(final TASKSTEP globalStep) {
    this.globalStep = globalStep;
  }

  public final TASKSTEP getLastGlobalStep() {
    return lastGlobalStep;
  }

  public final void setLastGlobalStep(final TASKSTEP lastGlobalStep) {
    this.lastGlobalStep = lastGlobalStep;
  }

  public final int getStep() {
    return step;
  }

  public final void setStep(final int step) {
    this.step = step;
  }

  public final ErrorCode getStepStatus() {
    return stepStatus;
  }

  public final void setStepStatus(final ErrorCode stepStatus) {
    this.stepStatus = stepStatus;
  }

  public final ErrorCode getInfoStatus() {
    return infoStatus;
  }

  public final void setInfoStatus(final ErrorCode infoStatus) {
    this.infoStatus = infoStatus;
  }

  public final int getRank() {
    return rank;
  }

  public final void setRank(final int rank) {
    this.rank = rank;
  }

  public final Timestamp getStart() {
    return start;
  }

  public final void setStart(final Timestamp start) {
    this.start = start;
  }

  public final Timestamp getStop() {
    return stop;
  }

  public final void setStop(final Timestamp stop) {
    this.stop = stop;
  }

  public final UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public final void setUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info;
  }
}
