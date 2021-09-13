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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.configuration.BadConfigurationException;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Rules.Tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.common.database.data.AbstractDbData.*;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.*;

/**
 * Rule data object
 */
@XmlType(name = ROOT)
@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({
    "IDRULE", "MODETRANS", "RECVPATH", "SENDPATH", "ARCHIVEPATH", "WORKPATH",
    "UPDATEDINFO", "HOSTIDS", "SPRETASKS", "SPOSTASKS", "SERRORTASKS",
    "RPRETASKS", "RPOSTTASKS", "RERRORTASKS"
})
public class Rule {

  @XmlElement(name = XIDRULE)
  @JsonProperty("IDRULE")
  private String name;

  @XmlElement(name = XMODE)
  @JsonProperty("MODETRANS")
  private int mode;

  @XmlElementWrapper(name = XHOSTIDS)
  @XmlElement(name = XHOSTID)
  @JsonIgnore
  private List<String> hostids;

  @XmlElement(name = XRECVPATH, required = true)
  @JsonProperty("RECVPATH")
  private String recvPath;

  @XmlElement(name = XSENDPATH, required = true)
  @JsonProperty("SENDPATH")
  private String sendPath;

  @XmlElement(name = XARCHIVEPATH, required = true)
  @JsonProperty("ARCHIVEPATH")
  private String archivePath;

  @XmlElement(name = XWORKPATH, required = true)
  @JsonProperty("WORKPATH")
  private String workPath;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> rPreTasks;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> rPostTasks;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> rErrorTasks;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> sPreTasks;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> sPostTasks;

  @XmlTransient
  @JsonIgnore
  private List<RuleTask> sErrorTasks;

  @JsonProperty("UPDATEDINFO")
  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor
   */
  public Rule() {
    hostids = new ArrayList<String>();
    rPreTasks = new ArrayList<RuleTask>();
    rPostTasks = new ArrayList<RuleTask>();
    rErrorTasks = new ArrayList<RuleTask>();
    sPreTasks = new ArrayList<RuleTask>();
    sPostTasks = new ArrayList<RuleTask>();
    sErrorTasks = new ArrayList<RuleTask>();
  }

  @XmlElementDecl(name = XRPRETASKS)
  @XmlElement(name = XRPRETASKS)
  @JsonIgnore
  public final Tasks get_rPreTasks() {
    return new Tasks(rPreTasks);
  }

  @XmlElementDecl(name = XRPOSTTASKS)
  @XmlElement(name = XRPOSTTASKS)
  @JsonIgnore
  public final Tasks get_rPostTasks() {
    return new Tasks(rPostTasks);
  }

  @XmlElementDecl(name = XRERRORTASKS)
  @XmlElement(name = XRERRORTASKS)
  @JsonIgnore
  public final Tasks get_rErrorTasks() {
    return new Tasks(rErrorTasks);
  }

  @XmlElementDecl(name = XSPRETASKS)
  @XmlElement(name = XSPRETASKS)
  @JsonIgnore
  public final Tasks get_sPreTasks() {
    return new Tasks(sPreTasks);
  }

  @XmlElementDecl(name = XSPOSTTASKS)
  @XmlElement(name = XSPOSTTASKS)
  @JsonIgnore
  public final Tasks get_sPostTasks() {
    return new Tasks(sPostTasks);
  }

  @XmlElementDecl(name = XSERRORTASKS)
  @XmlElement(name = XSERRORTASKS)
  @JsonIgnore
  public final Tasks get_sErrorTasks() {
    return new Tasks(sErrorTasks);
  }

  public Rule(final String name, final int mode, final List<String> hostids,
              final String recvPath, final String sendPath,
              final String archivePath, final String workPath,
              final List<RuleTask> rPre, final List<RuleTask> rPost,
              final List<RuleTask> rError, final List<RuleTask> sPre,
              final List<RuleTask> sPost, final List<RuleTask> sError,
              final UpdatedInfo updatedInfo) throws WaarpDatabaseSqlException {
    this(name, mode, hostids, recvPath, sendPath, archivePath, workPath, rPre,
         rPost, rError, sPre, sPost, sError);
    this.updatedInfo = updatedInfo;
    checkValues();
  }

  public Rule(final String name, final int mode, final List<String> hostids,
              final String recvPath, final String sendPath,
              final String archivePath, final String workPath,
              final List<RuleTask> rPre, final List<RuleTask> rPost,
              final List<RuleTask> rError, final List<RuleTask> sPre,
              final List<RuleTask> sPost, final List<RuleTask> sError)
      throws WaarpDatabaseSqlException {
    this.name = name;
    this.mode = mode;
    this.hostids = hostids;
    this.recvPath = checkPath(recvPath);
    this.sendPath = checkPath(sendPath);
    this.archivePath = checkPath(archivePath);
    this.workPath = checkPath(workPath);
    rPreTasks = rPre;
    rPostTasks = rPost;
    rErrorTasks = rError;
    sPreTasks = sPre;
    sPostTasks = sPost;
    sErrorTasks = sError;
    checkValues();
  }

  public Rule(final String name, final int mode, final List<String> hostids,
              final String recvPath, final String sendPath,
              final String archivePath, final String workPath)
      throws WaarpDatabaseSqlException {
    this(name, mode, hostids, recvPath, sendPath, archivePath, workPath,
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>());
  }

  public Rule(final String name, final int mode, final List<String> hostids)
      throws WaarpDatabaseSqlException {
    this(name, mode, hostids, "", "", "", "");
  }

  public Rule(final String name, final int mode)
      throws WaarpDatabaseSqlException {
    this(name, mode, new ArrayList<String>());
  }

  @JsonIgnore
  public final void checkValues() throws WaarpDatabaseSqlException {
    validateLength(Types.NVARCHAR, name);
    validateLength(Types.VARCHAR, recvPath, sendPath, archivePath, workPath);
    validateLength(Types.LONGVARCHAR, getXMLHostids(), getXMLRErrorTasks(),
                   getXMLRPreTasks(), getXMLRPostTasks(), getXMLSErrorTasks(),
                   getXMLSPreTasks(), getXMLSPostTasks());
  }

  public final boolean isAuthorized(final String hostid) {
    return hostids.contains(hostid);
  }

  @JsonProperty("HOSTIDS")
  public final String getXMLHostids() {
    final StringBuilder res = new StringBuilder("<hostids>");
    for (final String hostid : hostids) {
      res.append("<hostid>").append(hostid).append("</hostid>");
    }
    return res.append("</hostids>").toString();
  }

  @JsonProperty("RPRETASKS")
  public final String getXMLRPreTasks() {
    return getXMLTasks(rPreTasks);
  }

  @JsonProperty("RPOSTTASKS")
  public final String getXMLRPostTasks() {
    return getXMLTasks(rPostTasks);
  }

  @JsonProperty("RERRORTASKS")
  public final String getXMLRErrorTasks() {
    return getXMLTasks(rErrorTasks);
  }

  @JsonProperty("SPRETASKS")
  public final String getXMLSPreTasks() {
    return getXMLTasks(sPreTasks);
  }

  @JsonProperty("SPOSTTASKS")
  public final String getXMLSPostTasks() {
    return getXMLTasks(sPostTasks);
  }

  @JsonProperty("SERRORTASKS")
  public final String getXMLSErrorTasks() {
    return getXMLTasks(sErrorTasks);
  }

  private String getXMLTasks(final List<RuleTask> tasks) {
    final StringBuilder res = new StringBuilder("<tasks>");
    for (final RuleTask task : tasks) {
      res.append(task.getXML());
    }
    return res.append("</tasks>").toString();
  }

  public final String getName() {
    return name;
  }

  public final void setName(final String name) {
    this.name = name;
  }

  public final int getMode() {
    return mode;
  }

  public final void setMode(final int mode) {
    this.mode = mode;
  }

  public final List<String> getHostids() {
    return hostids;
  }

  public final void setHostids(final List<String> hostids) {
    this.hostids = hostids;
  }

  private String checkPath(final String path) throws WaarpDatabaseSqlException {
    if (ParametersChecker.isEmpty(path)) {
      return "";
    }
    final String path2 = path.replace("//", "/").replaceAll("[\\\\]+", "\\\\");
    try {
      return FileBasedConfiguration.checkNotAbsolutePathNotUnderBase(
          Configuration.configuration, path2);
    } catch (final BadConfigurationException e) {
      throw new WaarpDatabaseSqlException(e);
    }
  }

  public final String getRecvPath() {
    return recvPath;
  }

  public final void setRecvPath(final String recvPath)
      throws WaarpDatabaseSqlException {
    this.recvPath = checkPath(recvPath);
  }

  public final String getSendPath() {
    return sendPath;
  }

  public final void setSendPath(final String sendPath)
      throws WaarpDatabaseSqlException {
    this.sendPath = checkPath(sendPath);
  }

  public final String getArchivePath() {
    return archivePath;
  }

  public final void setArchivePath(final String archivePath)
      throws WaarpDatabaseSqlException {
    this.archivePath = checkPath(archivePath);
  }

  public final String getWorkPath() {
    return workPath;
  }

  public final void setWorkPath(final String workPath)
      throws WaarpDatabaseSqlException {
    this.workPath = checkPath(workPath);
  }

  @JsonIgnore
  public final List<RuleTask> getRPreTasks() {
    return rPreTasks;
  }

  public final void setRPreTasks(final List<RuleTask> rPreTasks) {
    this.rPreTasks = rPreTasks;
  }

  @JsonIgnore
  public final List<RuleTask> getRPostTasks() {
    return rPostTasks;
  }

  public final void setRPostTasks(final List<RuleTask> rPostTasks) {
    this.rPostTasks = rPostTasks;
  }

  @JsonIgnore
  public final List<RuleTask> getRErrorTasks() {
    return rErrorTasks;
  }

  public final void setRErrorTasks(final List<RuleTask> rErrorTasks) {
    this.rErrorTasks = rErrorTasks;
  }

  @JsonIgnore
  public final List<RuleTask> getSPreTasks() {
    return sPreTasks;
  }

  public final void setSPreTasks(final List<RuleTask> sPreTasks) {
    this.sPreTasks = sPreTasks;
  }

  @JsonIgnore
  public final List<RuleTask> getSPostTasks() {
    return sPostTasks;
  }

  public final void setSPostTasks(final List<RuleTask> sPostTasks) {
    this.sPostTasks = sPostTasks;
  }

  @JsonIgnore
  public final List<RuleTask> getSErrorTasks() {
    return sErrorTasks;
  }

  public final void setSErrorTasks(final List<RuleTask> sErrorTasks) {
    this.sErrorTasks = sErrorTasks;
  }

  public final UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public final void setUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info;
  }
}
