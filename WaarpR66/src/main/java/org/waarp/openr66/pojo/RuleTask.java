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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.waarp.openr66.configuration.RuleFileBasedConfiguration.*;
import static org.waarp.openr66.database.data.DbRule.*;

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

  public RuleTask() {
    // Nothing
  }

  public RuleTask(final String type, final String path, final int delay) {
    this.type = type;
    this.path = path;
    this.delay = delay;
  }

  public final String getXML() {
    String res = "<task>";
    res = res + "<type>" + type + "</type>";
    res = res + "<path>" + path + "</path>";
    res = res + "<delay>" + delay + "</delay>";
    return res + "</task>";
  }

  @Override
  public final String toString() {
    return getXML();
  }

  public final String getType() {
    return type;
  }

  public final void setType(final String type) {
    this.type = type;
  }

  public final String getPath() {
    return path;
  }

  public final void setPath(final String path) {
    this.path = path;
  }

  public final int getDelay() {
    return delay;
  }

  public final void setDelay(final int delay) {
    this.delay = delay;
  }
}
