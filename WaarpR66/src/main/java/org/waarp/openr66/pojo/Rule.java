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

import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Rules.Tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.*;

/**
 * Rule data object
 */
@XmlType(name = ROOT)
@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({
    "IDRULE", "MODETRANS", "RECVPATH", "SENDPATH", "ARCHIVEPATH",
    "WORKPATH", "UPDATEDINFO", "HOSTIDS", "SPRETASKS", "SPOSTASKS", "SERRORTASKS",
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
  public Tasks get_rPreTasks() {
    return new Tasks(rPreTasks);
  }

  @XmlElementDecl(name = XRPOSTTASKS)
  @XmlElement(name = XRPOSTTASKS)
  @JsonIgnore
  public Tasks get_rPostTasks() {
    return new Tasks(rPostTasks);
  }

  @XmlElementDecl(name = XRERRORTASKS)
  @XmlElement(name = XRERRORTASKS)
  @JsonIgnore
  public Tasks get_rErrorTasks() {
    return new Tasks(rErrorTasks);
  }

  @XmlElementDecl(name = XSPRETASKS)
  @XmlElement(name = XSPRETASKS)
  @JsonIgnore
  public Tasks get_sPreTasks() {
    return new Tasks(sPreTasks);
  }

  @XmlElementDecl(name = XSPOSTTASKS)
  @XmlElement(name = XSPOSTTASKS)
  @JsonIgnore
  public Tasks get_sPostTasks() {
    return new Tasks(sPostTasks);
  }

  @XmlElementDecl(name = XSERRORTASKS)
  @XmlElement(name = XSERRORTASKS)
  @JsonIgnore
  public Tasks get_sErrorTasks() {
    return new Tasks(sErrorTasks);
  }

  public Rule(String name, int mode, List<String> hostids, String recvPath,
              String sendPath, String archivePath, String workPath,
              List<RuleTask> rPre, List<RuleTask> rPost, List<RuleTask> rError,
              List<RuleTask> sPre, List<RuleTask> sPost, List<RuleTask> sError,
              UpdatedInfo updatedInfo) {
    this(name, mode, hostids, recvPath, sendPath, archivePath, workPath, rPre,
         rPost, rError, sPre, sPost, sError);
    this.updatedInfo = updatedInfo;
  }

  public Rule(String name, int mode, List<String> hostids, String recvPath,
              String sendPath, String archivePath, String workPath,
              List<RuleTask> rPre, List<RuleTask> rPost, List<RuleTask> rError,
              List<RuleTask> sPre, List<RuleTask> sPost,
              List<RuleTask> sError) {
    this.name = name;
    this.mode = mode;
    this.hostids = hostids;
    this.recvPath = recvPath;
    this.sendPath = sendPath;
    this.archivePath = archivePath;
    this.workPath = workPath;
    rPreTasks = rPre;
    rPostTasks = rPost;
    rErrorTasks = rError;
    sPreTasks = sPre;
    sPostTasks = sPost;
    sErrorTasks = sError;
  }

  public Rule(String name, int mode, List<String> hostids, String recvPath,
              String sendPath, String archivePath, String workPath) {
    this(name, mode, hostids, recvPath, sendPath, archivePath, workPath,
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
         new ArrayList<RuleTask>(), new ArrayList<RuleTask>());
  }

  public Rule(String name, int mode, List<String> hostids) {
    this(name, mode, hostids, "", "", "", "");
  }

  public Rule(String name, int mode) {
    this(name, mode, new ArrayList<String>());
  }

  public boolean isAuthorized(String hostid) {
    return hostids.contains(hostid);
  }

  @JsonProperty("HOSTIDS")
  public String getXMLHostids() {
    StringBuilder res = new StringBuilder("<hostids>");
    for (final String hostid : hostids) {
      res.append("<hostid>").append(hostid).append("</hostid>");
    }
    return res.append("</hostids>").toString();
  }

  @JsonProperty("RPRETASKS")
  public String getXMLRPreTasks() {
    return getXMLTasks(rPreTasks);
  }

  @JsonProperty("RPOSTTASKS")
  public String getXMLRPostTasks() {
    return getXMLTasks(rPostTasks);
  }

  @JsonProperty("RERRORTASKS")
  public String getXMLRErrorTasks() {
    return getXMLTasks(rErrorTasks);
  }

  @JsonProperty("SPRETASKS")
  public String getXMLSPreTasks() {
    return getXMLTasks(sPreTasks);
  }

  @JsonProperty("SPOSTTASKS")
  public String getXMLSPostTasks() {
    return getXMLTasks(sPostTasks);
  }

  @JsonProperty("SERRORTASKS")
  public String getXMLSErrorTasks() {
    return getXMLTasks(sErrorTasks);
  }

  private String getXMLTasks(List<RuleTask> tasks) {
    StringBuilder res = new StringBuilder("<tasks>");
    for (final RuleTask task : tasks) {
      res.append(task.getXML());
    }
    return res.append("</tasks>").toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public List<String> getHostids() {
    return hostids;
  }

  public void setHostids(List<String> hostids) {
    this.hostids = hostids;
  }

  public String getRecvPath() {
    return recvPath;
  }

  public void setRecvPath(String recvPath) {
    this.recvPath = recvPath;
  }

  public String getSendPath() {
    return sendPath;
  }

  public void setSendPath(String sendPath) {
    this.sendPath = sendPath;
  }

  public String getArchivePath() {
    return archivePath;
  }

  public void setArchivePath(String archivePath) {
    this.archivePath = archivePath;
  }

  public String getWorkPath() {
    return workPath;
  }

  public void setWorkPath(String workPath) {
    this.workPath = workPath;
  }

  @JsonIgnore
  public List<RuleTask> getRPreTasks() {
    return rPreTasks;
  }

  public void setRPreTasks(List<RuleTask> rPreTasks) {
    this.rPreTasks = rPreTasks;
  }

  @JsonIgnore
  public List<RuleTask> getRPostTasks() {
    return rPostTasks;
  }

  public void setRPostTasks(List<RuleTask> rPostTasks) {
    this.rPostTasks = rPostTasks;
  }

  @JsonIgnore
  public List<RuleTask> getRErrorTasks() {
    return rErrorTasks;
  }

  public void setRErrorTasks(List<RuleTask> rErrorTasks) {
    this.rErrorTasks = rErrorTasks;
  }

  @JsonIgnore
  public List<RuleTask> getSPreTasks() {
    return sPreTasks;
  }

  public void setSPreTasks(List<RuleTask> sPreTasks) {
    this.sPreTasks = sPreTasks;
  }

  @JsonIgnore
  public List<RuleTask> getSPostTasks() {
    return sPostTasks;
  }

  public void setSPostTasks(List<RuleTask> sPostTasks) {
    this.sPostTasks = sPostTasks;
  }

  @JsonIgnore
  public List<RuleTask> getSErrorTasks() {
    return sErrorTasks;
  }

  public void setSErrorTasks(List<RuleTask> sErrorTasks) {
    this.sErrorTasks = sErrorTasks;
  }

  public UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public void setUpdatedInfo(UpdatedInfo info) {
    updatedInfo = info;
  }
}
