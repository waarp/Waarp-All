package org.waarp.openr66.pojo;

import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Rules.Tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.ROOT;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XARCHIVEPATH;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XHOSTID;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XHOSTIDS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XIDRULE;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XMODE;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XRECVPATH;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XRERRORTASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XRPOSTTASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XRPRETASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XSENDPATH;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XSERRORTASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XSPOSTTASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XSPRETASKS;
import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XWORKPATH;

/**
 * Rule data object
 */
@XmlType(name = ROOT)
@XmlAccessorType(XmlAccessType.FIELD)
public class Rule {

    @XmlElement(name = XIDRULE)
    private String name;

    @XmlElement(name = XMODE)
    private int mode;

    @XmlElementWrapper(name = XHOSTIDS)
    @XmlElement(name = XHOSTID)
    private List<String> hostids;

    @XmlElement(name = XRECVPATH, required = true)
    private String recvPath;

    @XmlElement(name = XSENDPATH, required = true)
    private String sendPath;

    @XmlElement(name = XARCHIVEPATH, required = true)
    private String archivePath;

    @XmlElement(name = XWORKPATH, required = true)
    private String workPath;

    @XmlTransient
    private List<RuleTask> rPreTasks;

    @XmlTransient
    private List<RuleTask> rPostTasks;

    @XmlTransient
    private List<RuleTask> rErrorTasks;

    @XmlTransient
    private List<RuleTask> sPreTasks;

    @XmlTransient
    private List<RuleTask> sPostTasks;

    @XmlTransient
    private List<RuleTask> sErrorTasks;

    private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

    /**
     * Empty constructor for compatibility issues
     */
    @Deprecated
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
    public Tasks get_rPreTasks() {
        return new Tasks(this.rPreTasks);
    }

    @XmlElementDecl(name = XRPOSTTASKS)
    @XmlElement(name = XRPOSTTASKS)
    public Tasks get_rPostTasks() {
        return new Tasks(this.rPostTasks);
    }

    @XmlElementDecl(name = XRERRORTASKS)
    @XmlElement(name = XRERRORTASKS)
    public Tasks get_rErrorTasks() {
        return new Tasks(this.rErrorTasks);
    }

    @XmlElementDecl(name = XSPRETASKS)
    @XmlElement(name = XSPRETASKS)
    public Tasks get_sPreTasks() {
        return new Tasks(this.sPreTasks);
    }

    @XmlElementDecl(name = XSPOSTTASKS)
    @XmlElement(name = XSPOSTTASKS)
    public Tasks get_sPostTasks() {
        return new Tasks(this.sPostTasks);
    }

    @XmlElementDecl(name = XSERRORTASKS)
    @XmlElement(name = XSERRORTASKS)
    public Tasks get_sErrorTasks() {
        return new Tasks(this.sErrorTasks);
    }


    public Rule(String name, int mode, List<String> hostids, String recvPath,
            String sendPath, String archivePath, String workPath,
            List<RuleTask> rPre, List<RuleTask> rPost, List<RuleTask> rError,
            List<RuleTask> sPre, List<RuleTask> sPost, List<RuleTask> sError,
            UpdatedInfo updatedInfo) {
        this(name, mode, hostids, recvPath, sendPath, archivePath, workPath,
                rPre, rPost, rError, sPre, sPost, sError);
        this.updatedInfo = updatedInfo;
    }

    public Rule(String name, int mode, List<String> hostids, String recvPath,
            String sendPath, String archivePath, String workPath,
            List<RuleTask> rPre, List<RuleTask> rPost, List<RuleTask> rError,
            List<RuleTask> sPre, List<RuleTask> sPost, List<RuleTask> sError) {
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

    @Deprecated
    public String getXMLHostids() {
        String res = "<hostids>";
        for (String hostid : this.hostids) {
            res = res + "<hostid>" + hostid + "</hostid>";
        }
        return res + "</hostids>";
    }

    @Deprecated
    public String getXMLRPreTasks() {
        return getXMLTasks(this.rPreTasks);
    }

    @Deprecated
    public String getXMLRPostTasks() {
        return getXMLTasks(this.rPostTasks);
    }

    @Deprecated
    public String getXMLRErrorTasks() {
        return getXMLTasks(this.rErrorTasks);
    }

    @Deprecated
    public String getXMLSPreTasks() {
        return getXMLTasks(this.sPreTasks);
    }

    @Deprecated
    public String getXMLSPostTasks() {
        return getXMLTasks(this.sPostTasks);
    }

    @Deprecated
    public String getXMLSErrorTasks() {
        return getXMLTasks(this.sErrorTasks);
    }

    @Deprecated
    private String getXMLTasks(List<RuleTask> tasks) {
        String res = "<tasks>";
        for (RuleTask task : tasks) {
            res = res + task.getXML();
        }
        return res + "</tasks>";
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public List<String> getHostids() {
        return this.hostids;
    }

    public void setHostids(List<String> hostids) {
        this.hostids = hostids;
    }

    public String getRecvPath() {
        return this.recvPath;
    }

    public void setRecvPath(String recvPath) {
        this.recvPath = recvPath;
    }

    public String getSendPath() {
        return this.sendPath;
    }

    public void setSendPath(String sendPath) {
        this.sendPath = sendPath;
    }

    public String getArchivePath() {
        return this.archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getWorkPath() {
        return this.workPath;
    }

    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }

    public List<RuleTask> getRPreTasks() {
        return this.rPreTasks;
    }

    public void setRPreTasks(List<RuleTask> rPreTasks) {
        this.rPreTasks = rPreTasks;
    }

    public List<RuleTask> getRPostTasks() {
        return this.rPostTasks;
    }

    public void setRPostTasks(List<RuleTask> rPostTasks) {
        this.rPostTasks = rPostTasks;
    }

    public List<RuleTask> getRErrorTasks() {
        return this.rErrorTasks;
    }

    public void setRErrorTasks(List<RuleTask> rErrorTasks) {
        this.rErrorTasks = rErrorTasks;
    }

    public List<RuleTask> getSPreTasks() {
        return this.sPreTasks;
    }

    public void setSPreTasks(List<RuleTask> sPreTasks) {
        this.sPreTasks = sPreTasks;
    }

    public List<RuleTask> getSPostTasks() {
        return this.sPostTasks;
    }

    public void setSPostTasks(List<RuleTask> sPostTasks) {
        this.sPostTasks = sPostTasks;
    }

    public List<RuleTask> getSErrorTasks() {
        return this.sErrorTasks;
    }

    public void setSErrorTasks(List<RuleTask> sErrorTasks) {
        this.sErrorTasks = sErrorTasks;
    }

    public UpdatedInfo getUpdatedInfo() {
        return this.updatedInfo;
    }

    public void setUpdatedInfo(UpdatedInfo info) {
        this.updatedInfo = info;
    }
}
