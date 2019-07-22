package org.waarp.openr66.pojo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.XTASK;
import static org.waarp.openr66.database.data.DbRule.TASK_DELAY;
import static org.waarp.openr66.database.data.DbRule.TASK_PATH;
import static org.waarp.openr66.database.data.DbRule.TASK_TYPE;

/**
 * RuleTask data object
 */
@XmlType(name = XTASK)
@XmlAccessorType(XmlAccessType.FIELD)
public class RuleTask {

    @XmlElement(name = TASK_TYPE)
    private String type;

    @XmlElement(name = TASK_PATH)
    private String path;

    @XmlElement(name = TASK_DELAY)
    private int delay;

    @SuppressWarnings("unused")
    public RuleTask() {}

    public RuleTask(String type, String path, int delay) {
        this.type = type;
        this.path = path;
        this.delay = delay;
    }

    public String getXML() {
        String res = "<task>";
        res = res + "<type>" + type + "</type>";
        res = res + "<path>" + path + "</path>";
        res = res + "<delay>" + delay + "</delay>";
        return res + "</task>";
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDelay() {
        return this.delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
